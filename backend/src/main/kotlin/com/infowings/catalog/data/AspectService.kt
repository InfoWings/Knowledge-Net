package com.infowings.catalog.data

import com.infowings.catalog.common.*
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

    /**
     * Creates new Aspect and saves it into DB
     * @throws AspectAlreadyExist,
     * @throws IllegalArgumentException in case of incorrect input data,
     * @throws AspectDoesNotExist if some AspectProperty has incorrect aspect id
     */
    fun createAspect(aspectData: AspectData): Aspect {
        findByName(aspectData.name)?.let { throw AspectAlreadyExist(it.id) }

        val measure: Measure<*>? = GlobalMeasureMap[aspectData.measure]

        val baseType: BaseType? = when {
            aspectData.baseType != null -> BaseType.restoreBaseType(aspectData.baseType)
            measure != null -> measure.baseType
            else -> BaseType.Nothing
        }

        if (baseType != null && measure != null && baseType != measure.baseType)
            throw IllegalArgumentException("Base type and measure base type should be the same")

        return save(aspectData.name, measure, baseType, aspectData.properties)
    }

    /**
     * Search [Aspect] by it's name
     * @return null if nothing's found
     * todo: немного неконсистентно где-то летит исключение, где-то null
     */
    fun findByName(name: String): Aspect? = db.query(selectAspectByName, name) { rs ->
        rs.map { it.toVertex().toAspect() }.firstOrNull()
    }

    fun getAspects(): List<Aspect> = db.query(selectFromAspect) { rs ->
        rs.mapNotNull { it.toVertexOrNUll()?.toAspect() }.toList()
    }

    /**
     * Search [Aspect] by it's id
     * @throws AspectDoesNotExist
     */
    fun findById(id: String): Aspect = db.getVertexById(id)?.toAspect() ?: throw AspectDoesNotExist(id)

    fun changeName(id: String, newName: String): Aspect = transaction(db) {
        val aspectVertex = db.getVertexById(id) ?: throw AspectDoesNotExist(id)
        findByName(newName)?.let { throw AspectModificationException(id, "Aspect with name $newName already exists") }
        aspectVertex["name"] = newName
        return@transaction aspectVertex.save<OVertex>()
    }.let { findById(id) }

    fun changeBaseType(id: String, newType: BaseType): Aspect = transaction(db) {
        val aspectVertex = db.getVertexById(id) ?: throw AspectDoesNotExist(id)
        aspectVertex.get<String?>("measure")?.let { throw AspectModificationException(id, "Aspect has a measure") }
        // todo: существует хотябы одно значения данного аспекта ---> throw AspectModificationException
        aspectVertex["baseType"] = newType.name
        return@transaction aspectVertex.save<OVertex>()
    }.let { findById(id) }

    fun changeMeasure(id: String, measure: Measure<*>): Aspect = transaction(db) {
        val aspectVertex = db.getVertexById(id) ?: throw AspectDoesNotExist(id)
        val sameGroup = MeasureGroupMap[aspectVertex.measureName]?.measureList?.contains(measure) ?: false
        if (!sameGroup) {
            // todo: существует хотябы одно значения данного аспекта ---> throw AspectModificationException
            aspectVertex["baseType"] = measure.baseType.name
        }
        aspectVertex["measure"] = measure.name
        return@transaction aspectVertex.save<OVertex>()
    }.let { findById(id) }

    fun addProperty(id: String, property: AspectPropertyData): Aspect = transaction(db) {
        val aspectVertex = db.getVertexById(id) ?: throw AspectDoesNotExist(id)
        return@transaction property.saveAspectProperty().let { aspectVertex.addEdge(it, ASPECT_ASPECTPROPERTY_EDGE) }
    }.let { findById(id) }

    fun changePropertyName(propertyId: String, newPropertyName: String): AspectProperty = transaction(db) {
        val propertyVertex = db.getVertexById(propertyId) ?: throw AspectPropertyDoesNotExist(propertyId)
        propertyVertex["name"] = newPropertyName
        return@transaction propertyVertex.save<OVertex>()
    }.toAspectProperty()

    fun changePropertyPower(propertyId: String, newPropertyPower: AspectPropertyPower): AspectProperty = transaction(db) {
        val propertyVertex = db.getVertexById(propertyId) ?: throw AspectPropertyDoesNotExist(propertyId)
        propertyVertex["power"] = newPropertyPower.name
        return@transaction propertyVertex.save<OVertex>()
    }.toAspectProperty()

    fun changePropertyAspect(propertyId: String, newAspectId: String): AspectProperty = transaction(db) {
        val propertyVertex = db.getVertexById(propertyId) ?: throw AspectPropertyDoesNotExist(propertyId)
        db.getVertexById(newAspectId) ?: throw AspectDoesNotExist(newAspectId)
        // todo: if существует хотябы одно значения данного аспекта throw [AspectPropertyModificationException]
        propertyVertex["aspectId"] = newAspectId
        return@transaction propertyVertex.save<OVertex>()
    }.toAspectProperty()

    fun loadAspectProperty(propertyId: String): AspectProperty =
            db.getVertexById(propertyId)?.toAspectProperty()
                    ?: throw AspectPropertyDoesNotExist(propertyId)

    private fun save(name: String, measure: Measure<*>?, baseType: BaseType?, properties: List<AspectPropertyData>): Aspect {
        logger.trace("Adding aspect $name, $measure, $baseType, ${properties.size}")

        val save: OVertex = transaction(db) { session ->
            val measureVertex: OVertex? = measure?.name?.let { measureService.findMeasure(it) }
            val aspectVertex: OVertex = session.newVertex(ASPECT_CLASS)

            aspectVertex["name"] = name
            aspectVertex["baseType"] = baseType?.name ?: measure?.baseType?.name
            aspectVertex["measure"] = measure?.name
            measureVertex?.let { aspectVertex.addEdge(it, ASPECT_MEASURE_CLASS).save<OEdge>() }

            properties
                    .map { it.saveAspectProperty() }
                    .forEach { aspectVertex.addEdge(it, ASPECT_ASPECTPROPERTY_EDGE).save<OEdge>() }

            return@transaction aspectVertex.save()
        }
        logger.trace("Aspect $name saved with id: ${save.id}")
        return findById(save.id)
    }

    private fun AspectPropertyData.saveAspectProperty(): OVertex = transaction(db) { session ->
        logger.trace("Adding aspect property $name for aspect $aspectId")
        val aspectVertex: OVertex = db.getVertexById(aspectId) ?: throw AspectDoesNotExist(aspectId)
        val power = AspectPropertyPower.valueOf(this.power)
        val aspectPropertyVertex: OVertex = session.newVertex(ASPECT_PROPERTY_CLASS)

        aspectPropertyVertex["name"] = name
        aspectPropertyVertex["aspectId"] = aspectId
        aspectPropertyVertex["power"] = power.name

        aspectPropertyVertex.addEdge(aspectVertex, ASPECT_ASPECTPROPERTY_EDGE).save<OEdge>()

        return@transaction aspectPropertyVertex.save<OVertex>().also {
            logger.trace("Saved aspect property $name with temporary id: ${it.id}")
        }
    }

    private val OVertex.baseType: BaseType?
        get() = BaseType.restoreBaseType(this["baseType"])
    private val OVertex.name: String
        get() = this["name"]
    private val OVertex.aspect: String
        get() = this["aspectId"]
    private val OVertex.measureName: String?
        get() = this["measure"]
    private val OVertex.measure: Measure<*>?
        get() = GlobalMeasureMap[this["measure"]]

    private fun OVertex.toAspect(): Aspect =
            Aspect(id, name, measure, baseType?.let { OpenDomain(it) }, baseType, loadProperties(this))


    private fun loadProperties(oVertex: OVertex): List<AspectProperty> = session(db) {
        oVertex
                .getEdges(ODirection.OUT, ASPECT_ASPECTPROPERTY_EDGE)
                .map { it.to }
                .map { loadAspectProperty(it.id) }
    }

    private fun OVertex.toAspectProperty(): AspectProperty =
            AspectProperty(id, name, findById(aspect), AspectPropertyPower.valueOf(this["power"]))
}

private const val selectFromAspect = "SELECT FROM Aspect"
private const val selectAspectByName = "SELECT FROM Aspect where name = ? "

private val logger = loggerFor<AspectService>()

sealed class AspectException(message: String? = null) : Exception(message)
class AspectAlreadyExist(val id: String) : AspectException("id = $id")
class AspectDoesNotExist(val id: String) : AspectException("id = $id")
class AspectPropertyDoesNotExist(val id: String) : AspectException("id = $id")
class AspectModificationException(val id: String, message: String?) : AspectException("id = $id, message = $message")
class AspectPropertyModificationException(val id: String, message: String?) : AspectException("id = $id, message = $message")