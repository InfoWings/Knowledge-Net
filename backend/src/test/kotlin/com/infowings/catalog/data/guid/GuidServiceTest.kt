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
import com.infowings.catalog.data.objekt.ObjectPropertyValue
import com.infowings.catalog.data.objekt.ObjectService
import com.infowings.catalog.data.reference.book.ReferenceBookService
import com.infowings.catalog.randomName
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.test.assertEquals

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

        val property = AspectPropertyData("", randomName("p"), baseAspect.idStrict(), baseAspect.guidSoft(), PropertyCardinality.INFINITY.name, null)

        val ad2 = AspectData("", randomName("complex"), Kilometre.name, null, BaseType.Decimal.name, listOf(property))
        complexAspect = aspectService.save(ad2, username)

        subject = subjectService.createSubject(SubjectData.Initial(name = randomName("subject")), username)

        val textAspect = aspectService.save(
            AspectData(
                id = "", name = randomName("aspect"), description = "aspect description",
                version = 0, deleted = false, baseType = BaseType.Text.name
            ), username
        )

        refBook = refBookService.createReferenceBook(randomName("refbook"), textAspect.idStrict(), username)

        objectChange = objectService.create(ObjectCreateRequest(randomName("object"), "description", subject.id), username)

        propertyChange =
            objectService.create(PropertyCreateRequest(objectChange.id, randomName("object"), "property description", baseAspect.idStrict()), username)

        rootValue = transaction(db) {
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
            assertEquals(ValueDTO.nullValue(), objectValueBrief.value)
            assertEquals(baseAspect.name, objectValueBrief.aspectName)
        }
    }
}