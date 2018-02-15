package com.infowings.catalog.data

import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OVertex
import com.orientechnologies.orient.core.sql.executor.OResultSet


class AspectService(private val database: OrientDatabase, private val measureService: MeasureService) {

    private fun save(name: String, measureUnit: Measure<*>?, baseType: BaseType?): Aspect {
        val save: OVertex = transaction(database) { db ->
            val statement = "SELECT FROM $MEASURE_VERTEX where name = ? ";
            val rs: OResultSet = db.query(statement, measureUnit.toString());
            val measureVertex: OVertex? = measureUnit?.name?.let { measureService.findMeasure(it, db) }

            val aspectVertex: OVertex = db.newVertex(ASPECT_CLASS)

            aspectVertex["name"] = name
            aspectVertex["baseType"] = baseType?.name ?: measureUnit?.baseType?.name
            measureVertex?.let { aspectVertex.addEdge(it, MEASURE_ASPECT_CLASS) }

            return@transaction aspectVertex.save()
        }
        return Aspect(
            id = save.id,
            name = name,
            measureUnit = measureUnit,
            baseType = baseType,
            domain = baseType?.let { OpenDomain(baseType) }
        )

    }

    fun createAspect(name: String, measureName: String?, baseTypeString: String?): Aspect {
        if (findByName(name) != null)
            throw AspectAlreadyExist

        val measure: Measure<*>? =  GlobalMeasureMap[measureName]

        val baseType: BaseType? = when {
            baseTypeString != null -> BaseType.restoreBaseType(baseTypeString)
            measure != null -> measure.baseType
            else -> null
        }

        if (baseType != null && measure!= null && baseType != measure.baseType)
            throw IllegalArgumentException("Base type and measure base type should be the same")

        return save(name, measure, baseType)
    }

    fun findByName(name: String): Aspect? {
        val statement = "SELECT FROM Aspect where name = ? ";

        return transaction(database) { db ->
            val rs: OResultSet = db.query(statement, name);
            if (rs.hasNext()) {
                val vertex: OVertex = rs.next().toVertex
                return@transaction Aspect(
                    id = vertex.id,
                    name = vertex.name,
                    measureUnit = GlobalMeasureMap[vertex.measureName],
                    baseType = vertex.baseType,
                    domain = vertex.baseType?.let { OpenDomain(it) }
                )
            }
            return@transaction null
        }
    }

    fun getAspects(): List<Aspect> = TODO()

    private val OVertex.baseType: BaseType?
        get() = BaseType.restoreBaseType(this["baseType"])
    private val OVertex.name: String
        get() = this["name"]
    private val OVertex.measureName: String?
        get() = this.getEdges(ODirection.OUT, MEASURE_ASPECT_CLASS).firstOrNull()?.to?.name
    private val OVertex.id: String
        get() = identity.toString()
}


object AspectAlreadyExist : Throwable()

class AspectDoesNotExistId(id: Long) : Throwable()
class AspectDoesNotExistName(name: String) : Throwable()
