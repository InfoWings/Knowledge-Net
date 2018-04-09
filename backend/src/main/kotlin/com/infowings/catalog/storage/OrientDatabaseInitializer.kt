package com.infowings.catalog.storage

import com.infowings.catalog.auth.UserEntity
import com.infowings.catalog.auth.Users
import com.infowings.catalog.common.*
import com.infowings.catalog.data.*
import com.infowings.catalog.data.history.*
import com.infowings.catalog.data.reference.book.REFERENCE_BOOK_ASPECT_EDGE
import com.infowings.catalog.data.reference.book.REFERENCE_BOOK_CHILD_EDGE
import com.infowings.catalog.data.reference.book.REFERENCE_BOOK_ITEM_VERTEX
import com.infowings.catalog.data.reference.book.REFERENCE_BOOK_VERTEX
import com.infowings.catalog.loggerFor
import com.orientechnologies.orient.core.db.document.ODatabaseDocument
import com.orientechnologies.orient.core.metadata.schema.OClass
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.OElement
import com.orientechnologies.orient.core.record.ORecord
import com.orientechnologies.orient.core.record.impl.ODocument

const val ATTR_NAME = "name"
const val ATTR_DESC = "description"

const val USER_CLASS = "User"
const val ASPECT_CLASS = "Aspect"
const val ASPECT_PROPERTY_CLASS = "AspectProperty"
const val ASPECT_ASPECT_PROPERTY_EDGE = "AspectPropertyEdge"
const val ASPECT_MEASURE_CLASS = "AspectToMeasure"
const val SUBJECT_CLASS = "Subject"
const val ASPECT_SUBJECT_EDGE = "AspectSubjectEdge"

const val SELECT_BY_NAME = "SELECT FROM ? where $ATTR_NAME = ? "


private val logger = loggerFor<OrientDatabaseInitializer>()

/** Initialization default db values, every method executes only in case describing part of db is empty. */
class OrientDatabaseInitializer(private val database: OrientDatabase) {

    private fun initVertex(session: ODatabaseDocument, name: String) =
        session.getClass(name) ?: session.createVertexClass(name)

    private fun initEdge(session: ODatabaseDocument, name: String) =
        session.getClass(name) ?: session.createEdgeClass(name)

    /** Executes only if there is no Class $USER_CLASS in db */
    fun initUsers(): OrientDatabaseInitializer = session(database) { session ->
        if (session.getClass(USER_CLASS) == null) {
            logger.info("Init users")
            initUser(Users.user)
            initUser(Users.admin)
            initUser(Users.poweredUser)
        }
        return@session this
    }

    /** Executes only if there is no Class Aspect in db */
    fun initAspects(): OrientDatabaseInitializer = session(database) { session ->
        logger.info("Init aspects")
        val aspectClass = session.getClass(ASPECT_CLASS) ?: createAspectVertex(session)
        aspectClass.getProperty(ATTR_DESC) ?: aspectClass.createProperty(ATTR_DESC, OType.STRING)
        if (session.getClass(ASPECT_CLASS) == null) {
            session.createVertexClass(ASPECT_CLASS)
                .createProperty("name", OType.STRING).isMandatory = true
            createIgnoreCaseIndex(session, ASPECT_CLASS)
        }
        session.getClass(ASPECT_PROPERTY_CLASS) ?: session.createVertexClass(ASPECT_PROPERTY_CLASS)
        session.getClass(ASPECT_MEASURE_CLASS) ?: session.createEdgeClass(ASPECT_MEASURE_CLASS)
        session.getClass(ASPECT_ASPECT_PROPERTY_EDGE) ?: session.createEdgeClass(ASPECT_ASPECT_PROPERTY_EDGE)

        return@session this
    }

    fun initHistory(): OrientDatabaseInitializer = session(database) { session ->
        initVertex(session, HISTORY_CLASS)
        initVertex(session, HISTORY_EVENT_CLASS)
        initVertex(session, HISTORY_ELEMENT_CLASS)
        initVertex(session, HISTORY_ADD_LINK_CLASS)
        initVertex(session, HISTORY_DROP_LINK_CLASS)

        initEdge(session, HISTORY_EDGE)

        return@session this
    }

    private fun createAspectVertex(session: ODatabaseDocument): OClass {
        val vertexClass = session.createVertexClass(ASPECT_CLASS)
        vertexClass.createProperty(ATTR_NAME, OType.STRING).isMandatory = true
        createIgnoreCaseIndex(session, ASPECT_CLASS)
        return vertexClass
    }

    private fun createVertexWithNameAndDesc(session: ODatabaseDocument, className: String) {
        val vertex = session.getClass(className) ?: createVertexWithName(className, session)
        vertex.getProperty(ATTR_DESC) ?: vertex.createProperty(ATTR_DESC, OType.STRING)
    }

    private fun createVertexWithName(className: String, session: ODatabaseDocument): OClass {
        logger.info("create vertex: $className")
        val vertex = session.createVertexClass(className)
        vertex.createProperty(ATTR_NAME, OType.STRING).setMandatory(true).createIndex(OClass.INDEX_TYPE.NOTUNIQUE)
        createIgnoreCaseIndex(session, className)
        return vertex
    }

    /** Initializes measures */
    fun initMeasures(): OrientDatabaseInitializer = session(database) { session ->
        createVertexWithNameAndDesc(session, MEASURE_GROUP_VERTEX)
        createVertexWithNameAndDesc(session, MEASURE_VERTEX)
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
        return@session this
    }

    /** Initializes measures search */
    fun initSearch() {
        initLuceneIndex(MEASURE_VERTEX)
        initLuceneIndex(ASPECT_CLASS)
        initLuceneIndex(SUBJECT_CLASS)
        initLuceneIndex(MEASURE_GROUP_VERTEX)
    }

    fun initReferenceBooks(): OrientDatabaseInitializer = session(database) { session ->
        logger.info("Init reference books")
        createVertexWithNameAndDesc(session, REFERENCE_BOOK_VERTEX)

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

    fun initSubject(): OrientDatabaseInitializer = session(database) { session ->
        logger.info("Init subject")
        createVertexWithNameAndDesc(session, SUBJECT_CLASS)
        session.getClass(ASPECT_SUBJECT_EDGE) ?: session.createEdgeClass(ASPECT_SUBJECT_EDGE)
        return@session this
    }

    private fun initLuceneIndex(classType: String) {
        initLuceneIndex(classType, ATTR_NAME)
        initLuceneIndex(classType, ATTR_DESC)
    }

    private fun initLuceneIndex(classType: String, attrName: String) =
        session(database) { session ->
            val iName = "$classType.lucene.$attrName"
            val oClass = session.getClass(classType)
            if (oClass.getClassIndex(iName) == null) {
                val metadata = ODocument()
                metadata.setProperty("allowLeadingWildcard", true)
                CreateIndexWrapper.createIndexWrapper(
                    oClass,
                    iName,
                    "FULLTEXT",
                    null,
                    metadata,
                    "LUCENE",
                    arrayOf(attrName)
                )
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

    private fun initUser(user: UserEntity) = user.apply {
        initUser(username, password, role.name)
    }

    private fun createIgnoreCaseIndex(session: ODatabaseDocument, className: String) {
        session.command("CREATE INDEX $className.index.name.ic ON $className (name COLLATE ci) NOTUNIQUE")
    }
}

