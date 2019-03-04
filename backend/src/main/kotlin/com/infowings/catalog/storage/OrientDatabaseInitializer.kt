package com.infowings.catalog.storage

import com.infowings.catalog.auth.user.HISTORY_USER_EDGE
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
import com.orientechnologies.orient.core.metadata.schema.OProperty
import com.orientechnologies.orient.core.metadata.schema.OType

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
    USER(USER_CLASS),
    GUID("Guid");

    companion object {
        private val ext2Class = OrientClass.values().map { it.extName to it }.toMap()

        fun fromExtName(extName: String): OrientClass = ext2Class.getValue(extName)
    }
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
    OBJECT_VALUE_REF_REFBOOK_ITEM(OBJECT_VALUE_REF_REFBOOK_ITEM_EDGE),
    OBJECT_VALUE_DOMAIN_ELEMENT(OBJECT_VALUE_DOMAIN_ELEMENT_EDGE),
    //    GUID_OF_OBJECT_PROPERTY("GuidOfObjectPropertyEdge"),
    GUID_OF_OBJECT_VALUE("GuidOfObjectValueEdge"),
}


const val ATTR_VALUE = "value"
const val ATTR_NAME = "name"
const val ATTR_DESC = "description"
const val ATTR_GUID = "guid"

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
const val OBJECT_VALUE_REF_OBJECT_PROPERTY_EDGE = "ObjectValueRefObjectPropertyEdge"
const val OBJECT_VALUE_REF_OBJECT_VALUE_EDGE = "ObjectValueRefObjectValueEdge"
const val OBJECT_VALUE_SUBJECT_EDGE = "ObjectValueSubjectEdge"
const val OBJECT_VALUE_DOMAIN_ELEMENT_EDGE = "ObjectValueRefBookItemEdge"
const val OBJECT_VALUE_REF_REFBOOK_ITEM_EDGE = "ObjectValueRefRefBookItemEdge"
const val OBJECT_VALUE_MEASURE_EDGE = "ObjectValueMeasureEdge"
const val OBJECT_VALUE_ASPECT_EDGE = "ObjectValueAspectEdge"
const val OBJECT_VALUE_REF_ASPECT_PROPERTY_EDGE = "ObjectValueRefAspectPropertyEdge"

private val logger = loggerFor<OrientDatabaseInitializer>()

/** Initialization default db values, every method executes only in case describing part of db is empty. */
class OrientDatabaseInitializer(private val database: OrientDatabase) {

    private fun ODatabaseDocument.initVertex(name: String) = getClass(name) ?: createVertexClass(name)

    private fun ODatabaseDocument.initEdge(name: String) = getClass(name) ?: createEdgeClass(name)

    private fun OClass.initProperty(name: String, type: OType, block: OProperty.() -> Unit = {}): OProperty {
        val property = getProperty(name)
        if (property != null)
            return property

        return createProperty(name, type).apply { block() }
    }

    fun initUsers(): OrientDatabaseInitializer = session(database) { session ->
        logger.info("Init user vertex class")
        session.initVertex(USER_CLASS)
        return@session this
    }

    /** Executes only if there is no Class Aspect in db */
    fun initAspects(): OrientDatabaseInitializer = session(database) { session ->
        logger.info("Init aspects")

        createAspectVertex(session)
        createAspectPropertyVertex(session)

        session.initEdge(ASPECT_MEASURE_CLASS)
        session.initEdge(ASPECT_ASPECT_PROPERTY_EDGE)

        return@session this
    }

    fun initObject(): OrientDatabaseInitializer = session(database) { session ->
        logger.info("Init objects")

        val objectVertexClass = session.initVertex(OBJECT_CLASS)

        objectVertexClass.initProperty(ATTR_NAME, OType.STRING) {
            isMandatory = true
        }
        objectVertexClass.initProperty(ATTR_DESC, OType.STRING)

        addGuidProperty(objectVertexClass)

        initIgnoreCaseIndex(OBJECT_CLASS)

        session.initEdge(OBJECT_SUBJECT_EDGE)

        val objectPropertyVertex = session.initVertex(OBJECT_PROPERTY_CLASS)
        addGuidProperty(objectPropertyVertex)

        session.initEdge(OBJECT_OBJECT_PROPERTY_EDGE)
        session.initEdge(ASPECT_OBJECT_PROPERTY_EDGE)

        if (session.getClass(OrientClass.OBJECT_VALUE.extName) == null) {
            val objectValueVertexClass = session.createVertexClass(OrientClass.OBJECT_VALUE.extName)
            objectValueVertexClass.createProperty("str", OType.STRING)
            database.createLuceneIndex(OrientClass.OBJECT_VALUE.extName, "str")
        }

        session.initEdge(OBJECT_VALUE_OBJECT_PROPERTY_EDGE)

        session.initEdge(OBJECT_VALUE_ASPECT_PROPERTY_EDGE)
        session.initEdge(OBJECT_VALUE_OBJECT_VALUE_EDGE)
        session.initEdge(OBJECT_VALUE_OBJECT_EDGE)
        session.initEdge(OBJECT_VALUE_REF_OBJECT_PROPERTY_EDGE)
        session.initEdge(OBJECT_VALUE_REF_OBJECT_VALUE_EDGE)
        session.initEdge(OBJECT_VALUE_ASPECT_EDGE)
        session.initEdge(OBJECT_VALUE_REF_ASPECT_PROPERTY_EDGE)
        session.initEdge(OBJECT_VALUE_SUBJECT_EDGE)
        session.initEdge(OBJECT_VALUE_DOMAIN_ELEMENT_EDGE)
        session.initEdge(OBJECT_VALUE_REF_REFBOOK_ITEM_EDGE)
        session.initEdge(OBJECT_VALUE_MEASURE_EDGE)


        return@session this
    }

