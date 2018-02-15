package com.infowings.catalog.data

import com.infowings.catalog.storage.MEASURE_ASPECT_CLASS
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.transaction
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OElement
import com.orientechnologies.orient.core.record.OVertex
import com.orientechnologies.orient.core.sql.executor.OResult
import com.orientechnologies.orient.core.sql.executor.OResultSet


class AspectService(private val database: OrientDatabase) {

    private fun save(name: String, measureUnitString: String?, baseTypeString: String?): Aspect {
        val measureUnit = restoreMeasureUnit(measureUnitString)
        val baseType: BaseType? = when {
            baseTypeString != null -> BaseType.restoreBaseType(baseTypeString)
            measureUnit != null -> measureUnit.baseType
            else -> null
        }

        if (baseType != null && measureUnit != null && baseType != measureUnit.baseType)
            throw IllegalArgumentException("Base type and measure base type should be the same")

        val save: OElement = transaction(database) { db ->
            val statement = "SELECT FROM Measure where name = ? ";
            val rs: OResultSet = db.query(statement, measureUnit.toString());
            val row: OVertex = rs.next().vertex.orElse(null) ?: throw Exception()





            val doc: OVertex = db.newVertex("Aspect")
            doc.setProperty("name", name)
            doc.setProperty("baseType", baseType?.name ?: measureUnit?.baseType?.name)
            doc.addEdge(row)
            measureUnit?.let { doc.setProperty("measureUnit", measureUnit.toString()) }

            return@transaction doc.save()
        }
        return Aspect(
            id = save.identity.toString(),
            name = name,
            measureUnit = measureUnit,
            baseType = baseType,
            domain = baseType?.let { OpenDomain(baseType) }
        )

    }

    fun createAspect(name: String, measureUnit: String?, baseType: String?): Aspect {
        if (findByName(name) != null)
            throw AspectAlreadyExist

        return save(name, measureUnit, baseType)
    }

    fun findByName(name: String): Aspect? {
        val statement = "SELECT FROM Aspect where name = ? ";

        return transaction(database) { db ->
            val rs: OResultSet = db.query(statement, name);
            if (rs.hasNext()) {
                val row: OVertex = rs.next().vertex.orElse(null) ?: throw Exception()
                val baseType = BaseType.restoreBaseType(row.getProperty("baseType"))
                val measureUnit = restoreMeasureUnit(row.getProperty("measureUnit"))
                val edges = row.getEdges(ODirection.OUT)
                return@transaction Aspect(
                    id = row.identity.toString(),
                    name = row.getProperty("name"),
                    measureUnit = measureUnit,
                    baseType = baseType,
                    domain = OpenDomain(baseType)
                )
            }
            return@transaction null
        }
    }

    fun findById(id: Long): Aspect? = TODO()

//    fun createPropertyForAspect(aspectId: Long, name: String, propertyAspect: String, propertyPower: String): Aspect {
//        val parentAspect = findById(aspectId) ?: throw AspectDoesNotExistId(aspectId)
//
//        val innerAspectEntity = findByName(propertyAspect) ?: throw AspectDoesNotExistName(propertyAspect)
//
//        // parentAspect.properties += propertyService.save(name, innerAspectEntity, propertyPower)
//
//        return save(parentAspect)
//    }

    fun getAspects(): List<Aspect> = TODO()
}


object AspectAlreadyExist : Throwable()

class AspectDoesNotExistId(id: Long) : Throwable()
class AspectDoesNotExistName(name: String) : Throwable()
