package com.infowings.catalog.storage

import com.infowings.common.catalog.data.*
import com.infowings.common.catalog.loggerFor
import com.orientechnologies.orient.core.db.ODatabaseSession
import com.orientechnologies.orient.core.metadata.schema.OClass
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.OElement
import com.orientechnologies.orient.core.record.ORecord
import com.orientechnologies.orient.core.record.impl.ODocument

const val USER_CLASS = "User"
const val ASPECT_CLASS = "Aspect"
const val ASPECT_PROPERTY_CLASS = "AspectProperty"
const val ASPECT_ASPECTPROPERTY_EDGE = "AspectPropertyEdge"
const val ASPECT_MEASURE_CLASS = "AspectToMeasure"

private val logger = loggerFor<OrientDatabaseInitializer>()

/** Initialization default db values, every method executes only in case describing part of db is empty. */
class OrientDatabaseInitializer(private val database: OrientDatabase) {

    /** Executes only if there is no Class $USER_CLASS in db */
    fun initUsers(): OrientDatabaseInitializer = session(database) { session ->
        if (session.getClass("User") == null) {
            logger.info("Init users")
            initUser("user", "user", "USER")
            initUser("admin", "admin", "ADMIN")
            initUser("powereduser", "powereduser", "POWERED_USER")
        }
        return this
    }

    /** Executes only if there is no Class Aspect in db */
    fun initAspects(): OrientDatabaseInitializer = session(database) { session ->
        logger.info("Init aspects")
        if (session.getClass(ASPECT_CLASS) == null) {
            session.createVertexClass(ASPECT_CLASS)
            session.createVertexClass(ASPECT_PROPERTY_CLASS)
            session.createEdgeClass(ASPECT_MEASURE_CLASS)
            session.createEdgeClass(ASPECT_ASPECTPROPERTY_EDGE)
        }
        return this
    }

    /** Initializes measures */
    fun initMeasures(): OrientDatabaseInitializer = session(database) { session ->
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
        /** Add initial measures to database */
        val localMeasureService = MeasureService()
        session(database) {
            MeasureGroupMap.values.forEach { localMeasureService.saveGroup(it, database) }
            localMeasureService.linkGroupsBidirectional(AreaGroup, LengthGroup, database)
            localMeasureService.linkGroupsBidirectional(VolumeGroup, LengthGroup, database)
            localMeasureService.linkGroupsBidirectional(SpeedGroup, LengthGroup, database)
            localMeasureService.linkGroupsBidirectional(TorqueGroup, LengthGroup, database)
            localMeasureService.linkGroupsBidirectional(PressureGroup, AreaGroup, database)
            localMeasureService.linkGroupsBidirectional(DensityGroup, VolumeGroup, database)
            localMeasureService.linkGroupsBidirectional(PressureGroup, MassGroup, database)
            localMeasureService.linkGroupsBidirectional(DensityGroup, MassGroup, database)
            localMeasureService.linkGroupsBidirectional(WorkEnergyGroup, PowerEnergyGroup, database)
            localMeasureService.linkGroupsBidirectional(RotationFrequencyGroup, LengthGroup, database)
            localMeasureService.linkGroupsBidirectional(TorqueGroup, PowerGroup, database)
            localMeasureService.linkGroupsBidirectional(PowerEnergyGroup, TimeGroup, database)
            localMeasureService.linkGroupsBidirectional(SpeedGroup, TimeGroup, database)
            localMeasureService.linkGroupsBidirectional(RotationFrequencyGroup, TimeGroup, database)
        }
        return this
    }

    /** Initializes measures */
    fun initMeasuresSearch(): OrientDatabaseInitializer {
        val iName = "$MEASURE_VERTEX.lucene.name"
        val oClass = session.getClass(MEASURE_VERTEX)
        if (oClass.getClassIndex(iName) == null) {
            val metadata = ODocument()
            metadata.setProperty("allowLeadingWildcard", true)
            CreateIndexWrapper.createIndexWrapper(oClass, iName, "FULLTEXT", null, metadata, "LUCENE", arrayOf("name"))
        }
        return this
    }

    /** Create user in database */
    private fun initUser(username: String, password: String, role: String) = session(database) { session ->
        val user: OElement = session.newInstance(USER_CLASS)
        user.setProperty("username", username)
        user.setProperty("password", password)
        user.setProperty("role", role)
        user.save<ORecord>()
    }
}