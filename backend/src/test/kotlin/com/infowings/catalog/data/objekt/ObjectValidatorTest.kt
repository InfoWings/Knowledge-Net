package com.infowings.catalog.data.objekt

import com.infowings.catalog.MasterCatalog
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
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.record.impl.OVertexDocument
import junit.framework.Assert.fail
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import kotlin.test.assertEquals


@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
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

    private lateinit var complexAspect: AspectData

    private val username = "admin"

    @Before
    fun initTestData() {
        validator = ObjectValidator(objectService, subjectService, measureService, refBookService, aspectDao)
        subject = subjectService.createSubject(SubjectData(name = "subjectName", description = "descr"), username)
        aspect = aspectService.save(
            AspectData(
                name = "aspectName",
                description = "aspectDescr",
                baseType = BaseType.Text.name
            ), username
        )
        val property = AspectPropertyData("", "p", aspect.idStrict(), PropertyCardinality.INFINITY.name, null)
        val complexAspectData = AspectData(
            "",
            "complex",
            Kilometre.name,
            null,
            BaseType.Decimal.name,
            listOf(property)
        )
        complexAspect = aspectService.save(complexAspectData, username)
    }


    @Test
    fun objectValidatorTest() {
        val request = ObjectCreateRequest("objectValidatorTestName", "object descr", subject.id, subject.version)

        val createInfo = validator.checkedForCreation(request)

        assertEquals(request.name, createInfo.name, "names must be equal")
        assertEquals(request.description, createInfo.description, "descriptions must be equal")
        assertEquals(subject.id, createInfo.subject.id, "vertex's subject must point to subject")
        assertEquals(ORecordId(subject.id), createInfo.subject.identity, "vertex's subject must point to subject")
    }


    @Test
    fun objectValidatorAbsentSubjectTest() {
        val request = ObjectCreateRequest(
            "objectValidatorAbsentSubjectTestName",
            "object descr",
            createNonExistentSubjectKey(),
            null
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
    fun objectValidatorEmptyObjectNameTest() {
        val request = ObjectCreateRequest("", "object descr", subject.id, subject.version)

        try {
            validator.checkedForCreation(request)
            Assert.fail("Nothing thrown")
        } catch (e: EmptyObjectNameException) {
        } catch (e: Exception) {
            Assert.fail("Unexpected exception: $e")
        }
    }

    @Test
    fun objectPropertyValidatorTest() {
        val objectRequest =
            ObjectCreateRequest("objectPropertyValidatorTestName", "object descr", subject.id, subject.version)
        val objectVertex = createObject(objectRequest)

        val propertyRequest = PropertyCreateRequest(
            name = "prop_objectPropertyValidatorTestName",
            cardinality = PropertyCardinality.INFINITY.name, objectId = objectVertex.id, aspectId = aspect.idStrict()
        )
        val propertyInfo = validator.checkedForCreation(propertyRequest)

        assertEquals(propertyRequest.name, propertyInfo.name, "names must be equal")
        assertEquals(propertyRequest.cardinality, propertyInfo.cardinality.name, "cardinalities must be equal")
        assertEquals(propertyRequest.objectId, propertyInfo.objekt.id, "object id must keep the same")
        assertEquals(propertyRequest.aspectId, propertyInfo.aspect.id, "aspect id must keep the same")
    }

    @Test
    fun objectPropertyAbsentObjectValidatorTest() {
        val objectRequest =
            ObjectCreateRequest(
                "objectPropertyAbsentObjectValidatorTestName",
                "object descr",
                subject.id,
                subject.version
            )
        val objectVertex = createObject(objectRequest)

        val propertyRequest = PropertyCreateRequest(
            name = "prop_objectPropertyValidatorTestName",
            cardinality = PropertyCardinality.INFINITY.name,
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
    fun objectPropertyAbsentAspectValidatorTest() {
        val objectRequest =
            ObjectCreateRequest(
                "objectPropertyAbsentAspectValidatorTestName",
                "object descr",
                subject.id,
                subject.version
            )
        val objectVertex = createObject(objectRequest)

        val propertyRequest = PropertyCreateRequest(
            name = "prop_objectPropertyValidatorTestName",
            cardinality = PropertyCardinality.INFINITY.name,
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
    fun objectValueValidatorSimpleIntTest() {
        val objectRequest =
            ObjectCreateRequest("objectValueValidatorTestSimpleIntName", "object descr", subject.id, subject.version)
        val createdObject = createObject(objectRequest)

        val propertyRequest = PropertyCreateRequest(
            name = "prop_objectPropertyValidatorSimpleIntTestName",
            cardinality = PropertyCardinality.INFINITY.name, objectId = createdObject.id, aspectId = aspect.idStrict()
        )
        val savedProperty = createObjectProperty(propertyRequest)
        val scalarValue = ObjectValueData.IntegerValue(123, null)
        val valueRequest = ValueCreateRequest(
            value = scalarValue,
            objectPropertyId = savedProperty.id,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val objectValue = validator.checkedForCreation(valueRequest)

        assertEquals(scalarValue, objectValue.value.toObjectValueData(), "values must be equal")
        assertEquals(
            valueRequest.aspectPropertyId,
            objectValue.aspectProperty?.id,
            "root characteristics must be equal"
        )
        assertEquals(valueRequest.objectPropertyId, objectValue.objectProperty.id, "root characteristics must be equal")
    }


    @Test
    fun objectValueValidatorSimpleIntWithRangeTest() {
        val objectRequest = ObjectCreateRequest(
            "objectValueValidatorTestSimpleIntWithRangeName",
            "object descr", subject.id, subject.version
        )
        val createdObject = createObject(objectRequest)

        val propertyRequest = PropertyCreateRequest(
            name = "prop_objectPropertyValidatorSimpleIntWithRangeTestName",
            cardinality = PropertyCardinality.INFINITY.name,
            objectId = createdObject.id, aspectId = aspect.idStrict()
        )
        val createdProperty = createObjectProperty(propertyRequest)

        val scalarValue = ObjectValueData.IntegerValue(123, null)
        val valueData = ValueCreateRequest(
            scalarValue,
            createdProperty.id,
            complexAspect.properties[0].id,
            null,
            null
        )
        val objectValue = validator.checkedForCreation(valueData)

        assertEquals(scalarValue, objectValue.value.toObjectValueData(), "scalar values must be equal")
        assertEquals(valueData.aspectPropertyId, objectValue.aspectProperty?.id, "aspect properties must be equal")
        assertEquals(valueData.objectPropertyId, objectValue.objectProperty.id, "object properties must be equal")
    }


    @Test
    fun objectValueValidatorSimpleStrTest() {
        val objectRequest =
            ObjectCreateRequest("objectValueValidatorTestSimpleStrName", "object descr", subject.id, subject.version)
        val createdObject = createObject(objectRequest)

        val propertyRequest = PropertyCreateRequest(
            name = "prop_objectPropertyValidatorSimpleStrTestName",
            cardinality = PropertyCardinality.INFINITY.name, objectId = createdObject.id, aspectId = aspect.idStrict()
        )
        val createdProperty = createObjectProperty(propertyRequest)

        val scalarValue = ObjectValueData.StringValue("string-value")
        val valueRequest = ValueCreateRequest(
            value = scalarValue,
            objectPropertyId = createdProperty.id,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val valueInfo = validator.checkedForCreation(valueRequest)

        assertEquals(scalarValue, valueInfo.value.toObjectValueData(), "values must be equal")
        assertEquals(valueRequest.aspectPropertyId, valueInfo.aspectProperty?.id, "aspect properties must be equal")
        assertEquals(valueRequest.objectPropertyId, valueInfo.objectProperty.id, "object properties must be equal")
    }

    @Test
    fun objectSecondPropertyValidatorTest() {
        val objectRequest =
            ObjectCreateRequest("objectSecondPropertyValidatorTestName", "object descr", subject.id, subject.version)
        val objectVertex = createObject(objectRequest)

        val propertyRequest1 = PropertyCreateRequest(
            name = "1:prop_objectSecondPropertyValidatorTestName",
            cardinality = PropertyCardinality.INFINITY.name, objectId = objectVertex.id, aspectId = aspect.idStrict()
        )
        val propertyVertex = createObjectProperty(propertyRequest1)

        val propertyRequest2 = PropertyCreateRequest(
            name = "2:prop_objectSecondPropertyValidatorTestName",
            cardinality = PropertyCardinality.ONE.name, objectId = objectVertex.id, aspectId = complexAspect.idStrict()
        )

        val propertyInfo = validator.checkedForCreation(propertyRequest2)

        assertEquals(propertyRequest2.name, propertyInfo.name, "names must be equal")
        assertEquals(propertyRequest2.cardinality, propertyInfo.cardinality.name, "cardinalities must be equal")
        assertEquals(propertyRequest2.objectId, propertyInfo.objekt.id, "object id must keep the same")
        assertEquals(propertyRequest2.aspectId, propertyInfo.aspect.id, "aspect id must keep the same")
    }


    private fun createObject(info: ObjectCreateInfo): ObjectVertex = transaction(db) {
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