    fun initHistory(): OrientDatabaseInitializer = session(database) { session ->
        session.initVertex(HISTORY_CLASS)
        session.initVertex(HISTORY_EVENT_CLASS)
        session.initVertex(HISTORY_ELEMENT_CLASS)
        session.initVertex(HISTORY_ADD_LINK_CLASS)
        session.initVertex(HISTORY_DROP_LINK_CLASS)

        session.initEdge(HISTORY_EDGE)
        session.initEdge(HISTORY_USER_EDGE)
        session.initEdge(HISTORY_ELEMENT_EDGE)
        session.initEdge(HISTORY_ADD_LINK_EDGE)
        session.initEdge(HISTORY_DROP_LINK_EDGE)

        return@session this
    }

    fun initGuid(): OrientDatabaseInitializer = session(database) { session ->
        session.initVertex(OrientClass.GUID.extName)

        listOf(
            OrientEdge.GUID_OF_OBJECT_VALUE
        ).forEach {
            session.initEdge(it.extName)
        }

        return@session this
    }

    private fun createAspectVertex(session: ODatabaseDocument): OClass {
        val aspectClass = session.initVertex(ASPECT_CLASS)

        aspectClass.initProperty(ATTR_DESC, OType.STRING)
        aspectClass.initProperty(ATTR_NAME, OType.STRING) {
            isMandatory = true
        }
        addGuidProperty(aspectClass)
        initIgnoreCaseIndex(ASPECT_CLASS)

        return aspectClass
    }

    private fun createAspectPropertyVertex(session: ODatabaseDocument): OClass {
        val aspectPropertyClass = session.initVertex(ASPECT_PROPERTY_CLASS)

        addGuidProperty(aspectPropertyClass)

        aspectPropertyClass.initProperty("name_with_aspect", OType.STRING)

        return aspectPropertyClass
    }

    private fun createVertexWithNameAndDesc(session: ODatabaseDocument, className: String): OClass {
        val vertex = session.initVertex(className)

        vertex.initProperty(ATTR_NAME, OType.STRING) {
            isMandatory = true
            initBasicIndex()
        }
        vertex.initProperty(ATTR_DESC, OType.STRING)
        initIgnoreCaseIndex(className)

        return vertex
    }

    /** Initializes measures */
    fun initMeasures(): OrientDatabaseInitializer = session(database) { session ->
        createVertexWithNameAndDesc(session, MEASURE_GROUP_VERTEX)
        createVertexWithNameAndDesc(session, MEASURE_VERTEX)
        session.initEdge(MEASURE_GROUP_EDGE)
        session.initEdge(MEASURE_BASE_EDGE)
        session.initEdge(MEASURE_BASE_AND_GROUP_EDGE)

        /** Add initial measures to database */
        val localMeasureService = MeasureService(database)
        transaction(database) {
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

    fun initSearch(): OrientDatabaseInitializer {
        initLuceneIndex(ASPECT_CLASS)
        initLuceneIndex(SUBJECT_CLASS)
        initLuceneIndex(OBJECT_CLASS)
        database.createLuceneIndex(OrientClass.REFBOOK_ITEM.extName, ATTR_VALUE)
        database.createLuceneIndex(OrientClass.REFBOOK_ITEM.extName, ATTR_DESC)
        database.createLuceneIndex(OrientClass.ASPECT_PROPERTY.extName, "name_with_aspect")

        return this
    }

    fun initSearchMeasure() {
        initLuceneIndex(MEASURE_VERTEX)
        initLuceneIndex(MEASURE_GROUP_VERTEX)
    }


    fun initReferenceBooks(): OrientDatabaseInitializer = session(database) { session ->
        logger.info("Init reference books")

        val vertexClass = session.initVertex(REFERENCE_BOOK_ITEM_VERTEX)

        vertexClass.initProperty("value", OType.STRING)
        vertexClass.initProperty("description", OType.STRING)
        vertexClass.initProperty("deleted", OType.BOOLEAN)

        addGuidProperty(vertexClass)

        session.getClass(REFERENCE_BOOK_CHILD_EDGE) ?: session.createEdgeClass(REFERENCE_BOOK_CHILD_EDGE)
        session.getClass(REFERENCE_BOOK_ROOT_EDGE) ?: session.createEdgeClass(REFERENCE_BOOK_ROOT_EDGE)
        session.getClass(ASPECT_REFERENCE_BOOK_EDGE) ?: session.createEdgeClass(ASPECT_REFERENCE_BOOK_EDGE)

        return@session this
    }

    fun initSubject(): OrientDatabaseInitializer = session(database) { session ->
        logger.info("Init subject")
        val subjectVertex = createVertexWithNameAndDesc(session, SUBJECT_CLASS)
        addGuidProperty(subjectVertex)

        session.initEdge(ASPECT_SUBJECT_EDGE)
        return@session this
    }

    private fun addGuidProperty(oClass: OClass) {
        oClass.initProperty(ATTR_GUID, OType.STRING) {
            isMandatory = true
            initBasicIndex()
        }
    }

    private fun initLuceneIndex(classType: String) {
        database.createLuceneIndex(classType, ATTR_NAME)
        database.createLuceneIndex(classType, ATTR_DESC)
    }

    private fun initIgnoreCaseIndex(className: String) = database.getICIndex(className) ?: database.createICIndex(className)

    private fun OProperty.initBasicIndex() = createIndex(OClass.INDEX_TYPE.NOTUNIQUE)
}