package com.infowings.catalog.data

import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.*
import com.infowings.common.catalog.data.AspectData
import com.infowings.common.catalog.data.AspectPropertyData
import com.orientechnologies.orient.core.db.document.ODatabaseDocument
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OVertex
import com.orientechnologies.orient.core.sql.executor.OResultSet


class AspectService(private val database: OrientDatabase, private val measureService: MeasureService) {

    private fun save(
        name: String,
        measure: Measure<*>?,
        baseType: BaseType?,
        properties: Set<AspectPropertyData>
    ): Aspect {
        logger.debug("Adding aspect $name, $measure, $baseType, ${properties.size}")
        val save: OVertex = transaction(database) { session ->
            val measureVertex: OVertex? = measure?.name?.let { measureService.findMeasure(it, session) }
            val aspectVertex: OVertex = session.newVertex(ASPECT_CLASS)

            aspectVertex["name"] = name
            aspectVertex["baseType"] = baseType?.name ?: measure?.baseType?.name
            aspectVertex["measure"] = measure?.name
            measureVertex?.let { aspectVertex.addEdge(it, MEASURE_ASPECT_CLASS) }

            val props: List<OVertex> = properties.map { vertex ->
                saveAspectProperty(AspectProperty("", vertex.name, findById(vertex.aspectId, session), AspectPropertyPower.valueOf(vertex.power)), session)
            }

            for (prop in props) {
                aspectVertex.addEdge(prop, ASPECT_ASPECTPROPERTY_EDGE)
            }

            return@transaction aspectVertex.save()
        }
        logger.trace("Aspect $name saved with id: ${save.id}")
        return findById(save.id)
    }

    internal fun saveAspectProperty(property: AspectProperty, session: ODatabaseDocument): OVertex {
        logger.trace("Adding aspect property ${property.name} for aspect ${property.aspect.id}")
        val aspectPropertyVertex: OVertex = session.newVertex(ASPECT_PROPERTY_CLASS)

        aspectPropertyVertex["name"] = property.name
        aspectPropertyVertex["aspectId"] = property.aspect.id
        aspectPropertyVertex["power"] = property.power.name

        val aspectVertex: OVertex = session.getVertexById(property.aspect.id)
                ?: throw IllegalStateException("No aspect with id: ${property.aspect.id}")

        aspectPropertyVertex.addEdge(aspectVertex, ASPECT_ASPECTPROPERTY_EDGE)

        return aspectPropertyVertex.save<OVertex>().also {
            logger.trace("Saved aspect property ${property.name} with temporary id: ${it.id}")
        }
    }

    fun loadAspectProperty(id: String, session: ODatabaseDocument): AspectProperty =
        session.getVertexById(id)?.toAspectProperty(session)
                ?: throw IllegalArgumentException("No aspect property with id: $id")

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

    fun findByName(name: String): Aspect? {
        val statement = "SELECT FROM Aspect where name = ? ";

        return transaction(database) { session ->
            val rs: OResultSet = session.query(statement, name);
            if (rs.hasNext()) {
                return@transaction rs.next().toVertex().toAspect(session)
            }
            return@transaction null
        }
    }

    private fun findById(id: String, session: ODatabaseDocument): Aspect = (session.getVertexById(id))?.toAspect(session)
            ?: throw IllegalStateException("Incorrect data: cannot load aspect $id")

    fun findById(id: String): Aspect = transaction(database) { session ->
        return@transaction session.getVertexById(id)?.toAspect(session)
                ?: throw IllegalStateException("Incorrect data: cannot load aspect $id")
    }

    private fun ODatabaseDocument.getVertexById(id: String): OVertex? {
        val statement = "SELECT FROM ?"
        val rs: OResultSet = query(statement, ORecordId(id));
        return if (rs.hasNext()) {
            rs.next().toVertexOrNUll()
        } else {
            null
        }
    }

    fun getAspects(): List<Aspect> = TODO()

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

    private fun loadProperties(oVertex: OVertex, session: ODatabaseDocument): Set<AspectProperty> {
        val vertexes = oVertex.getEdges(ODirection.OUT, ASPECT_ASPECTPROPERTY_EDGE).map { it.to }

        return vertexes.map { loadAspectProperty(it.id, session) }.toSet()

    }

    private fun OVertex.toAspectProperty(session: ODatabaseDocument): AspectProperty =
        AspectProperty(id, name, findById(aspect, session), AspectPropertyPower.valueOf(this["power"]))
}

private val logger = loggerFor<AspectService>()

object AspectAlreadyExist : Throwable()

class AspectDoesNotExistId(id: Long) : Throwable()
class AspectDoesNotExistName(name: String) : Throwable()
