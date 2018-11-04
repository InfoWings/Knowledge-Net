package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.ObjectCreateRequest
import com.infowings.catalog.common.objekt.PropertyCreateRequest
import com.infowings.catalog.common.objekt.ValueCreateRequest
import com.infowings.catalog.data.MeasureService
import com.infowings.catalog.data.Subject
import com.infowings.catalog.data.SubjectNotFoundException
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectDaoService
import com.infowings.catalog.data.aspect.AspectDoesNotExist
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.reference.book.ReferenceBookService
import com.infowings.catalog.randomName
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.record.impl.OVertexDocument
import org.junit.Assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.test.assertEquals
import kotlin.test.fail

@ExtendWith(SpringExtension::class)
@SpringBootTest
@Suppress("StringLiteralDuplication")
class ObjectValidatorTest {
    @Autowired
    private lateinit var db: OrientDatabase
    @Autowired
    private lateinit var dao: ObjectDaoService
    @Autowired
    private lateinit var subjectService: SubjectService
    @Autowired
    private lateinit var aspectDao: AspectDaoService
    @Autowired
    private lateinit var aspectService: AspectService
    @Autowired
    private lateinit var measureService: MeasureService
    @Autowired
    private lateinit var objectService: ObjectService
    @Autowired
    private lateinit var refBookService: ReferenceBookService


    private lateinit var validator: ObjectValidator

    private lateinit var subject: Subject

    private lateinit var aspect: AspectData

    private lateinit var aspectInt: AspectData

    private lateinit var complexAspect: AspectData

    private lateinit var aspectRef: AspectData

    private val username = "admin"

    @BeforeEach
    fun initTestData() {
        validator = TrimmingObjectValidator(MainObjectValidator(objectService, subjectService, measureService, refBookService, dao, aspectDao))
        subject = subjectService.createSubject(SubjectData(name = randomName(), description = "descr"), username)
        aspect = aspectService.save(AspectData(name = randomName(), description = "aspectDescr", baseType = BaseType.Text.name), username)
        aspectInt = aspectService.save(AspectData(name = randomName(), description = "aspectDescr", baseType = BaseType.Integer.name), username)
        aspectRef = aspectService.save(AspectData(name = randomName(), description = "aspectDescr", baseType = BaseType.Reference.name), username)

        val property = AspectPropertyData("", "p", aspect.idStrict(), aspect.guidSoft(), PropertyCardinality.INFINITY.name, null)
        val complexAspectData = AspectData(
            id = "",
            name = randomName(),
            measure = Kilometre.name,
            domain = null,
            baseType = BaseType.Decimal.name,
            properties = listOf(property)
        )
        complexAspect = aspectService.save(complexAspectData, username)
    }


    @Test
    fun `Object validator success`() {
        val request = ObjectCreateRequest("objectValidatorTestName", "object descr", subject.id)

        val createInfo = validator.checkedForCreation(request)

        assertEquals(request.name, createInfo.name, "names must be equal")
        assertEquals(request.description, createInfo.description, "descriptions must be equal")
        assertEquals(subject.id, createInfo.subject.id, "vertex's subject must point to subject")
        assertEquals(ORecordId(subject.id), createInfo.subject.identity, "vertex's subject must point to subject")
    }


    @Test
    fun `Object validator when subject is missing`() {
        val request = ObjectCreateRequest(
            "objectValidatorAbsentSubjectTestName",
            "object descr",
            createNonExistentSubjectKey()
        )

        try {
            validator.checkedForCreation(request)
            Assert.fail("Nothing thrown")
        } catch (e: SubjectNotFoundException) {
        } catch (e: Exception) {
            Assert.fail("Unexpected exception: $e")
        }

    }

    @Test
    fun `Object validator when object name is empty`() {
        val request = ObjectCreateRequest("", "object descr", subject.id)

        try {
            validator.checkedForCreation(request)
            Assert.fail("Nothing thrown")
        } catch (e: EmptyObjectCreateNameException) {
        } catch (e: Exception) {
            Assert.fail("Unexpected exception: $e")
        }
    }

    @Test
    fun `Object property validator success`() {
        val objectRequest =
            ObjectCreateRequest("objectPropertyValidatorTestName", "object descr", subject.id)
        val objectVertex = createObject(objectRequest)

        val propertyRequest = PropertyCreateRequest(
            name = "prop_objectPropertyValidatorTestName",
            description = null, objectId = objectVertex.id, aspectId = aspect.idStrict()
        )
        val propertyInfo = validator.checkedForCreation(propertyRequest)

        assertEquals(propertyRequest.name, propertyInfo.name, "names must be equal")
        assertEquals(propertyRequest.objectId, propertyInfo.objekt.id, "object id must keep the same")
        assertEquals(propertyRequest.aspectId, propertyInfo.aspect.id, "aspect id must keep the same")
    }

