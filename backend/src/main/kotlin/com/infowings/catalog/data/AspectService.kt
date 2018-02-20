package com.infowings.catalog.data

import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.*
import com.infowings.common.catalog.data.AspectData
import com.infowings.common.catalog.data.AspectPropertyData
import com.orientechnologies.orient.core.db.document.ODatabaseDocument
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OEdge
import com.orientechnologies.orient.core.record.OVertex

/**
 * Data layer for Aspect & Aspect properties
 * Both stored as vertexes [ASPECT_CLASS] & [ASPECT_PROPERTY_CLASS] linked by [ASPECT_ASPECTPROPERTY_EDGE]
 * [ASPECT_CLASS] can be linked with [Measure] by [ASPECT_MEASURE_CLASS]
 */
class AspectService(private val database: OrientDatabase, private val measureService: MeasureService) {

    private fun save(name: String, measure: Measure<*>?, baseType: BaseType?, properties: List<AspectPropertyData>): Aspect {
        logger.trace("Adding aspect $name, $measure, $baseType, ${properties.size}")

        val save: OVertex = transaction(database) { session ->
            val measureVertex: OVertex? = measure?.name?.let { measureService.findMeasure(it, session) }
            val aspectVertex: OVertex = session.newVertex(ASPECT_CLASS)

            aspectVertex["name"] = name
            aspectVertex["baseType"] = baseType?.name ?: measure?.baseType?.name
            aspectVertex["measure"] = measure?.name
            measureVertex?.let { aspectVertex.addEdge(it, ASPECT_MEASURE_CLASS).save<OEdge>() }

            for (property in properties) {
                val aspect = findById(property.aspectId, session)
                val power = AspectPropertyPower.valueOf(property.power)
                val aspectPropertyVertex = AspectProperty("", property.name, aspect, power).saveAspectProperty(session)
                aspectVertex.addEdge(aspectPropertyVertex, ASPECT_ASPECTPROPERTY_EDGE).save<OEdge>()
            }

            return@transaction aspectVertex.save()
        }
        logger.trace("Aspect $name saved with id: ${save.id}")
        return findById(save.id)
    }

    private fun AspectProperty.saveAspectProperty(session: ODatabaseDocument): OVertex {
        logger.trace("Adding aspect property $name for aspect ${aspect.id}")
        val aspectPropertyVertex: OVertex = session.newVertex(ASPECT_PROPERTY_CLASS)

        aspectPropertyVertex["name"] = name
        aspectPropertyVertex["aspectId"] = aspect.id
        aspectPropertyVertex["power"] = power.name

        val aspectVertex: OVertex = session.getVertexById(aspect.id) ?: throw AspectDoesNotExist(aspect.id)

        aspectPropertyVertex.addEdge(aspectVertex, ASPECT_ASPECTPROPERTY_EDGE).save<OEdge>()

        return aspectPropertyVertex.save<OVertex>().also {
            logger.trace("Saved aspect property $name with temporary id: ${it.id}")
        }
    }

    private fun loadAspectProperty(id: String, session: ODatabaseDocument): AspectProperty =
        session.getVertexById(id)?.toAspectProperty(session)
                ?: throw IllegalArgumentException("No aspect property with id: $id")

    /**
     * Creates new Aspect and saves it into DB
     * @throws AspectAlreadyExist,
     * @throws IllegalArgumentException in case of incorrect input data,
     * @throws AspectDoesNotExist if some AspectProperty has incorrect aspect id
     */
    fun createAspect(aspectData: AspectData): Aspect {
        if (findByName(aspectData.name) != null)
            throw AspectAlreadyExist

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
    fun findByName(name: String): Aspect? = database.query(selectAspectByName, name) { rs, session ->
        rs.map { it.toVertex().toAspect(session) }.firstOrNull()
    }
//    fun findByName(name: String): Aspect? = transaction(database){session ->
//        session.query(selectAspectByName, name).use { rs ->
//            rs.asSequence()
//                .map { it.toVertex().toAspect(session)  }
//                .firstOrNull()
//        }
//    }

    fun getAspects(): List<Aspect> = database.query(selectFromAspect) { rs, session ->
        rs.mapNotNull { it.toVertexOrNUll()?.toAspect(session) }.toList()
    }

    private fun findById(id: String, session: ODatabaseDocument): Aspect =
        session.getVertexById(id)?.toAspect(session) ?: throw AspectDoesNotExist(id)

    /**
     * Search [Aspect] by it's id
     * @throws AspectDoesNotExist
     */
    fun findById(id: String): Aspect = session(database) { findById(id, session = it) }

    private fun ODatabaseDocument.getVertexById(id: String): OVertex? =
        query(selectById, ORecordId(id)).use { rs ->
            rs.asSequence()
                .map { it.toVertexOrNUll() }
                .firstOrNull()
        }

    private val OVertex.baseType: BaseType?
        get() = BaseType.restoreBaseType(this["baseType"])
    private val OVertex.name: String
        get() = this["name"]
    private val OVertex.aspect: String
        get() = this["aspectId"]
    private val OVertex.measureName: Measure<*>?
        get() = GlobalMeasureMap[this["measure"]]
    private val OVertex.id: String
        get() = identity.toString()

    private fun OVertex.toAspect(session: ODatabaseDocument): Aspect =
        Aspect(id, name, measureName, baseType?.let { OpenDomain(it) }, baseType, loadProperties(this, session))

    private fun loadProperties(oVertex: OVertex, session: ODatabaseDocument): List<AspectProperty> =
        oVertex
            .getEdges(ODirection.OUT, ASPECT_ASPECTPROPERTY_EDGE)
            .map { it.to }
            .map { loadAspectProperty(it.id, session) }

    private fun OVertex.toAspectProperty(session: ODatabaseDocument): AspectProperty =
        AspectProperty(id, name, findById(aspect, session), AspectPropertyPower.valueOf(this["power"]))
}

private const val selectFromAspect = "SELECT FROM Aspect"
private const val selectById = "SELECT FROM ?"
private const val selectAspectByName = "SELECT FROM Aspect where name = ? "

private val logger = loggerFor<AspectService>()

object AspectAlreadyExist : Throwable()
class AspectDoesNotExist(val id: String) : Throwable()
