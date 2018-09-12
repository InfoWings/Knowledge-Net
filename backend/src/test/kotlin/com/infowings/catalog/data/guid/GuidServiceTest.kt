package com.infowings.catalog.data.guid

import com.infowings.catalog.common.*
import com.infowings.catalog.common.guid.EntityClass
import com.infowings.catalog.common.objekt.ObjectChangeResponse
import com.infowings.catalog.common.objekt.ObjectCreateRequest
import com.infowings.catalog.common.objekt.PropertyCreateRequest
import com.infowings.catalog.common.objekt.PropertyCreateResponse
import com.infowings.catalog.data.Subject
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.aspect.toAspectPropertyVertex
import com.infowings.catalog.data.aspect.toAspectVertex
import com.infowings.catalog.data.history.HistoryFact
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.data.objekt.*
import com.infowings.catalog.data.reference.book.RefBookField
import com.infowings.catalog.data.reference.book.ReferenceBookService
import com.infowings.catalog.data.reference.book.toReferenceBookItemVertex
import com.infowings.catalog.data.subject.SubjectField
import com.infowings.catalog.data.subject.toSubjectVertex
import com.infowings.catalog.randomName
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OEdge
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.fail

@ExtendWith(SpringExtension::class)
@SpringBootTest
@Suppress("LargeClass", "UnsafeCallOnNullableType")
class GuidServiceTest {
    private val username = "admin"

    @Autowired
    lateinit var subjectService: SubjectService

    @Autowired
    lateinit var aspectService: AspectService

    @Autowired
    lateinit var guidService: GuidService

    @Autowired
    lateinit var refBookService: ReferenceBookService

    @Autowired
    lateinit var objectService: ObjectService

    @Autowired
    lateinit var historyService: HistoryService

    @Autowired
    lateinit var db: OrientDatabase

    lateinit var baseAspect: AspectData
    lateinit var complexAspect: AspectData
    lateinit var subject: Subject
    lateinit var refBook: ReferenceBook
    lateinit var objectChange: ObjectChangeResponse
    lateinit var propertyChange: PropertyCreateResponse
    lateinit var rootValue: ObjectPropertyValue

    @BeforeEach
    fun initData() {
        val ad = AspectData("", randomName("base"), Kilometre.name, null, BaseType.Decimal.name, emptyList())
        baseAspect = aspectService.save(ad, username)

        val property = AspectPropertyData("", randomName("p"), baseAspect.idStrict(), PropertyCardinality.INFINITY.name, null)

        val ad2 = AspectData("", randomName("complex"), Kilometre.name, null, BaseType.Decimal.name, listOf(property))
        complexAspect = aspectService.save(ad2, username)

        subject = subjectService.createSubject(SubjectData.Initial(name = randomName("subject")), username)

        val textAspect = aspectService.save(AspectData(id = "", name = randomName("aspect"), description = "aspect description",
            version = 0, deleted = false, baseType = BaseType.Text.name), username)

        refBook = refBookService.createReferenceBook(randomName("refbook"), textAspect.idStrict(), username)

        objectChange = objectService.create(ObjectCreateRequest(randomName("object"), "description", subject.id), username)

        propertyChange = objectService.create(PropertyCreateRequest(objectChange.id, randomName("object"), "property description", baseAspect.idStrict()), username)

        rootValue = transaction (db) {
            objectService.findPropertyValueById(propertyChange.rootValue.id).toObjectPropertyValue()
        }
    }

