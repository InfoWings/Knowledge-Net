package com.infowings.catalog.data

import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.transaction
import com.orientechnologies.orient.core.db.document.ODatabaseDocument
import com.orientechnologies.orient.core.record.ORecord
import com.orientechnologies.orient.core.record.OVertex
import com.orientechnologies.orient.core.sql.executor.OResultSet

const val MEASURE_GROUP_VERTEX = "MeasureGroupVertex"
const val MEASURE_VERTEX = "MeasureVertex"
const val MEASURE_GROUP_EDGE = "MeasureGroupEdge"
const val MEASURE_BASE_EDGE = "MeasureEdge"
const val MEASURE_BASE_AND_GROUP_EDGE = "MeasureGroupEdge"

class MeasureService(private val database: OrientDatabase) {
    init {
        /** Add initial measures to database */
        // val currentSession = database.acquire()
        transaction(database) { db ->
            val lengthGroupVertex = saveGroup(LengthGroup, db)
            val speedGroupVertex = saveGroup(SpeedGroup, db)
            if (lengthGroupVertex != null && speedGroupVertex != null) {
                lengthGroupVertex.addEdge(speedGroupVertex, MEASURE_GROUP_EDGE).save<ORecord>()
                speedGroupVertex.addEdge(lengthGroupVertex, MEASURE_GROUP_EDGE).save<ORecord>()
            }
        }
    }
    

    fun findMeasureGroup(groupName: String, session: ODatabaseDocument): OVertex? {
        val query = "SELECT * from $MEASURE_GROUP_VERTEX where name = ?"
        val records = session.query(query, groupName)
        return records.getVertex()
    }

    fun findMeasure(measureName: String, session: ODatabaseDocument): OVertex? {
        val query = "SELECT * from $MEASURE_VERTEX where name = ?"
        val records = session.query(query, measureName)
        return records.getVertex()
    }

    private fun saveGroup(group: MeasureGroup<*>, session: ODatabaseDocument): OVertex? {
        if (findMeasureGroup(group.name, session) != null) {
            loggerFor<MeasureService>().info("Group with name ${group.name} already exist in db")
            return null
        }
        val groupVertex = session.newVertex(MEASURE_GROUP_VERTEX)
        groupVertex.setProperty("name", group.name)
        val baseVertex = createMeasure(group.base.name, session)
        groupVertex.addEdge(baseVertex, MEASURE_BASE_AND_GROUP_EDGE)
        baseVertex.addEdge(groupVertex, MEASURE_BASE_AND_GROUP_EDGE)
        group.measureList.forEach {
            createMeasure(it.name, session).addEdge(baseVertex, MEASURE_BASE_EDGE).save<ORecord>()
        }
        baseVertex.save<ORecord>()
        return groupVertex.save()
    }

    private fun createMeasure(measureName: String, session: ODatabaseDocument): OVertex {
        findMeasure(measureName, session).let { if (it != null) return it }
        val groupVertex = session.newVertex(MEASURE_VERTEX)
        groupVertex.setProperty("name", measureName)
        return groupVertex
    }

    private fun OResultSet.getVertex() = if (this.hasNext()) this.next().vertex.orElse(null) else null
}