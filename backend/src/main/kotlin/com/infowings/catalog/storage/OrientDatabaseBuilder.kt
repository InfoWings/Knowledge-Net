package com.infowings.catalog.storage

import com.infowings.catalog.data.BaseMeasureUnit
import com.infowings.catalog.data.LengthMeasure
import com.infowings.catalog.data.SpeedMeasure
import com.orientechnologies.orient.core.db.ODatabaseSession
import com.orientechnologies.orient.core.metadata.schema.OClass
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OElement
import com.orientechnologies.orient.core.record.ORecord
import com.orientechnologies.orient.core.record.OVertex

class OrientDatabaseBuilder(private val session: ODatabaseSession) {

    companion object {
        const val MEASURE_VERTEX_CLASS = "Measure"
        const val MEASURE_EDGE_CLASS = "LinkedBy"
    }

    fun initUsers(): OrientDatabaseBuilder {
        if (session.getClass("User") == null) {
            val userClass = session.createClass("User")
            userClass.createProperty("username", OType.STRING).createIndex(OClass.INDEX_TYPE.UNIQUE)
            userClass.createProperty("password", OType.STRING)
            userClass.createProperty("role", OType.STRING)

            val user: OElement = session.newInstance("User")
            user.setProperty("username", "user")
            user.setProperty("password", "user")
            user.setProperty("role", "USER")
            user.save<ORecord>()

            val admin: OElement = session.newInstance("User")
            admin.setProperty("username", "admin")
            admin.setProperty("password", "admin")
            admin.setProperty("role", "ADMIN")
            admin.save<ORecord>()

            val poweredUser: OElement = session.newInstance("USER")
            poweredUser.setProperty("username", "powereduser")
            poweredUser.setProperty("password", "powereduser")
            poweredUser.setProperty("role", "POWERED_USER")
            poweredUser.save<ORecord>()
        }
        return this
    }

    fun initAspects(): OrientDatabaseBuilder {
        session.createClassIfNotExist("Aspect")
        return this
    }

    fun initMeasures(): OrientDatabaseBuilder {
        if (session.getClass(MEASURE_VERTEX_CLASS) == null) {
            val measureClass = session.createVertexClass(MEASURE_VERTEX_CLASS)
            measureClass.createProperty("name", OType.STRING).createIndex(OClass.INDEX_TYPE.UNIQUE)
            session.createEdgeClass(MEASURE_EDGE_CLASS)
            val initializedVertex = mutableMapOf<String, OVertex>()
            initMeasureVertex(LengthMeasure, initializedVertex)
            initMeasureVertex(SpeedMeasure, initializedVertex)
        }
        return this
    }

    private fun initMeasureVertex(measure: BaseMeasureUnit<*, *>, initializedVertex: MutableMap<String, OVertex>): OVertex {
        if (initializedVertex[measure.toString()] == null) {
            val measureVertex: OVertex = session.newVertex(MEASURE_VERTEX_CLASS)
            initializedVertex[measure.toString()] = measureVertex
            measureVertex.setProperty("name", measure.toString())
            measureVertex.save<ORecord>()
            measure.linkedTypes.forEach {
                val childVertex = initMeasureVertex(it, initializedVertex)
                val addedBefore = measureVertex.getEdges(ODirection.OUT).any {
                    it.to.getProperty<String>("name") == childVertex.getProperty<String>("name")
                }
                if (!addedBefore) {
                    measureVertex.addEdge(childVertex, MEASURE_EDGE_CLASS)
                }
            }
            return measureVertex
        }
        return initializedVertex[measure.toString()]!!
    }
}