    @Test
    fun testGuidMetadata() {
        val aspectMetas = guidService.metadata(listOfNotNull(baseAspect.guid))
        transaction(db) {
            assertEquals(1, aspectMetas.size)
            val meta = aspectMetas.first()
            assertEquals(meta.guid, baseAspect.guid)
            assertEquals(meta.id, baseAspect.id)
            assertEquals(meta.entityClass, EntityClass.ASPECT)
        }

        val property = complexAspect.properties[0]
        val aspectPropertyMetas = guidService.metadata(listOfNotNull(property.guid))
        transaction(db) {
            assertEquals(1, aspectPropertyMetas.size)
            val meta = aspectPropertyMetas.first()
            assertEquals(meta.guid, property.guid)
            assertEquals(meta.id, property.id)
            assertEquals(meta.entityClass, EntityClass.ASPECT_PROPERTY)
        }

        val subjectMetas = guidService.metadata(listOfNotNull(subject.guid))
        transaction(db) {
            assertEquals(1, subjectMetas.size)
            val meta = subjectMetas.first()
            assertEquals(meta.guid, subject.guid)
            assertEquals(meta.id, subject.id)
            assertEquals(meta.entityClass, EntityClass.SUBJECT)
        }

        val refBookMetas = guidService.metadata(listOfNotNull(refBook.guid))
        transaction(db) {
            assertEquals(1, refBookMetas.size)
            val meta = refBookMetas.first()
            assertEquals(refBook.guid, meta.guid)
            assertEquals(refBook.id, meta.id)
            assertEquals(EntityClass.REFBOOK_ITEM, meta.entityClass)
        }

        val objectMetas = guidService.metadata(listOfNotNull(objectChange.guid))
        transaction(db) {
            assertEquals(1, objectMetas.size)
            val meta = objectMetas.first()
            assertEquals(objectChange.guid, meta.guid)
            assertEquals(objectChange.id, meta.id)
            assertEquals(EntityClass.OBJECT, meta.entityClass)
        }

        val objectPropertyMetas = guidService.metadata(listOfNotNull(propertyChange.guid))
        transaction(db) {
            assertEquals(1, objectPropertyMetas.size)
            val meta = objectPropertyMetas.first()
            assertEquals(propertyChange.guid, meta.guid)
            assertEquals(propertyChange.id, meta.id)
            assertEquals(EntityClass.OBJECT_PROPERTY, meta.entityClass)
        }

        val rootValueGuid = rootValue.guid ?: throw IllegalStateException()
        val objectValueMetas = guidService.metadata(listOfNotNull(rootValueGuid))
        transaction(db) {
            assertEquals(1, objectValueMetas.size)
            val meta = objectValueMetas.first()
            assertEquals(rootValueGuid, meta.guid)
            assertEquals(propertyChange.rootValue.id, meta.id)
            assertEquals(EntityClass.OBJECT_VALUE, meta.entityClass)
        }
    }

    @Test
    fun testFind() {
        val aspects = guidService.findAspects(listOfNotNull(baseAspect.guid))
        transaction(db) {
            assertEquals(1, aspects.size)
            val found = aspects.first()
            assertEquals(baseAspect.id, found.id)
        }

        val property = complexAspect.properties[0]
        val aspectProperties = guidService.findAspectProperties(listOfNotNull(property.guid))
        transaction(db) {
            assertEquals(1, aspectProperties.size)
            val found = aspectProperties.first()
            assertEquals(property.id, found.id)
        }

        val subjects = guidService.findSubject(listOfNotNull(subject.guid))
        transaction(db) {
            assertEquals(1, subjects.size)
            val found = subjects.first()
            assertEquals(subject.id, found.id)
        }

        val refBookItems = guidService.findRefBookItems(listOfNotNull(refBook.guid))
        transaction(db) {
            assertEquals(1, refBookItems.size)
            val found = refBookItems.first()
            assertEquals(refBook.id, found.id)
        }

        val objectBrief = guidService.findObject(objectChange.guid!!)
        transaction(db) {
            assertEquals(objectChange.name, objectBrief.name)
        }

        val objectProperties = guidService.findObjectProperties(listOfNotNull(propertyChange.guid))
        transaction(db) {
            assertEquals(1, objectProperties.size)
            val found = objectProperties.first()
            assertEquals(propertyChange.id, found.id)
        }

        val objectValueBrief = guidService.findObjectValue(rootValue.guid!!)
        transaction(db) {
            assertEquals(rootValue.guid, objectValueBrief.guid)
            assertEquals(nullValueDto(), objectValueBrief.value)
            assertEquals(baseAspect.name, objectValueBrief.aspectName)
        }
    }

