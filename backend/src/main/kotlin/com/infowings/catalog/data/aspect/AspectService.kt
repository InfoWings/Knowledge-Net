package com.infowings.catalog.data.aspect

import com.infowings.catalog.auth.UserAcceptService
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import com.infowings.catalog.common.BaseType
import com.infowings.catalog.common.Measure
import com.infowings.catalog.data.history.HistoryFact
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.storage.*
import com.infowings.catalog.storage.transaction

/**
 * Data layer for Aspect & Aspect properties
 * Both stored as vertexes [ASPECT_CLASS] & [ASPECT_PROPERTY_CLASS] linked by [ASPECT_ASPECT_PROPERTY_EDGE]
 * [ASPECT_CLASS] can be linked with [Measure] by [ASPECT_MEASURE_CLASS]
 */
class AspectService(
    private val db: OrientDatabase,
    private val aspectDaoService: AspectDaoService,
    private val historyService: HistoryService,
    private val userAcceptService: UserAcceptService
) {
    private val aspectValidator = AspectValidator(aspectDaoService)

    private fun savePlain(aspectVertex: AspectVertex, aspectData: AspectData, context: HistoryContext): AspectVertex {
        val (deletedProperties, updatedProperties) = aspectData.properties.partition { it.deleted }
        deletedProperties.forEach { remove(it, context) }
        aspectVertex.saveAspectProperties(updatedProperties, context)
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

    /**
     * Creates new Aspect if [id] = null or empty and saves it into DB else updating existing
     * @param aspectData data that represents Aspect, which will be saved or updated
     * @throws AspectAlreadyExist,
     * @throws IllegalArgumentException in case of incorrect input data,
     * @throws AspectDoesNotExist if some AspectProperty has incorrect aspect id
     * @throws AspectCyclicDependencyException if one of AspectProperty of the aspect refers to parent Aspect
     */
    fun save(aspectData: AspectData, user: String = ""): Aspect {
        val userInfo: String? = userAcceptService.findByUsernameAsJson(user)

        val save: AspectVertex = transaction(db) {
            val aspectVertex = aspectData
                .checkAspectDataConsistent()
                .checkBusinessKey()
                .getOrCreateAspectVertex()

            val finishMethod  = if (aspectVertex.identity.isNew) this::createFinish else this::updateFinish

            return@transaction finishMethod(aspectVertex, aspectData, HistoryContext(user, userInfo))
        }

        return findById(save.id)
    }

    fun remove(aspectData: AspectData, userName: String, force: Boolean = false) {
        val userInfo: String? = userAcceptService.findByUsernameAsJson(userName)
        val context = HistoryContext(userName, userInfo)

        transaction(db) {
            val aspectId = aspectData.id ?: "null"

            val aspectVertex =
                aspectDaoService.getVertex(aspectId)?.toAspectVertex() ?: throw AspectDoesNotExist(aspectId)

            aspectVertex.checkAspectVersion(aspectData)

            when {
                aspectVertex.isLinkedBy() && force -> {
                    historyService.storeFact(aspectVertex.toSoftDeleteFact(context))
                    aspectDaoService.fakeRemove(aspectVertex)
                }
                aspectVertex.isLinkedBy() -> {
                    throw AspectHasLinkedEntitiesException(aspectId)
                }

                else -> {
                    historyService.storeFact(aspectVertex.toDeleteFact(context))
                    aspectDaoService.remove(aspectVertex)
                }
            }
        }
    }

    /**
     * Search [Aspect] by it's name
     * @return List of [Aspect] with name [name]
     */
    fun findByName(name: String): Set<Aspect> = aspectDaoService.findByName(name).map { it.toAspect() }.toSet()

    fun getAspects(): List<Aspect> = aspectDaoService.getAspects().map { it.toAspect() }.toList()

    /**
     * Search [Aspect] by it's id
     * @throws AspectDoesNotExist
     */
    fun findById(id: String): Aspect = aspectDaoService.getAspectVertex(id)?.toAspect() ?: throw AspectDoesNotExist(id)

    fun findPropertyVertexById(id: String): AspectPropertyVertex = aspectDaoService.getAspectPropertyVertex(id)
            ?: throw AspectPropertyDoesNotExist(id)


    /** Method is private and it is supposed that version checking successfully accepted before. */
    private fun remove(property: AspectPropertyData, context: HistoryContext) = transaction(db) {
        historyService.storeFact(findPropertyVertexById(property.id).toDeleteFact(context))

        val vertex = aspectDaoService.getAspectPropertyVertex(property.id)
                ?: throw AspectPropertyDoesNotExist(property.id)

        return@transaction aspectDaoService.remove(vertex)
    }

    /**
     * Load property by id
     * @throws AspectPropertyDoesNotExist
     */
    private fun loadAspectProperty(propertyId: String): AspectProperty =
        aspectDaoService.getAspectPropertyVertex(propertyId)?.toAspectProperty()
                ?: throw AspectPropertyDoesNotExist(propertyId)

    private fun loadProperties(aspectVertex: AspectVertex): List<AspectProperty> = transaction(db) {
        aspectVertex.properties.map { loadAspectProperty(it.id) }
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

    private fun AspectVertex.toAspect(): Aspect = transaction(db) {
        val baseTypeObj = baseType?.let { BaseType.restoreBaseType(it) }
        Aspect(
            id,
            name,
            measure,
            baseTypeObj?.let { OpenDomain(it) },
            baseTypeObj,
            loadProperties(this),
            version,
            subject,
            deleted,
            description
        )
    }

    private fun AspectPropertyVertex.toAspectProperty(): AspectProperty =
        AspectProperty(id, name, findById(aspect), AspectPropertyCardinality.valueOf(cardinality), version)

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
    ): HistoryFact {
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

data class HistoryContext(val user: String, val userInfo: String?)

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


class AspectNameCannotBeNull : AspectException()
class AspectHasLinkedEntitiesException(val id: String) : AspectException("Some entities refer to aspect $id")

class AspectInconsistentStateException(message: String) : AspectException(message)