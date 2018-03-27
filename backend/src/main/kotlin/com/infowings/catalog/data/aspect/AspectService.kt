package com.infowings.catalog.data.aspect

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import com.infowings.catalog.common.BaseType
import com.infowings.catalog.common.Measure
import com.infowings.catalog.data.history.*
import com.infowings.catalog.loggerFor
import com.infowings.catalog.search.SuggestionService
import com.infowings.catalog.storage.*
import com.infowings.catalog.storage.transaction
import hasIncomingEdges
import org.springframework.boot.autoconfigure.security.SecurityProperties


/**
 * Data layer for Aspect & Aspect properties
 * Both stored as vertexes [ASPECT_CLASS] & [ASPECT_PROPERTY_CLASS] linked by [ASPECT_ASPECTPROPERTY_EDGE]
 * [ASPECT_CLASS] can be linked with [Measure] by [ASPECT_MEASURE_CLASS]
 */
class AspectService(private val db: OrientDatabase,
                    private val aspectDaoService: AspectDaoService,
                    private val historyService: HistoryService,
                    suggestionService: SuggestionService) {

    private val aspectValidator = AspectValidator(aspectDaoService, suggestionService)

    /**
     * Creates new Aspect if [id] = null or empty and saves it into DB else updating existing
     * @param aspectData data that represents Aspect, which will be saved or updated
     * @throws AspectAlreadyExist,
     * @throws IllegalArgumentException in case of incorrect input data,
     * @throws AspectDoesNotExist if some AspectProperty has incorrect aspect id
     * @throws AspectCyclicDependencyException if one of AspectProperty of the aspect refers to parent Aspect
     */
    fun save(aspectData: AspectData, user: String = ""): Aspect {
        val isCreate = aspectData.id == null
        val logger = loggerFor<AspectService>()

        val save: AspectVertex = transaction(db) {

            val aspectVertex = aspectData
                    .checkAspectDataConsistent()
                    .checkBusinessKey()
                    .getOrCreateAspectVertex()

            logger.info("aspectVertex id-1: ${aspectVertex.id}")

            aspectVertex.saveAspectProperties(aspectData.properties)

            logger.info("aspectVertex id-2: ${aspectVertex.id}")

            val res = aspectDaoService.saveAspect(aspectVertex, aspectData)

            logger.info("res: ${res.id}, ${aspectVertex.id}")

            if (isCreate) {
                val event = aspectVertex.toHistoryEvent(user, aspectVertex.toCreatePayload())
                historyService.storeEvent(event)
            }

            return@transaction res
        }

        logger.info("save.id: ${save.id}")

        val result =  findById(save.id)

        logger.info("result: ${result.id}")

        return result
    }

    fun remove(aspect: Aspect, force: Boolean = false) = transaction(db) {
        val vertex = aspectDaoService.getVertex(aspect.id) ?: throw AspectDoesNotExist(aspect.id)

        val aspectVertex = vertex.toAspectVertex()

        aspectVertex.checkAspectVersion(aspect.toAspectData())

        when {
            aspectVertex.isLinkedBy() && force -> {
                // сюда - удаление связанного
            }
            aspectVertex.isLinkedBy() -> {
                throw AspectHasLinkedEntitiesException(aspect.id)
            }
            else ->
                aspectDaoService.remove(vertex)
        }
    }

    fun remove(property: AspectProperty) = transaction(db) {
        val vertex = aspectDaoService.getVertex(property.id) ?: throw AspectPropertyDoesNotExist(property.id)

        if (property.version != vertex.version) {
            throw AspectPropertyConcurrentModificationException(property.id,
                    "found aspect version ${vertex.version}" +
                            " instead of ${vertex.version}")
        }

        aspectDaoService.remove(vertex)
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
     * Create empty vertex in case [aspectData.id] is null or empty
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
     * Create empty vertex in case [aspectPropertyData.id] is null or empty
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

    private fun AspectVertex.toAspect(): Aspect {
        val baseTypeObj = baseType?.let { BaseType.restoreBaseType(it) }
        return Aspect(id, name, measure, baseTypeObj?.let { OpenDomain(it) }, baseTypeObj, loadProperties(this), version)
    }

    private fun AspectPropertyVertex.toAspectProperty(): AspectProperty =
            AspectProperty(id, name, findById(aspect), AspectPropertyCardinality.valueOf(cardinality), version)

    private fun AspectPropertyVertex.validateExistingAspectProperty(aspectPropertyData: AspectPropertyData): AspectPropertyVertex = this.also { aspectValidator.validateExistingAspectProperty(this, aspectPropertyData) }

    private fun AspectVertex.validateExistingAspect(aspectData: AspectData): AspectVertex = this.also { aspectValidator.validateExistingAspect(this, aspectData) }

    private fun AspectData.checkAspectDataConsistent(): AspectData = this.also { aspectValidator.checkAspectDataConsistent(this) }

    private fun AspectData.checkBusinessKey() = this.also { aspectValidator.checkBusinessKey(this) }

    private fun AspectVertex.saveAspectProperties(propertyData: List<AspectPropertyData>) {
        propertyData.forEach {
            val aspectPropertyVertex = it.getOrCreatePropertyVertex()
            aspectDaoService.saveAspectProperty(this, aspectPropertyVertex, it)
        }
    }
}

sealed class AspectException(message: String? = null) : Exception(message)
class AspectAlreadyExist(val name: String) : AspectException("name = $name")
class AspectDoesNotExist(val id: String) : AspectException("id = $id")
class AspectPropertyDoesNotExist(val id: String) : AspectException("id = $id")
class AspectConcurrentModificationException(val id: String, message: String?) : AspectException("id = $id, message = $message")
class AspectModificationException(val id: String, message: String?) : AspectException("id = $id, message = $message")
class AspectPropertyConcurrentModificationException(val id: String, message: String?) : AspectException("id = $id, message = $message")
class AspectPropertyModificationException(val id: String, message: String?) : AspectException("id = $id, message = $message")
class AspectCyclicDependencyException(cyclicIds: List<String>) :
        AspectException("Cyclic dependencies on aspects with id: $cyclicIds")
class AspectHasLinkedEntitiesException(val id: String): AspectException("Some entities refer to aspect $id")