    @Test
    fun `Object property validator when object is missing`() {
        val objectRequest =
            ObjectCreateRequest(
                "objectPropertyAbsentObjectValidatorTestName",
                "object descr",
                subject.id
            )
        createObject(objectRequest)

        val propertyRequest = PropertyCreateRequest(
            name = "prop_objectPropertyValidatorTestName",
            description = null,
            objectId = createNonExistentObjectKey(),
            aspectId = aspect.idStrict()
        )

        try {
            validator.checkedForCreation(propertyRequest)
            fail("Nothing thrown")
        } catch (e: ObjectNotFoundException) {
        } catch (e: Throwable) {
            fail("Unexpected exception: $e")
        }
    }

    @Test
    fun `Object property validator when aspect is missing`() {
        val objectRequest =
            ObjectCreateRequest(
                "objectPropertyAbsentAspectValidatorTestName",
                "object descr",
                subject.id
            )
        val objectVertex = createObject(objectRequest)

        val propertyRequest = PropertyCreateRequest(
            name = "prop_objectPropertyValidatorTestName",
            description = null,
            objectId = objectVertex.id,
            aspectId = createNonExistentAspectKey()
        )

        try {
            validator.checkedForCreation(propertyRequest)
            fail("Nothing thrown")
        } catch (e: AspectDoesNotExist) {
        } catch (e: Throwable) {
            fail("Unexpected exception: $e")
        }
    }

    /*
    @Test
    fun objectValidatorEmptyObjectPropertyNameTest() {
        val objectRequest =
            ObjectCreateRequest(
                "objectValidatorEmptyObjectPropertyNameTestName",
                "object descr",
                subject.id,
                subject.version
            )
        val objectVertex = createObject(objectRequest)

        val propertyRequest = PropertyCreateRequest(
            name = "", cardinality = PropertyCardinality.INFINITY.name,
            objectId = objectVertex.id, aspectId = aspect.idStrict()
        )

        try {
            validator.checkedForCreation(propertyRequest)
            Assert.fail("Nothing thrown")
        } catch (e: EmptyObjectPropertyNameException) {
        } catch (e: Exception) {
            Assert.fail("Unexpected exception: $e")
        }
    }
    */

    @Test
    fun `Object value validator simple int success`() {
        val objectRequest =
            ObjectCreateRequest("objectValueValidatorTestSimpleIntName", "object descr", subject.id)
        val createdObject = createObject(objectRequest)

        val propertyRequest = PropertyCreateRequest(
            name = "prop_objectPropertyValidatorSimpleIntTestName",
            description = null, objectId = createdObject.id, aspectId = aspectInt.idStrict()
        )
        val savedProperty = createObjectProperty(propertyRequest)
        val scalarValue = ObjectValueData.IntegerValue(123, null)
        val valueRequest = ValueCreateRequest(value = scalarValue, description = null, objectPropertyId = savedProperty.id)

        transaction(db) {
            val objectValue = validator.checkedForCreation(valueRequest)

            assertEquals(scalarValue, objectValue.value.toObjectValueData(), "values must be equal")
            assertEquals(valueRequest.aspectPropertyId, objectValue.aspectProperty?.id, "root characteristics must be equal")
            assertEquals(valueRequest.objectPropertyId, objectValue.objectProperty.id, "root characteristics must be equal")
        }

    }

    @Test
    fun `Object value validator int with range success`() {
        val objectRequest = ObjectCreateRequest(
            "objectValueValidatorTestSimpleIntWithRangeName",
            "object descr", subject.id
        )
        val createdObject = createObject(objectRequest)

        val propertyRequest = PropertyCreateRequest(
            name = "prop_objectPropertyValidatorSimpleIntWithRangeTestName",
            description = null,
            objectId = createdObject.id, aspectId = aspectInt.idStrict()
        )
        val createdProperty = createObjectProperty(propertyRequest)

        val scalarValue = ObjectValueData.IntegerValue(123, null)
        val valueData = ValueCreateRequest(scalarValue, null, createdProperty.id)

        transaction(db) {
            val objectValue = validator.checkedForCreation(valueData)

            assertEquals(scalarValue, objectValue.value.toObjectValueData(), "scalar values must be equal")
            assertEquals(valueData.objectPropertyId, objectValue.objectProperty.id, "object properties must be equal")
        }
    }


    @Test
    fun `Object value validator simple string success`() {
        val objectRequest =
            ObjectCreateRequest("objectValueValidatorTestSimpleStrName", "object descr", subject.id)
        val createdObject = createObject(objectRequest)

        val propertyRequest = PropertyCreateRequest(
            name = "prop_objectPropertyValidatorSimpleStrTestName",
            description = null, objectId = createdObject.id, aspectId = aspect.idStrict()
        )
        val createdProperty = createObjectProperty(propertyRequest)

        val scalarValue = ObjectValueData.StringValue("string-value")
        val valueRequest = ValueCreateRequest(value = scalarValue, description = null, objectPropertyId = createdProperty.id)

        transaction(db) {
            val valueInfo = validator.checkedForCreation(valueRequest)

            assertEquals(scalarValue, valueInfo.value.toObjectValueData(), "values must be equal")
            assertEquals(valueRequest.aspectPropertyId, valueInfo.aspectProperty?.id, "aspect properties must be equal")
            assertEquals(valueRequest.objectPropertyId, valueInfo.objectProperty.id, "object properties must be equal")
        }
    }

