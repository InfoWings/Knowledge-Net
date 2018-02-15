package com.infowings.catalog.storage

import com.infowings.catalog.data.BaseMeasureUnit
import com.infowings.catalog.data.LengthMeasure
import com.infowings.catalog.data.SpeedMeasure
import com.infowings.catalog.loggerFor
import com.orientechnologies.orient.core.db.ODatabaseSession
import com.orientechnologies.orient.core.metadata.schema.OClass
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OElement
import com.orientechnologies.orient.core.record.ORecord
import com.orientechnologies.orient.core.record.OVertex

const val MEASURE_VERTEX_CLASS = "Measure"
const val MEASURE_EDGE_CLASS = "LinkedBy"
const val USER_CLASS = "User"
const val ASPECT_CLASS = "Aspect"
const val MEASURE_ASPECT_CLASS = "AspectToMeasure"

private val logger = loggerFor<OrientDatabaseInitializer>()

/** Initialization default db values, every method executes only in case describing part of db is empty. */
class OrientDatabaseInitializer(private val session: ODatabaseSession) {

    /** Executes only if there is no Class $USER_CLASS in db */
    fun initUsers(): OrientDatabaseInitializer {
        if (session.getClass("User") == null) {
            logger.info("Init users")
            initUser("user", "user", "USER")
            initUser("admin", "admin", "ADMIN")
            initUser("powereduser", "powereduser", "POWERED_USER")
        }
        return this
    }

    /** Executes only if there is no Class Aspect in db */
    fun initAspects(): OrientDatabaseInitializer {
        logger.info("Init aspects")
        if (session.getClass(ASPECT_CLASS) == null) {
            session.createVertexClass(ASPECT_CLASS)
            session.createEdgeClass(MEASURE_ASPECT_CLASS)
        }
        return this
    }

    /** Initializes measures. Executes only if there is no Class $MEASURE_VERTEX_CLASS in db */
    fun initMeasures(): OrientDatabaseInitializer {
        if (session.getClass(MEASURE_VERTEX_CLASS) == null) {
            logger.info("Init measures")
            val measureClass = session.createVertexClass(MEASURE_VERTEX_CLASS)
            measureClass.createProperty("name", OType.STRING).createIndex(OClass.INDEX_TYPE.UNIQUE)
            session.createEdgeClass(MEASURE_EDGE_CLASS)
            val initializedVertex = mutableMapOf<String, OVertex>()
            initMeasureVertex(LengthMeasure, initializedVertex)
            initMeasureVertex(SpeedMeasure, initializedVertex)
        }
        return this
    }

    /** Create user in database */
    private fun initUser(username: String, password: String, role: String) {
        val user: OElement = session.newInstance(USER_CLASS)
        user.setProperty("username", username)
        user.setProperty("password", password)
        user.setProperty("role", role)
        user.save<ORecord>()
    }

    /** Init measure and all linked measures */
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
                    measureVertex.addEdge(childVertex, MEASURE_EDGE_CLASS).save<ORecord>()
                }
            }
            return measureVertex
        }
        return initializedVertex[measure.toString()]!!
    }
}