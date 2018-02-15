package com.infowings.catalog.data

import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.OVertex
import com.orientechnologies.orient.core.sql.executor.OResultSet


class AspectService(private val database: OrientDatabase) {

    private fun save(name: String, measureUnit: BaseMeasureUnit<*, *>?, baseType: BaseType?): Aspect {
        val save: OVertex = transaction(database) { db ->
            val statement = "SELECT FROM Measure where name = ? ";
            val rs: OResultSet = db.query(statement, measureUnit.toString());
            val row: OVertex = rs.next().toVertex

            val doc: OVertex = db.newVertex(ASPECT_CLASS)
            doc["name"] = name
            doc["baseType"] = baseType?.name ?: measureUnit?.baseType?.name
            doc.addEdge(row)
            measureUnit?.let { doc.setProperty("measureUnit", measureUnit.toString()) }

            return@transaction doc.save()
        }
        return Aspect(
            id = save.id,
            name = name,
            measureUnit = measureUnit,
            baseType = baseType,
            domain = baseType?.let { OpenDomain(baseType) }
        )

    }

    fun createAspect(name: String, measureUnitString: String?, baseTypeString: String?): Aspect {
        if (findByName(name) != null)
            throw AspectAlreadyExist

        val measureUnit = restoreMeasureUnit(measureUnitString)
        val baseType: BaseType? = when {
            baseTypeString != null -> BaseType.restoreBaseType(baseTypeString)
            measureUnit != null -> measureUnit.baseType
            else -> null
        }

        if (baseType != null && measureUnit != null && baseType != measureUnit.baseType)
            throw IllegalArgumentException("Base type and measure base type should be the same")

        return save(name, measureUnit, baseType)
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
                    measureUnit = vertex.measureUnit,
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
    private val OVertex.measureUnit: BaseMeasureUnit<*, *>?
        get() = restoreMeasureUnit(this["measureUnit"])
    private val OVertex.id: String
        get() = identity.toString()
}


object AspectAlreadyExist : Throwable()

class AspectDoesNotExistId(id: Long) : Throwable()
class AspectDoesNotExistName(name: String) : Throwable()
