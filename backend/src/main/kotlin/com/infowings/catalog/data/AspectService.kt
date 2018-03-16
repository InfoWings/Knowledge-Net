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
    AspectPropertyData(id, name, aspect, cardinality, version)


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
    fun save(aspectData: AspectData): Aspect {
        logger.trace("Saving aspect ${aspectData.name}, ${aspectData.measure}, ${aspectData.baseType}, ${aspectData.properties.size}")
        checkAspectData(aspectData)
        checkBusinessKey(aspectData)

        val save: OVertex = transaction(db) { session ->

            val aspectId = aspectData.id
            val aspectVertex: OVertex = if (aspectId?.isEmpty() == false) {
                db.getVertexById(aspectId)?.also { validateExistingAspect(it, aspectData) }
                        ?: throw IllegalStateException("Aspect with id $aspectId does not exist")
            } else {
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

            measureVertex?.let {
                if (!aspectVertex.getVertices(ODirection.OUT, ASPECT_MEASURE_CLASS).contains(it)) {
                    aspectVertex.addEdge(it, ASPECT_MEASURE_CLASS).save<OEdge>()
                }
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
        rs.mapNotNull { it.toVertexOrNull()?.toAspect() }.toList()
    }

    fun getAspect(vertex: OVertex): Aspect = vertex.toAspect()

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


    private fun validateExistingAspect(aspectVertex: OVertex, aspectData: AspectData) {
        checkAspectVersion(aspectVertex, aspectData)
        checkBaseTypeChangeCriteria(aspectVertex, aspectData)
        checkMeasureChangeCriteria(aspectVertex, aspectData)
    }

    private fun validateExistingAspectProperty(aspectPropertyVertex: OVertex, aspectPropertyData: AspectPropertyData) {
        checkPropertyAspectChangeCriteria(aspectPropertyVertex, aspectPropertyData)
    }

    private fun checkAspectVersion(aspectVertex: OVertex, aspectData: AspectData) {
        if (aspectVertex.version != aspectData.version) {
            throw AspectModificationException(aspectVertex.id, "Old version, db: ${aspectVertex.version}, param: ${aspectData.version}")
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
        val cardinality = AspectPropertyCardinality.valueOf(this.cardinality)
        val aspectPropertyVertex: OVertex = if (!id.isEmpty()) {
            db.getVertexById(id)?.also { validateExistingAspectProperty(it, this) }
                    ?: throw IllegalArgumentException("Incorrect property id")
        } else {
            session.newVertex(ASPECT_PROPERTY_CLASS)
        }

        aspectPropertyVertex["name"] = name
        aspectPropertyVertex["aspectId"] = aspectId
        aspectPropertyVertex["cardinality"] = cardinality.name

        // it is not aspectPropertyVertex.properties in mind. This links describe property->aspect relation
        if (!aspectPropertyVertex.getVertices(ODirection.OUT, ASPECT_ASPECTPROPERTY_EDGE).contains(aspectVertex)) {
            aspectPropertyVertex.addEdge(aspectVertex, ASPECT_ASPECTPROPERTY_EDGE).save<OEdge>()
        }

        return@transaction aspectPropertyVertex.save<OVertex>().also {
            logger.trace("Saved aspect property $name with temporary id: ${it.id}")
        }
    }

    private fun loadProperties(oVertex: OVertex): List<AspectProperty> = transaction(db) {
        oVertex
                .properties
                .map { loadAspectProperty(it.id) }
    }

    private fun checkBusinessKey(aspectData: AspectData) {
        val name: String = aspectData.name ?: throw AspectNameIsNull(aspectData)
        // check aspect business key
        if (findByName(name).filter { it.id != aspectData.id }.any { it.measure?.name == aspectData.measure }) {
            throw AspectAlreadyExist(name, aspectData.measure)
        }
        // check aspect properties business key
        val valid = aspectData.properties.distinctBy { Pair(it.name, it.aspectId) }.size != aspectData.properties.size
        if (valid) {
            throw IllegalArgumentException("Not correct property business key $aspectData")
        }
    }

    private fun checkAspectData(aspectData: AspectData) {
        val measureString: String? = aspectData.measure
        val baseType: String? = aspectData.baseType

        if (measureString != null && baseType != null) {
            val measure: Measure<*> = GlobalMeasureMap[measureString]
                    ?: throw IllegalArgumentException("Measure $measureString incorrect")

            if (measure.baseType != BaseType.restoreBaseType(aspectData.baseType)) {
                throw IllegalArgumentException("Measure $measure and base type $baseType relation incorrect")
            }
        }
    }

    private fun checkBaseTypeChangeCriteria(aspectVertex: OVertex, aspectData: AspectData) {
        if (aspectData.baseType != aspectVertex.baseType?.name) {
            if ((aspectData.measure != null && aspectData.measure == aspectVertex.measureName)
            /* или сущуствует хотя бы один экземпляр Аспекта */) {

                throw AspectModificationException(aspectVertex.id, "Impossible to change base type")
            }
        }
    }

    private fun checkMeasureChangeCriteria(aspectVertex: OVertex, aspectData: AspectData) {
        if (aspectData.measure != aspectVertex.measureName) {
            val sameGroup = aspectVertex.measureName == aspectData.measure
            if (!sameGroup && false /* сущуствует хотя бы один экземпляр Аспекта */) {
                throw AspectModificationException(aspectVertex.id, "Impossible to change measure")
            }
        }
    }

    private fun checkPropertyAspectChangeCriteria(aspectVertex: OVertex, aspectPropertyData: AspectPropertyData) {
        if (aspectVertex.aspect != aspectPropertyData.id) {
            if (false /* сущуствует хотя бы один экземпляр Аспекта */) {
                throw AspectPropertyModificationException(aspectVertex.id, "Impossible to change aspectId")
            }
        }
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
class AspectNameIsNull(aspectData: AspectData) : AspectException("Illegal aspect data: $aspectData")

private val OVertex.properties: List<OVertex>
    get() = getVertices(ODirection.OUT, ASPECT_ASPECTPROPERTY_EDGE).toList()
private val OVertex.baseType: BaseType?
    get() = measure?.baseType ?: BaseType.restoreBaseType(this["baseType"])
private val OVertex.name: String
    get() = this["name"]
private val OVertex.aspect: String
    get() = this["aspectId"]
private val OVertex.cardinality: String
    get() = this["cardinality"]
private val OVertex.measure: Measure<*>?
    get() = GlobalMeasureMap[measureName]
private val OVertex.measureName: String?
    get() = this["measure"]