package com.infowings.catalog.data

import com.infowings.catalog.common.*
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
        AspectPropertyData(id, name, aspect, power, version)


/**
 * Data layer for Aspect & Aspect properties
 * Both stored as vertexes [ASPECT_CLASS] & [ASPECT_PROPERTY_CLASS] linked by [ASPECT_ASPECTPROPERTY_EDGE]
 * [ASPECT_CLASS] can be linked with [Measure] by [ASPECT_MEASURE_CLASS]
 */
class AspectService(private val db: OrientDatabase, private val measureService: MeasureService) {

    /**
     * Creates new Aspect if [id] = null or empty and saves it into DB else updating existing
     * @throws AspectAlreadyExist,
     * @throws IllegalArgumentException in case of incorrect input data,
     * @throws AspectDoesNotExist if some AspectProperty has incorrect aspect id
     */
    fun saveAspect(aspectData: AspectData): Aspect {
        logger.trace("Saving aspect ${aspectData.name}, ${aspectData.measure}, ${aspectData.baseType}, ${aspectData.properties.size}")
        checkAspectData(aspectData)

        val save: OVertex = transaction(db) { session ->

            val aspectId = aspectData.id
            val aspectVertex: OVertex = if (aspectId?.isEmpty() == false) {
                db.getVertexById(aspectId)?.also {
                    checkAspectVersion(it, aspectData)
                } ?: throw IllegalStateException("Aspect with id $aspectId does not exist")
            } else {
                checkBusinessKey(aspectData.name, aspectData.measure)
                session.newVertex(ASPECT_CLASS)
            }

            aspectVertex["name"] = aspectData.name

            aspectVertex["baseType"] = when (aspectData.measure) {
                null -> aspectData.baseType
                else -> null
            }

            aspectVertex["measure"] = aspectData.measure

            val measureVertex: OVertex? = aspectData.measure?.let {
                measureService.findMeasure(it) ?: throw IllegalArgumentException("Measure $it does not exist")
            }

            measureVertex?.let { aspectVertex.addEdge(it, ASPECT_MEASURE_CLASS).save<OEdge>() }

            if (aspectData.properties.distinctBy { it.name }.size != aspectData.properties.size) {
                throw IllegalArgumentException("Properties for aspect should have different names")
            }

            val savedProperties = aspectVertex.properties.map { it.id }

            // now without removing
            aspectData.properties
                    .map { it.saveAspectProperty() }
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
    fun loadAspectProperty(propertyId: String): AspectProperty =
            db.getVertexById(propertyId)?.toAspectProperty()
                    ?: throw AspectPropertyDoesNotExist(propertyId)


    private fun checkAspectVersion(aspectVertex: OVertex, aspectData: AspectData) {
        if (aspectVertex.version != aspectData.version) {
            throw AspectModificationException(aspectVertex.id, "Old version")
        }
        val realVersionMap = aspectVertex.properties.map { it.id to it.version }.toMap()
        val receivedVersionMap = aspectData.properties.filter { it.id != "" }.map { it.id to it.version }.toMap()

        if (realVersionMap.keys.size != receivedVersionMap.keys.size) {
            throw AspectModificationException(aspectVertex.id, "Old version")
        }

        val different = realVersionMap.any { (k, v) -> v != receivedVersionMap[k] }
        if (different) {
            throw AspectModificationException(aspectVertex.id, "Old version")
        }
    }

    private fun AspectPropertyData.saveAspectProperty(): OVertex = transaction(db) { session ->
        logger.trace("Saving aspect property $name$ linked with aspect $aspectId")

        val aspectVertex: OVertex = db.getVertexById(aspectId) ?: throw AspectDoesNotExist(aspectId)
        val power = AspectPropertyPower.valueOf(power)
        val aspectPropertyVertex: OVertex = if (!id.isEmpty()) {
            db.getVertexById(id) ?: throw IllegalArgumentException("Incorrect property id")
        } else {
            session.newVertex(ASPECT_PROPERTY_CLASS)
        }

        aspectPropertyVertex["name"] = name
        aspectPropertyVertex["aspectId"] = aspectId
        aspectPropertyVertex["power"] = power.name

        if (!aspectPropertyVertex.getVertices(ODirection.OUT, ASPECT_ASPECTPROPERTY_EDGE).contains(aspectVertex)) {
            aspectPropertyVertex.addEdge(aspectVertex, ASPECT_ASPECTPROPERTY_EDGE).save<OEdge>()
        }

        return@transaction aspectPropertyVertex.save<OVertex>().also {
            logger.trace("Saved aspect property $name with temporary id: ${it.id}")
        }
    }

    private fun loadProperties(oVertex: OVertex): List<AspectProperty> = transaction(db) {
        oVertex
                .getVertices(ODirection.OUT, ASPECT_ASPECTPROPERTY_EDGE)
                .map { loadAspectProperty(it.id) }
    }

    private fun checkBusinessKey(name: String, measure: String?) {
        if (findByName(name).any { it.measure?.name == measure }) {
            throw AspectAlreadyExist(name, measure)
        }
    }

    private fun checkAspectData(aspectData: AspectData) {
        if (aspectData.measure != null
                && aspectData.baseType != null
                && GlobalMeasureMap[aspectData.measure!!]!!.baseType != BaseType.restoreBaseType(aspectData.baseType)) {

            throw IllegalArgumentException("Measure and base type relation incorrect")
        }
    }

    private fun OVertex.toAspect(): Aspect =
            Aspect(id, name, measure, baseType?.let { OpenDomain(it) }, baseType, loadProperties(this), version)

    private fun OVertex.toAspectProperty(): AspectProperty =
            AspectProperty(id, name, findById(aspect), AspectPropertyPower.valueOf(power), version)
}

private const val selectFromAspect = "SELECT FROM Aspect"
private const val selectAspectByName = "SELECT FROM Aspect where name = ? "

private val logger = loggerFor<AspectService>()

sealed class AspectException(message: String? = null) : Exception(message)
class AspectAlreadyExist(val name: String, val measure: String?) : AspectException("name = $name, measure = $measure")
class AspectDoesNotExist(val id: String) : AspectException("id = $id")
class AspectPropertyDoesNotExist(val id: String) : AspectException("id = $id")
class AspectModificationException(val id: String, message: String?) : AspectException("id = $id, message = $message")

private val OVertex.properties: List<OVertex>
    get() = getVertices(ODirection.OUT, ASPECT_ASPECTPROPERTY_EDGE).toList()
private val OVertex.baseType: BaseType?
    get() = measure?.baseType ?: BaseType.restoreBaseType(this["baseType"])
private val OVertex.name: String
    get() = this["name"]
private val OVertex.aspect: String
    get() = this["aspectId"]
private val OVertex.power: String
    get() = this["power"]
private val OVertex.measure: Measure<*>?
    get() = GlobalMeasureMap[measureName]
private val OVertex.measureName: String?
    get() = this["measure"]