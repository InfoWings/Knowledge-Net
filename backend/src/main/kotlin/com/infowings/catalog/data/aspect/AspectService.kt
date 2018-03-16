package com.infowings.catalog.data.aspect

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import com.infowings.catalog.common.BaseType
import com.infowings.catalog.common.Measure
import com.infowings.catalog.data.MeasureService
import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.*
import com.infowings.catalog.storage.transaction
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OEdge
import com.orientechnologies.orient.core.record.OVertex


/**
 * Data layer for Aspect & Aspect properties
 * Both stored as vertexes [ASPECT_CLASS] & [ASPECT_PROPERTY_CLASS] linked by [ASPECT_ASPECTPROPERTY_EDGE]
 * [ASPECT_CLASS] can be linked with [Measure] by [ASPECT_MEASURE_CLASS]
 */
class AspectService(private val db: OrientDatabase, private val measureService: MeasureService) {

    private val aspectValidator = AspectValidator(db)

    /**
     * Creates new Aspect if [id] = null or empty and saves it into DB else updating existing
     * @throws AspectAlreadyExist,
     * @throws IllegalArgumentException in case of incorrect input data,
     * @throws AspectDoesNotExist if some AspectProperty has incorrect aspect id
     */
    fun save(aspectData: AspectData): Aspect {
        logger.debug("Saving aspect ${aspectData.name}, ${aspectData.measure}, ${aspectData.baseType}, ${aspectData.properties.size}")
        val save: OVertex = transaction(db) {

            aspectValidator.checkAspectDataConsistent(aspectData)
            aspectValidator.checkBusinessKey(aspectData)

            val aspectVertex: AspectVertex = createOrGetAspectVertex(aspectData)

            aspectVertex.name = aspectData.name

            aspectVertex.baseType = when (aspectData.measure) {
                null -> aspectData.baseType
                else -> null
            }

            aspectVertex.measureName = aspectData.measure

            val measureVertex: OVertex? = aspectData.measure?.let { measureService.findMeasure(it) }

            measureVertex?.let {
                if (!aspectVertex.getVertices(ODirection.OUT, ASPECT_MEASURE_CLASS).contains(it)) {
                    aspectVertex.addEdge(it, ASPECT_MEASURE_CLASS).save<OEdge>()
                }
            }

            val savedProperties = aspectVertex.properties.map { it.id }

            // now without removing
            aspectData.properties
                    .map { saveAspectProperty(it) }
                    .filter { !savedProperties.contains(it.id) }
                    .forEach { aspectVertex.addEdge(it, ASPECT_ASPECTPROPERTY_EDGE).save<OEdge>() }

            return@transaction aspectVertex.save()
        }

        logger.debug("Aspect ${aspectData.name} saved with id: ${save.id}")
        return findById(save.id)
    }

    /**
     * Search [Aspect] by it's name
     * @return List of [Aspect] with name [name]
     */
    fun findByName(name: String): Set<Aspect> = db.query(selectAspectByName, name) { rs ->
        rs.map { it.toVertex().toAspectVertex().toAspect() }.toSet()
    }

    fun getAspects(): List<Aspect> = db.query(selectFromAspect) { rs ->
        rs.mapNotNull { it.toVertexOrNUll()?.toAspectVertex()?.toAspect() }.toList()
    }

    /**
     * Search [Aspect] by it's id
     * @throws AspectDoesNotExist
     */
    fun findById(id: String): Aspect = db.getVertexById(id)?.toAspectVertex()?.toAspect()
            ?: throw AspectDoesNotExist(id)

    /**
     * Load property by id
     * @throws AspectPropertyDoesNotExist
     */
    private fun loadAspectProperty(propertyId: String): AspectProperty =
            db.getVertexById(propertyId)?.toAspectPropertyVertex()?.toAspectProperty()
                    ?: throw AspectPropertyDoesNotExist(propertyId)

