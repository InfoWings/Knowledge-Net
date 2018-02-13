package com.infowings.catalog.data

import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.transaction
import com.orientechnologies.orient.core.record.OElement
import com.orientechnologies.orient.core.sql.executor.OResult
import com.orientechnologies.orient.core.sql.executor.OResultSet


class AspectService(private val database: OrientDatabase) {

    private fun save(name: String, measureUnitString: String?, baseTypeString: String?): Aspect {
        val baseType = BaseType.restoreBaseType(baseTypeString)
        val measureUnit = restoreMeasureUnit(measureUnitString)

        val save: OElement = transaction(database) { db ->
            val statement = "SELECT FROM Aspect where name = ? ";
            val rs: OResultSet = db.query(statement, name);
            while (rs.hasNext()) {
                val row: OResult = rs.next()
                println(row)
            }
            rs.close()

            val doc: OElement = db.newInstance("Aspect")
            doc.setProperty("name", name)
            baseTypeString?.let { doc.setProperty("dataType", baseTypeString) }
            measureUnit?.let { doc.setProperty("measureUnit", measureUnit.toString()) }

            return@transaction doc.save()
        }
        return Aspect(
            id = save.identity.toString(),
            name = name,
            measureUnit = measureUnit,
            baseType = baseType,
            domain = OpenDomain(baseType)
        )

    }

    fun createAspect(name: String, measureUnit: String?, baseType: String?): Aspect {
        if (findByName(name) != null)
            throw DataTypeAlreadyExist

        return save(name, measureUnit, baseType)
    }

    fun findByName(name: String): Aspect? = TODO()
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


object DataTypeAlreadyExist : Throwable()

class AspectDoesNotExistId(id: Long) : Throwable()
class AspectDoesNotExistName(name: String) : Throwable()