    @Test
    fun `Object property validator creating second property success`() {
        val objectRequest =
            ObjectCreateRequest("objectSecondPropertyValidatorTestName", "object descr", subject.id)
        val objectVertex = createObject(objectRequest)

        val propertyRequest1 = PropertyCreateRequest(
            name = "1:prop_objectSecondPropertyValidatorTestName",
            description = null, objectId = objectVertex.id, aspectId = aspect.idStrict()
        )
        createObjectProperty(propertyRequest1)

        val propertyRequest2 = PropertyCreateRequest(
            name = "2:prop_objectSecondPropertyValidatorTestName",
            description = null, objectId = objectVertex.id, aspectId = complexAspect.idStrict()
        )

        transaction(db) {
            val propertyInfo = validator.checkedForCreation(propertyRequest2)

            assertEquals(propertyRequest2.name, propertyInfo.name, "names must be equal")
            assertEquals(propertyRequest2.objectId, propertyInfo.objekt.id, "object id must keep the same")
            assertEquals(propertyRequest2.aspectId, propertyInfo.aspect.id, "aspect id must keep the same")
        }
    }

    @Test
    @Suppress("EmptyCatchBlock")
    fun `Object value with reference to another reference value`() {
        val objectChange = ObjectBuilder(objectService)
            .name(randomName())
            .description("object descr")
            .subject(subject)
            .build()

        val propertyRequest1 = PropertyCreateRequest(name = randomName("1"), description = null, objectId = objectChange.id, aspectId = aspect.idStrict())
        val property1 = createObjectProperty(propertyRequest1)

        val propertyRequest2 = PropertyCreateRequest(name = randomName("2"), description = null, objectId = objectChange.id, aspectId = aspectRef.idStrict())
        val property2 = createObjectProperty(propertyRequest2)

        val scalarValue = ObjectValueData.StringValue("string-value")
        val scalarValueRequest = ValueCreateRequest(value = scalarValue, description = null, objectPropertyId = property1.id)
        val scalarValueVertex = createObjectValue(scalarValueRequest)

        val refValue = ObjectValueData.Link(LinkValueData.ObjectValue(scalarValueVertex.id))
        val refValueRequest = ValueCreateRequest(value = refValue, description = null, objectPropertyId = property2.id)
        val refValueVertex = createObjectValue(refValueRequest)

        val refValue2 = ObjectValueData.Link(LinkValueData.ObjectValue(refValueVertex.id))
        val refValueRequest2 = ValueCreateRequest(value = refValue2, description = null, objectPropertyId = property2.id)

        transaction(db) {
            try {
                validator.checkedForCreation(refValueRequest2)
                fail("Nothing thrown")
            } catch (e: IllegalStateException) {
            }
        }
    }


    private fun createObject(info: ObjectWriteInfo): ObjectVertex = transaction(db) {
        val newVertex = dao.newObjectVertex()
        return@transaction dao.saveObject(newVertex, info, emptyList())
    }

    private fun createObject(request: ObjectCreateRequest): ObjectVertex {
        val info = validator.checkedForCreation(request)
        return createObject(info)
    }

    private fun createObjectProperty(propertyWriteInfo: PropertyWriteInfo): ObjectPropertyVertex = transaction(db) {
        val newPropertyVertex = dao.newObjectPropertyVertex()
        return@transaction dao.saveObjectProperty(newPropertyVertex, propertyWriteInfo, emptyList())
    }

    private fun createObjectProperty(propertyRequest: PropertyCreateRequest): ObjectPropertyVertex {
        val info: PropertyWriteInfo = validator.checkedForCreation(propertyRequest)
        return createObjectProperty(info)
    }

    private fun createObjectValue(valueWriteInfo: ValueWriteInfo): ObjectPropertyValueVertex = transaction(db) {
        val newValueVertex = dao.newObjectValueVertex()
        return@transaction dao.saveObjectValue(newValueVertex, valueWriteInfo)
    }

    private fun createObjectValue(valueRequest: ValueCreateRequest): ObjectPropertyValueVertex {
        val info: ValueWriteInfo = transaction(db) { validator.checkedForCreation(valueRequest) }
        return createObjectValue(info)
    }

    private fun createNonExistentKey(type: String): String {
        val vertex = transaction(db) {
            val newVertex = db.createNewVertex(type)
            newVertex.setProperty("name", "non-existent")
            return@transaction newVertex.save<OVertexDocument>()
        }

        val id = vertex.id

        session(db) {
            db.delete(vertex)
        }

        return id
    }

    private fun createNonExistentSubjectKey(): String = createNonExistentKey(SUBJECT_CLASS)
    private fun createNonExistentObjectKey(): String = createNonExistentKey(OBJECT_CLASS)
    private fun createNonExistentAspectKey(): String = createNonExistentKey(ASPECT_CLASS)
}