    private fun saveAspectProperty(aspectPropertyData: AspectPropertyData): OVertex = transaction(db) {
        logger.debug("Saving aspect property ${aspectPropertyData.name} linked with aspect ${aspectPropertyData.aspectId}")

        val aspectVertex: AspectVertex = db.getVertexById(aspectPropertyData.aspectId)?.toAspectVertex()
                ?: throw AspectDoesNotExist(aspectPropertyData.aspectId)

        val cardinality = AspectPropertyCardinality.valueOf(aspectPropertyData.cardinality)

        val aspectPropertyVertex = createOrGetAspectPropertyVertex(aspectPropertyData)

        aspectPropertyVertex.name = aspectPropertyData.name
        aspectPropertyVertex.aspect = aspectPropertyData.aspectId
        aspectPropertyVertex.cardinality = cardinality.name

        // it is not aspectPropertyVertex.properties in mind. This links describe property->aspect relation
        if (!aspectPropertyVertex.getVertices(ODirection.OUT, ASPECT_ASPECTPROPERTY_EDGE).contains(aspectVertex)) {
            aspectPropertyVertex.addEdge(aspectVertex, ASPECT_ASPECTPROPERTY_EDGE).save<OEdge>()
        }

        return@transaction aspectPropertyVertex.save<OVertex>().also {
            logger.debug("Saved aspect property ${aspectPropertyData.name} with temporary id: ${it.id}")
        }
    }

    private fun loadProperties(oVertex: AspectVertex): List<AspectProperty> = transaction(db) {
        oVertex
                .properties
                .map { loadAspectProperty(it.id) }
    }

    /**
     * Create empty vertex in case [aspectData.id] is null or empty
     * Otherwise validate and return vertex of class [ASPECT_CLASS] with given id
     * @throws IllegalStateException
     * @throws AspectConcurrentModificationException
     * */
    private fun createOrGetAspectVertex(aspectData: AspectData): AspectVertex {
        val aspectId = aspectData.id

        return if (!aspectId.isNullOrEmpty()) {

            db.getVertexById(aspectId!!)?.toAspectVertex()?.also {
                // if aspect is exist, we should validate it with new data
                aspectValidator.validateExistingAspect(it, aspectData)
            } ?: throw IllegalStateException("Aspect with id $aspectId does not exist")

        } else {
            db.createNewVertex(ASPECT_CLASS).toAspectVertex()
        }
    }

    /**
     * Create empty vertex in case [aspectPropertyData.id] is null or empty
     * Otherwise validate and return vertex of class [ASPECT_PROPERTY_CLASS] with given id
     * @throws IllegalStateException
     * @throws AspectPropertyModificationException
     * */
    private fun createOrGetAspectPropertyVertex(aspectPropertyData: AspectPropertyData): AspectPropertyVertex {
        val propertyId = aspectPropertyData.id

        return if (!propertyId.isEmpty()) {
            db.getVertexById(propertyId)?.toAspectPropertyVertex()?.also {
                // if aspect property is exist, we should validate it with new data
                aspectValidator.validateExistingAspectProperty(it, aspectPropertyData)
            } ?: throw IllegalArgumentException("Incorrect property id")

        } else {
            db.createNewVertex(ASPECT_PROPERTY_CLASS).toAspectPropertyVertex()
        }
    }

    private fun AspectVertex.toAspect(): Aspect {
        val baseTypeObj = baseType?.let { BaseType.restoreBaseType(it) }
        return Aspect(id, name, measure, baseTypeObj?.let { OpenDomain(it) }, baseTypeObj, loadProperties(this), version)
    }

    private fun AspectPropertyVertex.toAspectProperty(): AspectProperty =
            AspectProperty(id, name, findById(aspect), AspectPropertyCardinality.valueOf(cardinality), version)
}

private const val selectFromAspect = "SELECT FROM Aspect"
private const val selectAspectByName = "SELECT FROM Aspect where name = ? "

private val logger = loggerFor<AspectService>()

sealed class AspectException(message: String? = null) : Exception(message)
class AspectAlreadyExist(val name: String) : AspectException("name = $name")
class AspectDoesNotExist(val id: String) : AspectException("id = $id")
class AspectPropertyDoesNotExist(val id: String) : AspectException("id = $id")
class AspectConcurrentModificationException(val id: String, message: String?) : AspectException("id = $id, message = $message")
class AspectModificationException(val id: String, message: String?) : AspectException("id = $id, message = $message")
class AspectPropertyModificationException(val id: String, message: String?) : AspectException("id = $id, message = $message")
