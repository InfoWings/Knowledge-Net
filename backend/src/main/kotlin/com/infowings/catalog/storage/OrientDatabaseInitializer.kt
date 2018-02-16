package com.infowings.catalog.storage

import com.infowings.catalog.data.*
import com.infowings.catalog.loggerFor
import com.orientechnologies.orient.core.db.ODatabaseSession
import com.orientechnologies.orient.core.metadata.schema.OClass
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.OElement
import com.orientechnologies.orient.core.record.ORecord

const val USER_CLASS = "User"
const val ASPECT_CLASS = "Aspect"
const val ASPECT_PROPERTY_CLASS = "AspectProperty"
const val ASPECT_ASPECTPROPERTY_EDGE = "AspectPropertyEdge"
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
            session.createVertexClass(ASPECT_PROPERTY_CLASS)
            session.createEdgeClass(MEASURE_ASPECT_CLASS)
            session.createEdgeClass(ASPECT_ASPECTPROPERTY_EDGE)
        }
        return this
    }

    /** Initializes measures */
    fun initMeasures(): OrientDatabaseInitializer {
        if (session.getClass(MEASURE_GROUP_VERTEX) == null) {
            val vertexClass = session.createVertexClass(MEASURE_GROUP_VERTEX)
            vertexClass.createProperty("name", OType.STRING).createIndex(OClass.INDEX_TYPE.UNIQUE)
        }
        if (session.getClass(MEASURE_VERTEX) == null) {
            val vertexClass = session.createVertexClass(MEASURE_VERTEX)
            vertexClass.createProperty("name", OType.STRING).createIndex(OClass.INDEX_TYPE.UNIQUE)
        }
        if (session.getClass(MEASURE_GROUP_EDGE) == null) {
            session.createEdgeClass(MEASURE_GROUP_EDGE)
        }
        if (session.getClass(MEASURE_BASE_EDGE) == null) {
            session.createEdgeClass(MEASURE_BASE_EDGE)
        }
        if (session.getClass(MEASURE_BASE_AND_GROUP_EDGE) == null) {
            session.createEdgeClass(MEASURE_BASE_AND_GROUP_EDGE)
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
//    private fun initMeasureVertex(measure: BaseMeasureUnit<*, *>, initializedVertex: MutableMap<String, OVertex>): OVertex {
//        if (initializedVertex[measure.toString()] == null) {
//            val measureVertex: OVertex = session.newVertex(MEASURE_VERTEX_CLASS)
//            initializedVertex[measure.toString()] = measureVertex
//            measureVertex.setProperty("name", measure.name())
//            measureVertex.save<ORecord>()
//            measure.linkedTypes.forEach {
//                val childVertex = initMeasureVertex(it, initializedVertex)
//                val addedBefore = measureVertex.getEdges(ODirection.OUT).any {
//                    it.to.getProperty<String>("name") == childVertex.getProperty<String>("name")
//                }
//                if (!addedBefore) {
//                    measureVertex.addEdge(childVertex, MEASURE_EDGE_CLASS).save<ORecord>()
//                }
//            }
//            return measureVertex
//        }
//        return initializedVertex[measure.toString()]!!
//    }
}