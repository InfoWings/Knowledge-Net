package com.infowings.catalog.storage

import com.infowings.catalog.auth.user.HISTORY_USER_EDGE
import com.infowings.catalog.auth.user.UserDao
import com.infowings.catalog.auth.user.UserService
import com.infowings.catalog.common.*
import com.infowings.catalog.data.*
import com.infowings.catalog.data.history.*
import com.infowings.catalog.data.reference.book.ASPECT_REFERENCE_BOOK_EDGE
import com.infowings.catalog.data.reference.book.REFERENCE_BOOK_CHILD_EDGE
import com.infowings.catalog.data.reference.book.REFERENCE_BOOK_ITEM_VERTEX
import com.infowings.catalog.data.reference.book.REFERENCE_BOOK_ROOT_EDGE
import com.infowings.catalog.loggerFor
import com.orientechnologies.orient.core.db.document.ODatabaseDocument
import com.orientechnologies.orient.core.metadata.schema.OClass
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.record.impl.ODocument

enum class OrientClass(val extName: String) {
    ASPECT(ASPECT_CLASS),
    ASPECT_PROPERTY(ASPECT_PROPERTY_CLASS),
    SUBJECT(SUBJECT_CLASS),
    OBJECT(OBJECT_CLASS),
    OBJECT_PROPERTY(OBJECT_PROPERTY_CLASS),
    OBJECT_VALUE(OBJECT_PROPERTY_VALUE_CLASS),
    REFBOOK_ITEM(REFERENCE_BOOK_ITEM_VERTEX),
    MEASURE(MEASURE_VERTEX),
    MEASURE_GROUP(MEASURE_GROUP_VERTEX),
    HISTORY_EVENT(HISTORY_EVENT_CLASS),
    HISTORY_ELEMENT(HISTORY_ELEMENT_CLASS),
    HISTORY_ADD_LINK(HISTORY_ADD_LINK_CLASS),
    HISTORY_REMOVE_LINK(HISTORY_DROP_LINK_CLASS),
    USER(USER_CLASS)
}

enum class OrientEdge(val extName: String) {
    ASPECT_PROPERTY(ASPECT_ASPECT_PROPERTY_EDGE),
    SUBJECT_OF_ASPECT(ASPECT_SUBJECT_EDGE),
    SUBJECT_OF_OBJECT(OBJECT_SUBJECT_EDGE),
    OBJECT_OF_OBJECT_PROPERTY(OBJECT_OBJECT_PROPERTY_EDGE),
    ASPECT_OF_OBJECT_PROPERTY(ASPECT_OBJECT_PROPERTY_EDGE),
    OBJECT_PROPERTY_OF_OBJECT_VALUE(OBJECT_VALUE_OBJECT_PROPERTY_EDGE),
    ASPECT_PROPERTY_OF_OBJECT_VALUE(OBJECT_VALUE_ASPECT_PROPERTY_EDGE),
    OBJECT_VALUE_CHILD_OF_VALUE(OBJECT_VALUE_OBJECT_VALUE_EDGE),
    OBJECT_MEASURE_OF_VALUE(OBJECT_VALUE_MEASURE_EDGE),
    OBJECT_VALUE_REF_OBJECT(OBJECT_VALUE_OBJECT_EDGE),
    OBJECT_VALUE_REF_SUBJECT(OBJECT_VALUE_SUBJECT_EDGE),
    OBJECT_VALUE_REF_REFBOOK_ITEM(OBJECT_VALUE_REFBOOK_ITEM_EDGE),
}


const val ATTR_NAME = "name"
const val ATTR_DESC = "description"

const val USER_CLASS = "User"
const val ASPECT_CLASS = "Aspect"
const val ASPECT_PROPERTY_CLASS = "AspectProperty"
const val ASPECT_ASPECT_PROPERTY_EDGE = "AspectPropertyEdge"
const val ASPECT_MEASURE_CLASS = "AspectToMeasure"
const val SUBJECT_CLASS = "Subject"
const val ASPECT_SUBJECT_EDGE = "AspectSubjectEdge"
const val OBJECT_CLASS = "Object"
const val OBJECT_SUBJECT_EDGE = "ObjectToSubjectEdge"
const val OBJECT_PROPERTY_CLASS = "ObjectProperty"
const val OBJECT_OBJECT_PROPERTY_EDGE = "ObjectObjectPropertyEdge"
const val ASPECT_OBJECT_PROPERTY_EDGE = "AspectObjectPropertyEdge"
const val OBJECT_PROPERTY_VALUE_CLASS = "ObjectPropertyValue"
const val OBJECT_VALUE_OBJECT_PROPERTY_EDGE = "ObjectValueObjectPropertyEdge"
const val OBJECT_VALUE_ASPECT_PROPERTY_EDGE = "ObjectValueAspectPropertyEdge"
const val OBJECT_VALUE_OBJECT_VALUE_EDGE = "ObjectValueObjectValueEdge"
const val OBJECT_VALUE_OBJECT_EDGE = "ObjectValueObjectEdge"
const val OBJECT_VALUE_SUBJECT_EDGE = "ObjectValueSubjectEdge"
const val OBJECT_VALUE_REFBOOK_ITEM_EDGE = "ObjectValueRefBookItemEdge"
const val OBJECT_VALUE_MEASURE_EDGE = "ObjectValueMeasureEdge"