    @Suppress("EmptyCatchBlock")
    private fun resetExistingId(id: String) {
        try {
            guidService.setGuid(id, "admin")
            fail("No exception thrown")
        } catch (e: IllegalStateException) {
        }
    }

    private fun checkGuidFact(fact: HistoryFact, orientClass: OrientClass, fieldName: String) {
        assertEquals(orientClass.extName, fact.event.entityClass)
        assertEquals(setOf(fieldName), fact.payload.data.keys)

    }

    @Test
    fun testSetGuidAspect() {
        val property = complexAspect.properties[0]

        val aspectGuid = baseAspect.guid ?: throw IllegalStateException()
        val aspectPropertyGuid = property.guid ?: throw IllegalStateException()

        val aspectId = baseAspect.idStrict()
        val aspectPropertyId = property.id

        val aspectVertex = db.getVertexById(aspectId) ?: throw IllegalArgumentException()
        val aspectPropertyVertex = db.getVertexById(aspectPropertyId) ?: throw IllegalArgumentException()

        transaction(db) {
            aspectVertex.deleteOutEdges(OrientEdge.GUID_OF_ASPECT)
            aspectPropertyVertex.deleteOutEdges(OrientEdge.GUID_OF_ASPECT_PROPERTY)
        }

        val aspectMeta = guidService.setGuid(aspectId, "admin")
        assertNotEquals(aspectGuid, aspectMeta.guid)

        val aspectPropertyMeta = guidService.setGuid(aspectPropertyId, "admin")
        assertNotEquals(aspectPropertyGuid, aspectPropertyMeta.guid)

        val aspects = guidService.findAspects(listOf(aspectMeta.guid))
        assertEquals(1, aspects.size)
        val foundAspect = aspects.first()
        assertEquals(aspectId, foundAspect.id)
        assertEquals(aspectMeta.guid, foundAspect.guid)

        val timelineAspect = historyService.entityTimeline(aspectId)
        checkGuidFact(timelineAspect.last(), OrientClass.ASPECT, AspectField.GUID.name)

        resetExistingId(aspectId)
        resetExistingId(aspectPropertyId)

        val aspectVertexUpdated = db.getVertexById(aspectId) ?: throw IllegalArgumentException()
        transaction(db) { aspectVertexUpdated.deleteOutEdges(OrientEdge.GUID_OF_ASPECT) }
        val aspectPropertyVertexUpdated = db.getVertexById(aspectPropertyId) ?: throw IllegalArgumentException()
        transaction(db) { aspectPropertyVertexUpdated.deleteOutEdges(OrientEdge.GUID_OF_ASPECT_PROPERTY) }

        val setResultAspect = guidService.setGuids(OrientClass.ASPECT, "admin")
        assertEquals(1, setResultAspect.size)
        val setResultAspectProperty = guidService.setGuids(OrientClass.ASPECT_PROPERTY, "admin")
        assertEquals(1, setResultAspectProperty.size)

        val aspectMetaElement = setResultAspect.first()
        assertEquals(aspectId, aspectMetaElement.id)
        val aspectPropertyMetaElement = setResultAspectProperty.first()
        assertEquals(aspectPropertyId, aspectPropertyMetaElement.id)

        transaction(db) {
            val aspectVertexFresh = db.getVertexById(aspectId)?.toAspectVertex() ?: throw IllegalArgumentException()
            val aspectPropertyVertexFresh = db.getVertexById(aspectPropertyId)?.toAspectPropertyVertex() ?: throw IllegalArgumentException()

            assertEquals(aspectVertexFresh.id, aspectMetaElement.id)
            assertEquals(aspectPropertyVertexFresh.id, aspectPropertyMetaElement.id)

            assertEquals(aspectMetaElement.guid, aspectVertexFresh.guid)
            assertEquals(aspectPropertyMetaElement.guid, aspectPropertyVertexFresh.guid)
        }
    }

