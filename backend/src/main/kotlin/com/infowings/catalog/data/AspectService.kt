package com.infowings.catalog.data

import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.db.document.ODatabaseDocument
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.record.OVertex
import com.orientechnologies.orient.core.sql.executor.OResultSet


class AspectService(private val database: OrientDatabase, private val measureService: MeasureService) {

    private fun save(name: String, measureUnit: Measure<*>?, baseType: BaseType?): Aspect {
        val save: OVertex = transaction(database) { session ->

            val measureVertex: OVertex? = measureUnit?.name?.let { measureService.findMeasure(it, session) }
            val aspectVertex: OVertex = session.newVertex(ASPECT_CLASS)

            aspectVertex["name"] = name
            aspectVertex["baseType"] = baseType?.name ?: measureUnit?.baseType?.name
            aspectVertex["measure"] = measureUnit?.name
            measureVertex?.let { aspectVertex.addEdge(it, MEASURE_ASPECT_CLASS) }

            return@transaction aspectVertex.save()
        }
        return save.toAspect()
    }

    fun saveAspectProperty(property: AspectProperty, session: ODatabaseDocument): OVertex {
        val aspectPropertyVertex: OVertex = session.newVertex(ASPECT_PROPERTY_CLASS)

        aspectPropertyVertex["name"] = property.name
        aspectPropertyVertex["aspectId"] = property.aspect.id
        aspectPropertyVertex["power"] = property.power.name

        val aspectVertex: OVertex = session.getVertexById(property.aspect.id) ?: throw IllegalStateException("No aspect with id: ${property.aspect.id}")

        val saved = aspectPropertyVertex.save<OVertex>()
        aspectVertex.addEdge(saved, ASPECT_ASPECTPROPERTY_EDGE)
        aspectVertex.save<OVertex>()

        return saved
    }

    fun loadAspectProperty(id: String, session: ODatabaseDocument): AspectProperty =
        session.getVertexById(id)?.toAspectProperty(session) ?: throw IllegalArgumentException("No aspect property with id: $id")

    fun createAspect(name: String, measureName: String?, baseTypeString: String?): Aspect {
        if (findByName(name) != null)
            throw AspectAlreadyExist

        val measure: Measure<*>? = GlobalMeasureMap[measureName]

        val baseType: BaseType? = when {
            baseTypeString != null -> BaseType.restoreBaseType(baseTypeString)
            measure != null -> measure.baseType
            else -> BaseType.Nothing
        }

        if (baseType != null && measure != null && baseType != measure.baseType)
            throw IllegalArgumentException("Base type and measure base type should be the same")

        return save(name, measure, baseType)
    }

    fun findByName(name: String): Aspect? {
        val statement = "SELECT FROM Aspect where name = ? ";

        return transaction(database) { db ->
            val rs: OResultSet = db.query(statement, name);
            if (rs.hasNext()) {
                return@transaction rs.next().toVertex().toAspect()
            }
            return@transaction null
        }
    }

    fun findById(id: String, session: ODatabaseDocument): Aspect = (session.getVertexById(id))?.toAspect()
            ?: throw IllegalStateException("Incorrect data: cannot load aspect $id")

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

    private fun OVertex.toAspect(): Aspect = Aspect(id, name, measureName, baseType?.let { OpenDomain(it) }, baseType)

    private fun OVertex.toAspectProperty(session: ODatabaseDocument): AspectProperty =
        AspectProperty(id, name, findById(aspect, session), AspectPropertyPower.valueOf(this["power"]))
}


object AspectAlreadyExist : Throwable()

class AspectDoesNotExistId(id: Long) : Throwable()
class AspectDoesNotExistName(name: String) : Throwable()