private val logger = loggerFor<OrientDatabaseInitializer>()

/** Initialization default db values, every method executes only in case describing part of db is empty. */
class OrientDatabaseInitializer(private val database: OrientDatabase) {

    private fun initVertex(session: ODatabaseDocument, name: String) =
        session.getClass(name) ?: session.createVertexClass(name)

    private fun initEdge(session: ODatabaseDocument, name: String) =
        session.getClass(name) ?: session.createEdgeClass(name)

    fun initUsers(users: List<User>): OrientDatabaseInitializer = session(database) { session ->
        logger.info("Init users: " + users.map { it.username })
        if (session.getClass(USER_CLASS) == null) {
            session.getClass(USER_CLASS) ?: session.createVertexClass(USER_CLASS)
            val userService = UserService(database, UserDao(database))
            users.forEach { userService.createUser(it) }
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

    fun initObject(): OrientDatabaseInitializer = session(database) { session ->
        logger.info("Init objects")
        if (session.getClass(OBJECT_CLASS) == null) {
            val vertexClass = session.createVertexClass(OBJECT_CLASS)
            vertexClass.createProperty(ATTR_NAME, OType.STRING).isMandatory = true
            createIgnoreCaseIndex(session, OBJECT_CLASS)
        }
        initEdge(session, OBJECT_SUBJECT_EDGE)

        initVertex(session, OBJECT_PROPERTY_CLASS)
        initEdge(session, OBJECT_OBJECT_PROPERTY_EDGE)
        initEdge(session, ASPECT_OBJECT_PROPERTY_EDGE)

        initVertex(session, OBJECT_PROPERTY_VALUE_CLASS)
        initEdge(session, OBJECT_VALUE_OBJECT_PROPERTY_EDGE)

        initEdge(session, OBJECT_VALUE_ASPECT_PROPERTY_EDGE)
        initEdge(session, OBJECT_VALUE_OBJECT_VALUE_EDGE)
        initEdge(session, OBJECT_VALUE_OBJECT_EDGE)
        initEdge(session, OBJECT_VALUE_SUBJECT_EDGE)
        initEdge(session, OBJECT_VALUE_REFBOOK_ITEM_EDGE)
        initEdge(session, OBJECT_VALUE_MEASURE_EDGE)

        return@session this
    }

    fun initHistory(): OrientDatabaseInitializer = session(database) { session ->
        initVertex(session, HISTORY_CLASS)
        initVertex(session, HISTORY_EVENT_CLASS)
        initVertex(session, HISTORY_ELEMENT_CLASS)
        initVertex(session, HISTORY_ADD_LINK_CLASS)
        initVertex(session, HISTORY_DROP_LINK_CLASS)

        initEdge(session, HISTORY_EDGE)
        initEdge(session, HISTORY_USER_EDGE)
        initEdge(session, HISTORY_ELEMENT_EDGE)
        initEdge(session, HISTORY_ADD_LINK_EDGE)
        initEdge(session, HISTORY_DROP_LINK_EDGE)

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

        if (session.getClass(REFERENCE_BOOK_ITEM_VERTEX) == null) {
            val vertexClass = session.createVertexClass(REFERENCE_BOOK_ITEM_VERTEX)
            vertexClass.createProperty("value", OType.STRING)
            vertexClass.createProperty("deleted", OType.BOOLEAN)
        }

        session.getClass(REFERENCE_BOOK_CHILD_EDGE) ?: session.createEdgeClass(REFERENCE_BOOK_CHILD_EDGE)
        session.getClass(REFERENCE_BOOK_ROOT_EDGE) ?: session.createEdgeClass(REFERENCE_BOOK_ROOT_EDGE)
        session.getClass(ASPECT_REFERENCE_BOOK_EDGE) ?: session.createEdgeClass(ASPECT_REFERENCE_BOOK_EDGE)
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

    private fun createIgnoreCaseIndex(session: ODatabaseDocument, className: String) {
        session.command("CREATE INDEX $className.index.name.ic ON $className (name COLLATE ci) NOTUNIQUE")
    }
}