    @Test
    fun testSetGuidSubject() {
        val subjectGuid = subject.guid ?: throw IllegalStateException()

        val subjectId = subject.id

        val subjectVertex = db.getVertexById(subjectId) ?: throw IllegalArgumentException()

        transaction(db) {
            subjectVertex.deleteOutEdges(OrientEdge.GUID_OF_SUBJECT)
        }

        val subjectMeta = guidService.setGuid(subjectId, "admin")
        assertNotEquals(subjectGuid, subjectMeta.guid)

        val timeline = historyService.entityTimeline(subjectId)
        checkGuidFact(timeline.last(), OrientClass.SUBJECT, SubjectField.GUID.extName)

        resetExistingId(subjectId)

        val subjectVertexUpdated = db.getVertexById(subjectId) ?: throw IllegalArgumentException()
        transaction(db) { subjectVertexUpdated.deleteOutEdges(OrientEdge.GUID_OF_SUBJECT) }

        val setResult = guidService.setGuids(OrientClass.SUBJECT, "admin")
        assertEquals(1, setResult.size)
        val resultElement = setResult.first()
        assertEquals(subjectId, resultElement.id)

        transaction(db) {
            val subjectVertexFresh = db.getVertexById(subjectId)?.toSubjectVertex() ?: throw IllegalArgumentException()
            assertEquals(subjectId, resultElement.id)
            assertEquals(resultElement.guid, subjectVertexFresh.guid)
        }
    }

    @Test
    fun testSetGuidRbi() {
        val rbiGuid = refBook.guid ?: throw IllegalStateException()

        val rbiId = refBook.id

        val rbiVertex = db.getVertexById(rbiId) ?: throw IllegalArgumentException()

        transaction(db) { rbiVertex.deleteOutEdges(OrientEdge.GUID_OF_REFBOOK_ITEM) }

        val rbiMeta = guidService.setGuid(rbiId, "admin")
        assertNotEquals(rbiGuid, rbiMeta.guid)

        val timeline = historyService.entityTimeline(rbiId)
        checkGuidFact(timeline.last(), OrientClass.REFBOOK_ITEM, RefBookField.GUID.extName)

        resetExistingId(rbiId)

        val rbiVertexUpdated = db.getVertexById(rbiId) ?: throw IllegalArgumentException()
        transaction(db) { rbiVertexUpdated.deleteOutEdges(OrientEdge.GUID_OF_REFBOOK_ITEM) }

        val setResult = guidService.setGuids(OrientClass.REFBOOK_ITEM, "admin")
        assertEquals(1, setResult.size)

        val metaElement = setResult.first()
        assertEquals(rbiId, metaElement.id)

        transaction(db) {
            val rbiVertexFresh = db.getVertexById(rbiId)?.toReferenceBookItemVertex() ?: throw IllegalArgumentException()
            assertEquals(rbiId, metaElement.id)
            assertEquals(metaElement.guid, rbiVertexFresh.guid)
        }
    }

