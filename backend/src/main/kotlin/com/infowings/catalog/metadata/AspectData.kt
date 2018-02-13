package com.infowings.catalog.metadata

import com.infowings.catalog.data.Aspect
import com.infowings.catalog.data.BaseType
import com.infowings.catalog.data.OpenDomain
import com.infowings.catalog.data.restoreMeasureUnit
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.transaction
import com.orientechnologies.orient.core.record.OElement
import com.orientechnologies.orient.core.sql.executor.OResult
import com.orientechnologies.orient.core.sql.executor.OResultSet


class AspectService(private val database: OrientDatabase) {

    fun findByName(name: String): Aspect? = null
    fun findOne(id: Long): Aspect? = null
    fun findAll(): List<Aspect> = emptyList()

    fun save(name: String, measureUnit: String?, baseTypeString: String?): Aspect {
        val baseType = BaseType.restoreBaseType(baseTypeString)
        return save(Aspect("", name, restoreMeasureUnit(measureUnit), OpenDomain(baseType), baseType))
    }

    fun save(aspect: Aspect): Aspect {

        val save: OElement = transaction(database) { db ->

            val statement = "SELECT FROM Aspect";
            val rs: OResultSet = db.query(statement);
            while (rs.hasNext()) {
                val row: OResult = rs.next()
                println(row)
            }
            rs.close();

            val doc: OElement = db.newInstance("Aspect")
            doc.setProperty("name", aspect.name)
            doc.setProperty("dataType", aspect.baseType.name)
            doc.setProperty("measureUnit", aspect.measureUnit.toString())


            return@transaction doc.save()
        }
        return aspect.copy(id = save.identity.toString())

    }
}