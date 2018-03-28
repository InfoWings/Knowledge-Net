package com.infowings.catalog.storage

import com.infowings.catalog.common.*
import com.infowings.catalog.data.*
import com.infowings.catalog.data.history.*
import com.infowings.catalog.loggerFor
import com.orientechnologies.orient.core.db.document.ODatabaseDocument
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

    private fun initVertex(session: ODatabaseDocument, name: String) {
        session.getClass(name) ?: session.createVertexClass(name)
    }

    private fun initEdge(session: ODatabaseDocument, name: String) {
        session.getClass(name) ?: session.createEdgeClass(name)
    }

    /** Executes only if there is no Class $USER_CLASS in db */
    fun initUsers(): OrientDatabaseInitializer = session(database) { session ->
        if (session.getClass(USER_CLASS) == null) {
            logger.info("Init users")
            initUser("user", "user", "USER")
            initUser("admin", "admin", "ADMIN")
            initUser("powereduser", "powereduser", "POWERED_USER")
        }
        return@session this
    }

    /** Executes only if there is no Class Aspect in db */
    fun initAspects(): OrientDatabaseInitializer = session(database) { session ->
        logger.info("Init aspects")
        if (session.getClass(ASPECT_CLASS) == null) {
            session.createVertexClass(ASPECT_CLASS)
                    .createProperty("name", OType.STRING).isMandatory = true
        }
        session.getClass(ASPECT_PROPERTY_CLASS) ?: session.createVertexClass(ASPECT_PROPERTY_CLASS)
        session.getClass(ASPECT_MEASURE_CLASS) ?: session.createEdgeClass(ASPECT_MEASURE_CLASS)
        session.getClass(ASPECT_ASPECTPROPERTY_EDGE) ?: session.createEdgeClass(ASPECT_ASPECTPROPERTY_EDGE)
        initVertex(session, HISTORY_CLASS)
        initVertex(session, HISTORY_EVENT_CLASS)
        initVertex(session, HISTORY_ELEMENT_CLASS)
        initVertex(session, HISTORY_ADD_LINK_CLASS)
        initVertex(session, HISTORY_DROP_LINK_CLASS)
        return@session this
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
        session.getClass(MEASURE_GROUP_EDGE) ?: session.createEdgeClass(MEASURE_GROUP_EDGE)
        session.getClass(MEASURE_BASE_EDGE) ?: session.createEdgeClass(MEASURE_BASE_EDGE)
        session.getClass(MEASURE_BASE_AND_GROUP_EDGE) ?: session.createEdgeClass(MEASURE_BASE_AND_GROUP_EDGE)

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
            localMeasureService.linkGroupsBidirectional(WorkEnergyGroup, PowerGroup)
            localMeasureService.linkGroupsBidirectional(RotationFrequencyGroup, LengthGroup)
            localMeasureService.linkGroupsBidirectional(TorqueGroup, ForceGroup)
            localMeasureService.linkGroupsBidirectional(PowerGroup, TimeGroup)
            localMeasureService.linkGroupsBidirectional(SpeedGroup, TimeGroup)
            localMeasureService.linkGroupsBidirectional(RotationFrequencyGroup, TimeGroup)
        }
        return@session initSearch()
    }

    /** Initializes measures search */
    private fun initSearch(): OrientDatabaseInitializer {
        initLuceneIndex(MEASURE_VERTEX)
        initLuceneIndex(ASPECT_CLASS)
        initLuceneIndex(MEASURE_GROUP_VERTEX)
        return this
    }

    fun initReferenceBooks(): OrientDatabaseInitializer = session(database) { session ->
        logger.info("Init reference books")
        if (session.getClass(REFERENCE_BOOK_VERTEX) == null) {
            val vertexClass = session.createVertexClass(REFERENCE_BOOK_VERTEX)
            vertexClass.createProperty("aspectId", OType.STRING).createIndex(OClass.INDEX_TYPE.UNIQUE)
        }
        if (session.getClass(REFERENCE_BOOK_ITEM_VERTEX) == null) {
            val vertexClass = session.createVertexClass(REFERENCE_BOOK_ITEM_VERTEX)
            vertexClass.createProperty("value", OType.STRING)
        }

        session.getClass(REFERENCE_BOOK_CHILD_EDGE) ?: session.createEdgeClass(REFERENCE_BOOK_CHILD_EDGE)
        session.getClass(REFERENCE_BOOK_ASPECT_EDGE) ?: session.createEdgeClass(REFERENCE_BOOK_ASPECT_EDGE)
        return@session this
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