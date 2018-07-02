package com.infowings.catalog.data.aspect

import com.infowings.catalog.auth.user.UserService
import com.infowings.catalog.common.*
import com.infowings.catalog.data.history.HistoryContext
import com.infowings.catalog.data.history.HistoryFactWrite
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.data.reference.book.ReferenceBookService
import com.infowings.catalog.external.logTime
import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.*
import com.infowings.catalog.storage.transaction


interface AspectService {
    fun save(aspectData: AspectData, username: String): AspectData
    fun remove(aspectData: AspectData, username: String, force: Boolean = false)
    fun findByName(name: String): Set<AspectData>
    fun getAspects(orderBy: List<AspectOrderBy> = listOf(AspectOrderBy(AspectSortField.NAME, Direction.ASC)), query: String? = null): List<AspectData>
    fun findById(id: String): AspectData
    fun findPropertyById(id: String): AspectPropertyData
}

class NormalizedAspectService(private val innerService: AspectService) : AspectService by innerService {
    override fun save(aspectData: AspectData, username: String): Aspect = innerService.save(aspectData.normalize(), username)
    override fun remove(aspectData: AspectData, username: String, force: Boolean) = innerService.remove(aspectData.normalize(), username, force)
}

/**
 * Data layer for Aspect & Aspect properties
 * Both stored as vertexes [ASPECT_CLASS] & [ASPECT_PROPERTY_CLASS] linked by [ASPECT_ASPECT_PROPERTY_EDGE]
 * [ASPECT_CLASS] can be linked with [Measure] by [ASPECT_MEASURE_CLASS]
 */