    @Test
    fun testSetGuidObject() {
        val objectGuid = objectChange.guid ?: throw IllegalStateException()

        val objectId = objectChange.id

        val objectVertex = db.getVertexById(objectId) ?: throw IllegalArgumentException()

        transaction(db) {
            objectVertex.getEdges(ODirection.OUT, OrientEdge.GUID_OF_OBJECT.extName).forEach { it.delete<OEdge>() }
        }

        val objectMeta = guidService.setGuid(objectId, "admin")
        assertNotEquals(objectGuid, objectMeta.guid)

        val timeline = historyService.entityTimeline(objectId)
        checkGuidFact(timeline.last(), OrientClass.OBJECT, "guid")

        resetExistingId(objectId)

        val objectVertexUpdated = db.getVertexById(objectId) ?: throw IllegalArgumentException()
        transaction(db) { objectVertexUpdated.deleteOutEdges(OrientEdge.GUID_OF_OBJECT) }

        val setResult = guidService.setGuids(OrientClass.OBJECT, "admin")
        assertEquals(1, setResult.size)

        val metaElement = setResult.first()
        assertEquals(objectId, metaElement.id)

        transaction(db) {
            val objectVertexFresh = db.getVertexById(objectId)?.toObjectVertex() ?: throw IllegalArgumentException()
            assertEquals(objectId, metaElement.id)
            assertEquals(metaElement.guid, objectVertexFresh.guid)
        }
    }

    @Test
    fun testSetGuidObjectProperty() {
        val propertyGuid = propertyChange.guid ?: throw IllegalStateException()

        val propertyId = propertyChange.id

        val propertyVertex = db.getVertexById(propertyId) ?: throw IllegalArgumentException()

        transaction(db) {
            propertyVertex.getEdges(ODirection.OUT, OrientEdge.GUID_OF_OBJECT_PROPERTY.extName).forEach { it.delete<OEdge>() }
        }

        val propertyMeta = guidService.setGuid(propertyId, "admin")
        assertNotEquals(propertyGuid, propertyMeta.guid)

        val timeline = historyService.entityTimeline(propertyId)
        checkGuidFact(timeline.last(), OrientClass.OBJECT_PROPERTY, "guid")

        resetExistingId(propertyId)

        val propertyVertexUpdated = db.getVertexById(propertyId) ?: throw IllegalArgumentException()
        transaction(db) { propertyVertexUpdated.deleteOutEdges(OrientEdge.GUID_OF_OBJECT_PROPERTY) }

        val setResult = guidService.setGuids(OrientClass.OBJECT_PROPERTY, "admin")
        assertEquals(1, setResult.size)

        val metaElement = setResult.first()
        assertEquals(propertyId, metaElement.id)

        transaction(db) {
            val objectVertexFresh = db.getVertexById(propertyId)?.toObjectPropertyVertex() ?: throw IllegalArgumentException()
            assertEquals(propertyId, metaElement.id)
            assertEquals(metaElement.guid, objectVertexFresh.guid)
        }
    }

    @Test
    fun testSetGuidObjectValue() {
        val valueGuid = rootValue.guid ?: throw IllegalStateException()
        val valueId = rootValue.id.toString()

        val valueVertex = db.getVertexById(valueId) ?: throw IllegalArgumentException()

        transaction(db) { valueVertex.deleteOutEdges(OrientEdge.GUID_OF_OBJECT_VALUE) }

        val valueMeta = guidService.setGuid(valueId, "admin")
        assertNotEquals(valueGuid, valueMeta.guid)

        val timeline = historyService.entityTimeline(valueId)
        checkGuidFact(timeline.last(), OrientClass.OBJECT_VALUE, "guid")

        resetExistingId(valueId)

        val valueVertexUpdated = db.getVertexById(valueId) ?: throw IllegalArgumentException()
        transaction(db) { valueVertexUpdated.deleteOutEdges(OrientEdge.GUID_OF_OBJECT_VALUE) }

        val setResult = guidService.setGuids(OrientClass.OBJECT_VALUE, "admin")
        assertEquals(1, setResult.size)

        val metaElement = setResult.first()
        assertEquals(valueId, metaElement.id)

        transaction(db) {
            val valueVertexFresh = db.getVertexById(valueId)?.toObjectPropertyValueVertex() ?: throw IllegalArgumentException()
            assertEquals(valueId, metaElement.id)
            assertEquals(metaElement.guid, valueVertexFresh.guid)
        }
    }
}