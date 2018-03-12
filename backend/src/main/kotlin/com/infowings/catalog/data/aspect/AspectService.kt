package com.infowings.catalog.data.aspect

import com.infowings.catalog.common.*
import com.infowings.catalog.data.MeasureService
import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.*
import com.infowings.catalog.storage.transaction
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OEdge
import com.orientechnologies.orient.core.record.OVertex


/**
 * Public OVertex Extensions.
 */
fun OVertex.toAspectData(): AspectData =
        AspectData(id, name, measureName, baseType?.let { OpenDomain(it).toString() }, baseType?.name, properties.map { it.toAspectPropertyData() }, version)

fun OVertex.toAspectPropertyData(): AspectPropertyData =
        AspectPropertyData(id, name, aspect, cardinality, version)


/**
 * Data layer for Aspect & Aspect properties
 * Both stored as vertexes [ASPECT_CLASS] & [ASPECT_PROPERTY_CLASS] linked by [ASPECT_ASPECTPROPERTY_EDGE]
 * [ASPECT_CLASS] can be linked with [Measure] by [ASPECT_MEASURE_CLASS]
 */
class AspectService(private val db: OrientDatabase, private val measureService: MeasureService) {

    private val aspectValidator = AspectValidator(db, this)

    /**
     * Creates new Aspect if [id] = null or empty and saves it into DB else updating existing
     * @throws AspectAlreadyExist,
     * @throws IllegalArgumentException in case of incorrect input data,
     * @throws AspectDoesNotExist if some AspectProperty has incorrect aspect id
     */
    fun save(aspectData: AspectData): Aspect {
        logger.trace("Saving aspect ${aspectData.name}, ${aspectData.measure}, ${aspectData.baseType}, ${aspectData.properties.size}")
        val save: OVertex = transaction(db) {

            aspectValidator.checkAspectData(aspectData)
            aspectValidator.checkBusinessKey(aspectData)

            val aspectVertex: OVertex = aspectValidator.aspectVertex(aspectData)

            aspectVertex["name"] = aspectData.name

            aspectVertex["baseType"] = when (aspectData.measure) {
                null -> aspectData.baseType
                else -> null
            }

            aspectVertex["measure"] = aspectData.measure

            val measureVertex: OVertex? = aspectData.measure?.let {
                measureService.findMeasure(it) ?: throw IllegalArgumentException("Measure $it does not exist")
            }

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

        logger.trace("Aspect ${aspectData.name} saved with id: ${save.id}")
        return findById(save.id)
    }

    /**
     * Search [Aspect] by it's name
     * @return List of [Aspect] with name [name]
     */
    fun findByName(name: String): Set<Aspect> = db.query(selectAspectByName, name) { rs ->
        rs.map { it.toVertex().toAspect() }.toSet()
    }

    fun getAspects(): List<Aspect> = db.query(selectFromAspect) { rs ->
        rs.mapNotNull { it.toVertexOrNUll()?.toAspect() }.toList()
    }

    /**
     * Search [Aspect] by it's id
     * @throws AspectDoesNotExist
     */
    fun findById(id: String): Aspect = db.getVertexById(id)?.toAspect() ?: throw AspectDoesNotExist(id)

    /**
     * Load property by id
     * @throws AspectPropertyDoesNotExist
     */
    private fun loadAspectProperty(propertyId: String): AspectProperty =
            db.getVertexById(propertyId)?.toAspectProperty()
                    ?: throw AspectPropertyDoesNotExist(propertyId)

    private fun saveAspectProperty(aspectPropertyData: AspectPropertyData): OVertex = transaction(db) {
        logger.trace("Saving aspect property $aspectPropertyData.name$ linked with aspect $aspectPropertyData.aspectId")

        val aspectVertex: OVertex = db.getVertexById(aspectPropertyData.aspectId)
                ?: throw AspectDoesNotExist(aspectPropertyData.aspectId)
        val cardinality = AspectPropertyCardinality.valueOf(aspectPropertyData.cardinality)

        val aspectPropertyVertex = aspectValidator.aspectPropertyVertex(aspectPropertyData)

        aspectPropertyVertex["name"] = aspectPropertyData.name
        aspectPropertyVertex["aspectId"] = aspectPropertyData.aspectId
        aspectPropertyVertex["cardinality"] = cardinality.name

        // it is not aspectPropertyVertex.properties in mind. This links describe property->aspect relation
        if (!aspectPropertyVertex.getVertices(ODirection.OUT, ASPECT_ASPECTPROPERTY_EDGE).contains(aspectVertex)) {
            aspectPropertyVertex.addEdge(aspectVertex, ASPECT_ASPECTPROPERTY_EDGE).save<OEdge>()
        }

        return@transaction aspectPropertyVertex.save<OVertex>().also {
            logger.trace("Saved aspect property ${aspectPropertyData.name} with temporary id: ${it.id}")
        }
    }

    private fun loadProperties(oVertex: OVertex): List<AspectProperty> = transaction(db) {
        oVertex
                .properties
                .map { loadAspectProperty(it.id) }
    }

    private fun OVertex.toAspect(): Aspect =
            Aspect(id, name, measure, baseType?.let { OpenDomain(it) }, baseType, loadProperties(this), version)

    private fun OVertex.toAspectProperty(): AspectProperty =
            AspectProperty(id, name, findById(aspect), AspectPropertyCardinality.valueOf(cardinality), version)
}

private const val selectFromAspect = "SELECT FROM Aspect"
private const val selectAspectByName = "SELECT FROM Aspect where name = ? "

private val logger = loggerFor<AspectService>()

sealed class AspectException(message: String? = null) : Exception(message)
class AspectAlreadyExist(val name: String, val measure: String?) : AspectException("name = $name, measure = $measure")
class AspectDoesNotExist(val id: String) : AspectException("id = $id")
class AspectPropertyDoesNotExist(val id: String) : AspectException("id = $id")
class AspectModificationException(val id: String, message: String?) : AspectException("id = $id, message = $message")
class AspectPropertyModificationException(val id: String, message: String?) : AspectException("id = $id, message = $message")

internal val OVertex.properties: List<OVertex>
    get() = getVertices(ODirection.OUT, ASPECT_ASPECTPROPERTY_EDGE).toList()
internal val OVertex.baseType: BaseType?
    get() = measure?.baseType ?: BaseType.restoreBaseType(this["baseType"])
internal val OVertex.name: String
    get() = this["name"]
internal val OVertex.aspect: String
    get() = this["aspectId"]
internal val OVertex.cardinality: String
    get() = this["cardinality"]
internal val OVertex.measure: Measure<*>?
    get() = GlobalMeasureMap[measureName]
internal val OVertex.measureName: String?
    get() = this["measure"]