class DefaultAspectService(
    private val db: OrientDatabase,
    private val aspectDaoService: AspectDaoService,
    private val historyService: HistoryService,
    private val referenceBookService: ReferenceBookService,
    private val userService: UserService
) : AspectService {

    private val aspectValidator = AspectValidator(aspectDaoService)

    private fun savePlain(aspectVertex: AspectVertex, aspectData: AspectData, context: HistoryContext): AspectVertex {
        val (deletedProperties, updatedProperties) = aspectData.properties.partition { it.deleted }
        deletedProperties.forEach { remove(it, context) }
        aspectVertex.saveAspectProperties(updatedProperties, context)

        if (aspectVertex.referenceBookRootVertex != null && aspectData.refBookName == null) {
            // по ходу редактирования на фронте решили дропнуть справочник
            val refBook: ReferenceBook = referenceBookService.getReferenceBook(aspectVertex.id)
            // пока для простоты сделаем сразу принудительное удаление
            // чтобы сделать сделать общую схему (SoftDelete сначала, если не получилось, то Delete принудительно по
            // подтверждению от пользователя) - надо усложнить интерефейс и, что важнее, усложнить user experience
            // У нас появится подтверждение на удаление связанного справочника (в контексте редактирования аспекта),
            // вдобавок к подтверждению на удаление аспекта и подтверждению удаление справчника как следствию
            // выбора меры/типа (которое не зависит от связанности справочника)
            // Надо понять, какой UE мы хотим, что человек не запутался в подтверждениях.
            referenceBookService.removeReferenceBook(refBook, context.userVertex, force = true)
            aspectVertex.dropRefBookEdge()
        }

        return aspectDaoService.saveAspect(aspectVertex, aspectData)
    }

    /*
    Вспомогательный метод для save
    Завершает обновление на случай обновления
    Запускается изнутри транзакции на database
     */
    private fun updateFinish(
        aspectVertex: AspectVertex,
        aspectData: AspectData,
        context: HistoryContext
    ): AspectVertex {
        val baseSnapshot = aspectVertex.currentSnapshot()

        val res = savePlain(aspectVertex, aspectData, context)

        historyService.storeFact(aspectVertex.toUpdateFact(context, baseSnapshot))

        return res
    }

    /*
    Вспомогательный метод для save
    Завершает обновление на случай создания
    Запускается изнутри транзакции на database
    */
    private fun createFinish(
        aspectVertex: AspectVertex,
        aspectData: AspectData,
        context: HistoryContext
    ): AspectVertex {
        val res = savePlain(aspectVertex, aspectData, context)
        historyService.storeFact(aspectVertex.toCreateFact(context))
        return res
    }

    private val logger = loggerFor<AspectService>()

    /**
     * Creates new Aspect if [id] = null or empty and saves it into DB else updating existing
     * @param aspectData data that represents Aspect, which will be saved or updated
     * @throws AspectAlreadyExist,
     * @throws IllegalArgumentException in case of incorrect input data,
     * @throws AspectDoesNotExist if some AspectProperty has incorrect aspect id
     * @throws AspectCyclicDependencyException if one of AspectProperty of the aspect refers to parent Aspect
     * @throws AspectEmptyChangeException if new data is the same that old data
     */
    override fun save(aspectData: AspectData, username: String): AspectData {
        val userVertex = userService.findUserVertexByUsername(username)

        val save: AspectVertex = transaction(db) {
            val aspectVertex = aspectData
                .checkAspectDataConsistent()
                .checkBusinessKey()
                .getOrCreateAspectVertex()

            /*
            if (aspectVertex.toAspectData() == aspectData) {
                throw AspectEmptyChangeException()
            }*/

            val finishMethod = if (aspectVertex.identity.isNew) this::createFinish else {
                if (aspectVertex.toAspectData() == aspectData) {
                    throw AspectEmptyChangeException()
                }
                this::updateFinish
            }

            return@transaction finishMethod(aspectVertex, aspectData, HistoryContext(userVertex))
        }

        logger.debug("Aspect ${aspectData.name} saved/updated with id: ${save.id}")

        return if (save.identity.clusterPosition < 0) {
            // Кажется, что такого быть не должно. Но есть ощущение, что так бывало.
            // Но воспроизвести не удалось.
            // Оставим эту веточку. Последим за логами
            val res = findById(save.id)

            logger.warn("Cluster position is negative: ${save.id}. Aspect: $save. Recovered: $res")

            res
        } else transaction(db) { save.toAspectData() }
    }

    override fun remove(aspectData: AspectData, username: String, force: Boolean) {
        val userVertex = userService.findUserVertexByUsername(username)

        transaction(db) {
            val context = HistoryContext(userVertex)

            val aspectId = aspectData.id ?: throw IllegalStateException("Id is null")

            val aspectVertex =
                aspectDaoService.getVertex(aspectId)?.toAspectVertex() ?: throw AspectDoesNotExist(aspectId)

            aspectVertex.checkAspectVersion(aspectData)

            val refBook = referenceBookService.getReferenceBookOrNull(aspectId)

            val linked = aspectVertex.isLinkedBy()

            when {
                linked && force -> {
                    historyService.storeFact(aspectVertex.toSoftDeleteFact(context))
                    if (refBook != null) referenceBookService.removeReferenceBook(refBook, context.userVertex, force)
                    aspectDaoService.fakeRemove(aspectVertex)
                }
                linked -> {
                    throw AspectHasLinkedEntitiesException(aspectId)
                }
                else -> {
                    historyService.storeFact(aspectVertex.toDeleteFact(context))
                    if (refBook != null) referenceBookService.removeReferenceBook(refBook, context.userVertex)
                    aspectDaoService.remove(aspectVertex)
                }
            }
        }
    }

    /**
     * Search [AspectData] by it's name
     * @return List of [AspectData] with name [name]
     */
    override fun findByName(name: String): Set<AspectData> = transaction(db) {
        aspectDaoService.findByName(name).map { it.toAspectData() }.toSet()
    }

    override fun getAspects(
        orderBy: List<AspectOrderBy> = listOf(
            AspectOrderBy(
                AspectSortField.NAME,
                Direction.ASC
            )
        ),
        query: String? = null
    ): List<AspectData> {
        val aspects = transaction(db) {
            val vertices = logTime(logger, "getting aspect vertices") {
                when {
                    query == null || query.isBlank() -> aspectDaoService.getAspects()
                    else -> aspectDaoService.findTransitiveByNameQuery(query)
                }
            }
            logTime(logger, "extracting aspects") {
                vertices.map { it.toAspectData() }
            }
        }

        return logTime(logger, "sorting aspects") {
            aspects.sort(orderBy)
        }
    }

    private fun findVertexById(id: String): AspectVertex =
        aspectDaoService.getAspectVertex(id) ?: throw AspectDoesNotExist(id)

    /**
     * Search [AspectData] by it's id
     * @throws AspectDoesNotExist
     */
    override fun findById(id: String): AspectData = transaction(db) { findVertexById(id).toAspectData() }

    override fun findPropertyById(id: String) = findPropertyVertexById(id).toAspectPropertyData()

    private fun findPropertyVertexById(id: String): AspectPropertyVertex = aspectDaoService.getAspectPropertyVertex(id)
            ?: throw AspectPropertyDoesNotExist(id)


    private class CompareString(val value: String, val direction: Direction) : Comparable<CompareString> {
        override fun compareTo(other: CompareString): Int =
            direction.dir * value.toLowerCase().compareTo(other.value.toLowerCase())
    }

    private fun List<AspectData>.sort(orderBy: List<AspectOrderBy>): List<AspectData> {
        if (orderBy.isEmpty()) {
            return this
        }

        fun aspectNameAsc(aspect: AspectData): Comparable<*> = CompareString(aspect.name, Direction.ASC)
        fun aspectNameDesc(aspect: AspectData): Comparable<*> = CompareString(aspect.name, Direction.DESC)
        fun aspectSubjectNameAsc(aspect: AspectData): Comparable<*> =
            CompareString(aspect.subject?.name ?: "", Direction.ASC)

        fun aspectSubjectNameDesc(aspect: AspectData): Comparable<*> =
            CompareString(aspect.subject?.name ?: "", Direction.DESC)

        val m = mapOf<AspectSortField, Map<Direction, (AspectData) -> Comparable<*>>>(
            AspectSortField.NAME to mapOf(Direction.ASC to ::aspectNameAsc, Direction.DESC to ::aspectNameDesc),
            AspectSortField.SUBJECT to mapOf(
                Direction.ASC to ::aspectSubjectNameAsc,
                Direction.DESC to ::aspectSubjectNameDesc
            )
        )
        return this.sortedWith(compareBy(*orderBy.map { m.getValue(it.name).getValue(it.direction) }.toTypedArray()))
    }


    /** Method is private and it is supposed that version checking successfully accepted before. */
    private fun remove(property: AspectPropertyData, context: HistoryContext) = transaction(db) {
        historyService.storeFact(findPropertyVertexById(property.id).toDeleteFact(context))

        val vertex = aspectDaoService.getAspectPropertyVertex(property.id)
                ?: throw AspectPropertyDoesNotExist(property.id)

        return@transaction if (vertex.isLinkedBy()) aspectDaoService.fakeRemove(vertex) else aspectDaoService.remove(
            vertex
        )
    }

    /**
     * Create empty vertex in case [AspectData.id] is null or empty
     * Otherwise validate and return vertex of class [ASPECT_CLASS] with given id
     * @throws IllegalStateException
     * @throws AspectConcurrentModificationException
     * */
    private fun AspectData.getOrCreateAspectVertex(): AspectVertex {
        val aspectId = id

        if (aspectId.isNullOrEmpty())
            return aspectDaoService.createNewAspectVertex()


        return aspectDaoService.getAspectVertex(aspectId!!)
            ?.validateExistingAspect(this)
                ?: throw IllegalArgumentException("Incorrect aspect id")

    }

    /**
     * Create empty vertex in case [AspectPropertyData.id] is null or empty
     * Otherwise validate and return vertex of class [ASPECT_PROPERTY_CLASS] with given id
     * @throws IllegalStateException
     * @throws AspectPropertyModificationException
     * */
    private fun AspectPropertyData.getOrCreatePropertyVertex(): AspectPropertyVertex {
        val propertyId = id

        if (propertyId.isEmpty())
            return aspectDaoService.createNewAspectPropertyVertex()

        return aspectDaoService.getAspectPropertyVertex(propertyId)
            ?.validateExistingAspectProperty(this)
                ?: throw IllegalArgumentException("Incorrect property id")

    }

    private fun AspectPropertyVertex.validateExistingAspectProperty(aspectPropertyData: AspectPropertyData): AspectPropertyVertex =
        this.also { aspectValidator.validateExistingAspectProperty(this, aspectPropertyData) }

    private fun AspectVertex.validateExistingAspect(aspectData: AspectData): AspectVertex =
        this.also { aspectValidator.validateExistingAspect(this, aspectData) }

    private fun AspectData.checkAspectDataConsistent(): AspectData =
        this.also { aspectValidator.checkAspectDataConsistent(this) }

    private fun AspectData.checkBusinessKey() = this.also { aspectValidator.checkBusinessKey(this) }

    private fun AspectVertex.savePropertyWithHistory(
        vertex: AspectPropertyVertex,
        data: AspectPropertyData,
        context: HistoryContext
    ): HistoryFactWrite {
        return if (vertex.isJustCreated()) {
            aspectDaoService.saveAspectProperty(this, vertex, data)
            vertex.toCreateFact(context)
        } else {
            val previous = vertex.currentSnapshot()
            aspectDaoService.saveAspectProperty(this, vertex, data)
            vertex.toUpdateFact(context, previous)
        }
    }

    private fun AspectVertex.saveAspectProperties(propertyData: List<AspectPropertyData>, context: HistoryContext) {
        propertyData.forEach {
            val aspectPropertyVertex = it.getOrCreatePropertyVertex()
            historyService.storeFact(savePropertyWithHistory(aspectPropertyVertex, it, context))
        }
    }
}

