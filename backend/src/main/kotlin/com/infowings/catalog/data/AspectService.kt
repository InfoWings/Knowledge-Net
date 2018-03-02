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
        checkBusinessKey(aspectData.name)

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

    /**
     * Update [Aspect] by [AspectData]
     * [id] of [AspectData] is the id of editing [Aspect]. Any other info is using for update
     *
     * @throws AspectException
     * @throws IllegalArgumentException in case id is incorrect
     */
    fun updateAspect(updatingAspect: AspectData): Aspect = transaction(db) {
        var realAspect = updatingAspect.id?.let { findById(it) } ?: throw IllegalArgumentException("Incorrect id")

        if (realAspect.name != updatingAspect.name) {
            realAspect = changeName(realAspect.id, updatingAspect.name)
        }

        if (realAspect.baseType?.name != updatingAspect.baseType) {
            realAspect = changeBaseType(realAspect.id, BaseType.restoreBaseType(updatingAspect.baseType))
        }

        if (realAspect.measure?.name != updatingAspect.name) {
            realAspect = changeMeasure(realAspect.id, GlobalMeasureMap[updatingAspect.measure])
        }

        // todo: Check domens

        updatingAspect.properties.filter { it.id != "" }.forEach { property ->
            var realProperty = loadAspectProperty(property.id)

            if (realProperty.name != property.name) {
                realProperty = changePropertyName(property.id, property.name)
            }

            if (realProperty.power.name != property.power) {
                realProperty = changePropertyName(property.id, property.power)
            }

            if (realProperty.aspect.id != property.aspectId) {
                changePropertyAspect(property.id, property.aspectId)
            }
        }

        updatingAspect.properties.filter { it.id == "" }.forEach {
            addProperty(realAspect.id, it)
        }

        return@transaction realAspect

    }.let { findById(it.id) }

    /**
     * Change [Aspect] name by id
     * @throws AspectDoesNotExist
     * @throws AspectAlreadyExist
     */
    fun changeName(id: String, newName: String): Aspect = transaction(db) {
        val aspectVertex = db.getVertexById(id) ?: throw AspectDoesNotExist(id)
        checkBusinessKey(newName)
        aspectVertex["name"] = newName
        return@transaction aspectVertex.save<OVertex>()
    }.let { findById(id) }

    /**
     * Change [Aspect] base type
     * @throws AspectDoesNotExist
     * @throws AspectModificationException in case there exists measure or there exists value of aspect
     */
    fun changeBaseType(id: String, newType: BaseType?): Aspect = transaction(db) {
        val aspectVertex = db.getVertexById(id) ?: throw AspectDoesNotExist(id)
        aspectVertex.measure?.let { throw AspectModificationException(id, "Aspect has a measure") }
        // todo: существует хотябы одно значения данного аспекта ---> throw AspectModificationException
        aspectVertex["baseType"] = newType?.name
        return@transaction aspectVertex.save<OVertex>()
    }.let { findById(id) }

    /**
     * Change [Aspect] measure
     * @throws AspectDoesNotExist
     * @throws AspectModificationException in case there exists value of aspect
     */
    fun changeMeasure(id: String, measure: Measure<*>?): Aspect = transaction(db) {
        val aspectVertex = db.getVertexById(id) ?: throw AspectDoesNotExist(id)
        val sameGroup = MeasureGroupMap[aspectVertex.measureName]?.measureList?.contains(measure) ?: false
        if (measure == null || !sameGroup) {
            // todo: существует хотябы одно значения данного аспекта ---> throw AspectModificationException
            aspectVertex["baseType"] = measure?.baseType?.name
        }
        aspectVertex["measure"] = measure?.name

        return@transaction aspectVertex.save<OVertex>()
    }.let { findById(id) }

    /**
     * Creates new Aspect and saves it into DB
     * @throws AspectDoesNotExist,
     * @throws IllegalArgumentException in case of incorrect input data,
     * @throws AspectDoesNotExist if some AspectProperty has incorrect aspect id
     */
    fun addProperty(id: String, property: AspectPropertyData): Aspect = transaction(db) {
        val aspectVertex = db.getVertexById(id) ?: throw AspectDoesNotExist(id)
        if (loadProperties(aspectVertex).map { it.name }.contains(property.name)) {
            throw IllegalArgumentException("Properties for aspect should have different names")
        }
        return@transaction property.saveAspectProperty().let { aspectVertex.addEdge(it, ASPECT_ASPECTPROPERTY_EDGE).save<OEdge>() }
    }.let { findById(id) }

    /**
     * Change [AspectProperty] name
     * @throws AspectPropertyDoesNotExist
     * @throws AspectModificationException in for parent aspect there exists property with same name
     */
    fun changePropertyName(propertyId: String, newPropertyName: String): AspectProperty = transaction(db) {
        val propertyVertex = db.getVertexById(propertyId) ?: throw AspectPropertyDoesNotExist(propertyId)
        val aspectVertex = propertyVertex.getVertices(ODirection.IN).first()
        if (propertyVertex.name == newPropertyName) {
            return@transaction propertyVertex
        }
        val properties = loadProperties(aspectVertex)
        if (properties.map { it.name }.contains(newPropertyName)) {
            throw AspectPropertyModificationException(propertyId, "Properties for aspect should have different names")
        }
        propertyVertex["name"] = newPropertyName
        return@transaction propertyVertex.save<OVertex>()
    }.toAspectProperty()

    /**
     * Change [AspectProperty] power
     * @throws AspectPropertyDoesNotExist
     */
    fun changePropertyPower(propertyId: String, newPropertyPower: AspectPropertyPower): AspectProperty = transaction(db) {
        val propertyVertex = db.getVertexById(propertyId) ?: throw AspectPropertyDoesNotExist(propertyId)
        propertyVertex["power"] = newPropertyPower.name
        return@transaction propertyVertex.save<OVertex>()
    }.toAspectProperty()

    /**
     * Change [AspectProperty] name
     * @throws AspectPropertyDoesNotExist
     * @throws AspectModificationException in case there exists value of aspect
     */
    fun changePropertyAspect(propertyId: String, newAspectId: String): AspectProperty = transaction(db) {
        val propertyVertex = db.getVertexById(propertyId) ?: throw AspectPropertyDoesNotExist(propertyId)
        db.getVertexById(newAspectId) ?: throw AspectDoesNotExist(newAspectId)
        // todo: if существует хотябы одно значения данного аспекта throw [AspectPropertyModificationException]
        propertyVertex["aspectId"] = newAspectId
        return@transaction propertyVertex.save<OVertex>()
    }.toAspectProperty()

    /**
     * Load property by id
     * @throws AspectPropertyDoesNotExist
     */
    fun loadAspectProperty(propertyId: String): AspectProperty =
            db.getVertexById(propertyId)?.toAspectProperty()
                    ?: throw AspectPropertyDoesNotExist(propertyId)

    private fun save(name: String, measure: Measure<*>?, baseType: BaseType?, properties: List<AspectPropertyData>): Aspect {
        logger.trace("Adding aspect $name, $measure, $baseType, ${properties.size}")

        val save: OVertex = transaction(db) { session ->
            val measureVertex: OVertex? = measure?.name?.let {
                measureService.findMeasure(it) ?: throw IllegalArgumentException("Measure $it does not exist")
            }
            val aspectVertex: OVertex = session.newVertex(ASPECT_CLASS)

            aspectVertex["name"] = name
            aspectVertex["baseType"] = baseType?.name ?: measure?.baseType?.name
            aspectVertex["measure"] = measure?.name
            measureVertex?.let { aspectVertex.addEdge(it, ASPECT_MEASURE_CLASS).save<OEdge>() }

            if (properties.distinctBy { it.name }.size != properties.size) {
                throw IllegalArgumentException("Properties for aspect should have different names")
            }

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

    private fun checkBusinessKey(name: String) {
        findByName(name)?.let { throw AspectAlreadyExist(name) }
    }

    private fun OVertex.toAspectProperty(): AspectProperty =
            AspectProperty(id, name, findById(aspect), AspectPropertyPower.valueOf(this["power"]))
}

private const val selectFromAspect = "SELECT FROM Aspect"
private const val selectAspectByName = "SELECT FROM Aspect where name = ? "

private val logger = loggerFor<AspectService>()

sealed class AspectException(message: String? = null) : Exception(message)
class AspectAlreadyExist(val name: String) : AspectException("name = $name")
class AspectDoesNotExist(val id: String) : AspectException("id = $id")
class AspectPropertyDoesNotExist(val id: String) : AspectException("id = $id")
class AspectModificationException(val id: String, message: String?) : AspectException("id = $id, message = $message")
class AspectPropertyModificationException(val id: String, message: String?) : AspectException("id = $id, message = $message")