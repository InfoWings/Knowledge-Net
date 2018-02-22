package com.infowings.catalog.storage

import com.infowings.catalog.common.*
import com.infowings.catalog.data.*
import com.infowings.catalog.loggerFor
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
                    .createProperty("name", OType.STRING)
                    .setMandatory(true)
                    .createIndex(OClass.INDEX_TYPE.UNIQUE)
        }
        session.getClass(ASPECT_PROPERTY_CLASS) ?: session.createVertexClass(ASPECT_PROPERTY_CLASS)
        session.getClass(ASPECT_MEASURE_CLASS) ?: session.createEdgeClass(ASPECT_MEASURE_CLASS)
        session.getClass(ASPECT_ASPECTPROPERTY_EDGE) ?: session.createEdgeClass(ASPECT_ASPECTPROPERTY_EDGE)
        return initAspectSearch()
    }

    /** Initializes measures */
    fun initMeasures(): OrientDatabaseInitializer = session(database) { session ->
        if (session.getClass(MEASURE_GROUP_VERTEX) == null) {
            val vertexClass = session.createVertexClass(MEASURE_GROUP_VERTEX)
            vertexClass.createProperty("name", OType.STRING).setMandatory(true).createIndex(OClass.INDEX_TYPE.UNIQUE)
        }
        if (session.getClass(MEASURE_VERTEX) == null) {
            val vertexClass = session.createVertexClass(MEASURE_VERTEX)
            vertexClass.createProperty("name", OType.STRING).setMandatory(true).createIndex(OClass.INDEX_TYPE.UNIQUE)
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
        val localMeasureService = MeasureService(database)
        session(database) {
            MeasureGroupMap.values.forEach { localMeasureService.saveGroup(it) }
            localMeasureService.linkGroupsBidirectional(AreaGroup, LengthGroup)
            localMeasureService.linkGroupsBidirectional(VolumeGroup, LengthGroup)
            localMeasureService.linkGroupsBidirectional(SpeedGroup, LengthGroup)
            localMeasureService.linkGroupsBidirectional(TorqueGroup, LengthGroup)
            localMeasureService.linkGroupsBidirectional(PressureGroup, AreaGroup)
            localMeasureService.linkGroupsBidirectional(DensityGroup, VolumeGroup)
            localMeasureService.linkGroupsBidirectional(PressureGroup, MassGroup)
            localMeasureService.linkGroupsBidirectional(DensityGroup, MassGroup)
            localMeasureService.linkGroupsBidirectional(WorkEnergyGroup, PowerEnergyGroup)
            localMeasureService.linkGroupsBidirectional(RotationFrequencyGroup, LengthGroup)
            localMeasureService.linkGroupsBidirectional(TorqueGroup, PowerGroup)
            localMeasureService.linkGroupsBidirectional(PowerEnergyGroup, TimeGroup)
            localMeasureService.linkGroupsBidirectional(SpeedGroup, TimeGroup)
            localMeasureService.linkGroupsBidirectional(RotationFrequencyGroup, TimeGroup)
        }
        return initMeasuresSearch()
    }

    /** Initializes measures search */
    private fun initMeasuresSearch(): OrientDatabaseInitializer {
        initLuceneIndex(MEASURE_VERTEX)
        return this
    }

    /** Initializes aspect search */
    private fun initAspectSearch(): OrientDatabaseInitializer {
        initLuceneIndex(ASPECT_CLASS)
        return this
    }

    private fun initLuceneIndex(classType: String) =
            session(database) { session ->
                val iName = "$classType.lucene.name"
                val oClass = session.getClass(classType)
                if (oClass.getClassIndex(iName) == null) {
                    val metadata = ODocument()
                    metadata.setProperty("allowLeadingWildcard", true)
                    CreateIndexWrapper.createIndexWrapper(oClass, iName, "FULLTEXT", null, metadata, "LUCENE", arrayOf("name"))
                }
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