private fun AspectData.normalize(): AspectData = copy(
    name = this.name?.trim(),
    description = this.description?.trim(),
    properties = this.properties.map { it.copy(name = it.name.trim(), description = it.description?.trim()) })

sealed class AspectException(message: String? = null) : Exception(message)

class AspectAlreadyExist(val name: String, subject: String?) :
    AspectException("name = $name, subject ${subject ?: "GLOBAL"}")

class AspectDoesNotExist(val id: String) : AspectException("id = $id")

class AspectPropertyDoesNotExist(val id: String) : AspectException("id = $id")

class AspectConcurrentModificationException(val id: String, message: String?) :
    AspectException("id = $id, message = $message")

class AspectModificationException(val id: String, message: String?) : AspectException("id = $id, message = $message")

class AspectPropertyConcurrentModificationException(val id: String, message: String?) :
    AspectException("id = $id, message = $message")

class AspectPropertyModificationException(val id: String, message: String?) :
    AspectException("id = $id, message = $message")

class AspectCyclicDependencyException(cyclicIds: List<String>) :
    AspectException("Cyclic dependencies on aspects with id: $cyclicIds")

class AspectWithoutBaseTypeException(id: String) : AspectException("Aspect with id $id does not have base type")


class AspectNameCannotBeNull : AspectException()
class AspectHasLinkedEntitiesException(val id: String) : AspectException("Some entities refer to aspect $id")
class AspectInconsistentStateException(message: String) : AspectException(message)

class AspectEmptyChangeException : AspectException()