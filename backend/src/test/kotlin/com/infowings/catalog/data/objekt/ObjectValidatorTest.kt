package com.infowings.catalog.data.objekt


import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.*
import com.infowings.catalog.data.MeasureService
import com.infowings.catalog.data.Subject
import com.infowings.catalog.data.SubjectNotFoundException
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.Aspect
import com.infowings.catalog.data.aspect.AspectDoesNotExist
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.record.impl.OVertexDocument
import junit.framework.Assert.assertTrue
import junit.framework.Assert.fail
import kotlinx.serialization.Serializable
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
    private lateinit var aspectService: AspectService
    @Autowired
    private lateinit var measureService: MeasureService
    @Autowired
    private lateinit var objectService: ObjectService

    private lateinit var validator: ObjectValidator

    private lateinit var subject: Subject

    private lateinit var aspect: Aspect

    private lateinit var complexAspect: Aspect

    private val username = "admin"

    @Before
    fun initTestData() {
        validator = ObjectValidator(objectService, subjectService, measureService, aspectService)
        subject = subjectService.createSubject(SubjectData(name = "subjectName", description = "descr"), username)
        aspect = aspectService.save(AspectData(name = "aspectName", description = "aspectDescr", baseType = BaseType.Text.name), username)
        val property = AspectPropertyData("", "p", aspect.id, PropertyCardinality.INFINITY.name)
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
        val data = ObjectData(null, "objectValidatorTestName", "object descr", subject.id, emptyList())

        val vertex = validator.checkedForCreation(data)

        assertEquals(data.name, vertex.name, "names must be equal")
        assertEquals(data.description, vertex.description, "descriptions must be equal")
        assertEquals(subject.id, vertex.subject.id, "vertex's subject must point to subject")
        assertEquals(ORecordId(subject.id), vertex.subject.identity, "vertex's subject must point to subject")
        assertEquals(emptyList(), vertex.properties, "vertex's properties must be empty")
    }

    @Test
    fun objectValidatorNonNullIdTest() {
        val data = ObjectData("id", "objectValidatorNullIdTestName", "object descr", subject.id, emptyList())

        try {
            validator.checkedForCreation(data)
            fail("Nothing thrown")
        } catch (e: IllegalStateException) {

        } catch (e: Throwable) {
            fail("Unexpected exception: $e")
        }
    }

    @Test
    fun objectValidatorAbsentSubjectTest() {
        val data = ObjectData(null, "objectValidatorAbsentSubjectTestName", "object descr",
            createNonExistentSubjectKey(), emptyList())

        try {
            validator.checkedForCreation(data)
            Assert.fail("Nothing thrown")
        } catch (e: SubjectNotFoundException) {
        } catch (e: Exception) {
            Assert.fail("Unexpected exception: $e")
        }
    }

    @Test
    fun objectValidatorEmptyObjectNameTest() {
        val data = ObjectData(null, "", "object descr", subject.id, emptyList())

        try {
            validator.checkedForCreation(data)
            Assert.fail("Nothing thrown")
        } catch (e: EmptyObjectNameException) {
        } catch (e: Exception) {
            Assert.fail("Unexpected exception: $e")
        }
    }

    @Test
    fun objectPropertyValidatorTest() {
        val data = ObjectData(null, "objectPropertyValidatorTestName", "object descr", subject.id, emptyList())
        val objectVertex = createObject(data)

        val objectPropertyData = ObjectPropertyData(null, name = "prop_objectPropertyValidatorTestName",
            cardinality = PropertyCardinality.INFINITY, objectId = objectVertex.id, aspectId = aspect.id,
            valueIds = emptyList())

        val objectProperty = validator.checkedForCreation(objectPropertyData)

        assertTrue(objectProperty.id == null)
        assertEquals(0, objectProperty.values.size, "values must be empty")
        assertEquals(objectPropertyData.name, objectProperty.name, "names must be equal")
        assertEquals(objectPropertyData.cardinality, objectProperty.cardinality, "cardinalities must be equal")
        assertEquals(objectPropertyData.objectId, objectProperty.objekt.id, "object id must keep the same")
        assertEquals(objectPropertyData.aspectId, objectProperty.aspect.id, "aspect id must keep the same")
    }

    @Test
    fun objectPropertyNonNullPropertyIdValidatorTest() {
        val data = ObjectData(null, "objectPropertyNonNullPropertyIdValidatorTestName", "object descr", subject.id, emptyList())
        val objectVertex = createObject(data)

        val objectPropertyData = ObjectPropertyData("someId", name = "prop_objectPropertyValidatorTestName",
            cardinality = PropertyCardinality.INFINITY, objectId = objectVertex.id, aspectId = aspect.id,
            valueIds = emptyList())

        try {
            validator.checkedForCreation(objectPropertyData)
            fail("Nothing thrown")
        } catch (e: IllegalStateException) {
        } catch (e: Throwable) {
            fail("Unexpected exception: $e")
        }
    }

    @Test
    fun objectPropertyAbsentObjectValidatorTest() {
        val data = ObjectData(null, "objectPropertyAbsentObjectValidatorTestName", "object descr", subject.id, emptyList())
        val objectVertex = createObject(data)

        val objectPropertyData = ObjectPropertyData(null, name = "prop_objectPropertyValidatorTestName",
            cardinality = PropertyCardinality.INFINITY, objectId = createNonExistentObjectKey(), aspectId = aspect.id,
            valueIds = emptyList())

        try {
            validator.checkedForCreation(objectPropertyData)
            fail("Nothing thrown")
        } catch (e: ObjectNotFoundException) {
        } catch (e: Throwable) {
            fail("Unexpected exception: $e")
        }
    }

    @Test
    fun objectPropertyAbsentAspectValidatorTest() {
        val data = ObjectData(null, "objectPropertyAbsentAspectValidatorTestName", "object descr", subject.id, emptyList())
        val objectVertex = createObject(data)

        val objectPropertyData = ObjectPropertyData(null, name = "prop_objectPropertyValidatorTestName",
            cardinality = PropertyCardinality.INFINITY, objectId = objectVertex.id, aspectId = createNonExistentAspectKey(),
            valueIds = emptyList())

        try {
            validator.checkedForCreation(objectPropertyData)
            fail("Nothing thrown")
        } catch (e: AspectDoesNotExist) {
        } catch (e: Throwable) {
            fail("Unexpected exception: $e")
        }
    }

    @Test
    fun objectValidatorEmptyObjectPropertyNameTest() {
        val data =
            ObjectData(null, "objectValidatorEmptyObjectPropertyNameTestName", "object descr", subject.id, emptyList())
        val objectVertex = createObject(data)

        val objectPropertyData = ObjectPropertyData(null, name = "", cardinality = PropertyCardinality.INFINITY,
            objectId = objectVertex.id, aspectId = aspect.id, valueIds = emptyList())

        try {
            validator.checkedForCreation(objectPropertyData)
            Assert.fail("Nothing thrown")
        } catch (e: EmptyObjectPropertyNameException) {
        } catch (e: Exception) {
            Assert.fail("Unexpected exception: $e")
        }
    }

    @Test
    fun objectValueValidatorSimpleIntTest() {
        val data = ObjectData(null, "objectValueValidatorTestSimpleIntName", "object descr", subject.id, emptyList())
        val savedObject = createObject(data)

        val objectPropertyData = ObjectPropertyData(null, name = "prop_objectPropertyValidatorSimpleIntTestName",
            cardinality = PropertyCardinality.INFINITY, objectId = savedObject.id, aspectId = aspect.id,
            valueIds = emptyList())
        val savedProperty  = createObjectProperty(objectPropertyData)

        val valueData = ObjectPropertyValueData(null, ScalarValue.IntegerValue(123, "size"), null, null,
            savedProperty.id, complexAspect.id, null)
        val objectValue = validator.checkedForCreation(valueData)


        assertEquals(valueData.scalarValue, objectValue.scalarValue, "scalar values must be equal")
        assertEquals(valueData.range, objectValue.range, "ranges must be equal")
        assertEquals(valueData.precision, objectValue.precision, "precisions must be equal")
        assertEquals(valueData.rootCharacteristicId, objectValue.rootCharacteristic.id, "root characteristics must be equal")
        assertEquals(valueData.objectPropertyId, objectValue.objectProperty.id, "root characteristics must be equal")
    }


    @Test
    fun objectValueValidatorSimpleIntWithRangeTest() {
        val data = ObjectData(null, "objectValueValidatorTestSimpleIntWithRangeName",
            "object descr", subject.id, emptyList())
        val savedObject = createObject(data)

        val objectPropertyData = ObjectPropertyData(null,
            name = "prop_objectPropertyValidatorSimpleIntWithRangeTestName",
            cardinality = PropertyCardinality.INFINITY, objectId = savedObject.id, aspectId = aspect.id,
            valueIds = emptyList())
        val savedProperty  = createObjectProperty(objectPropertyData)

        val valueData = ObjectPropertyValueData(null, ScalarValue.IntegerValue(123, "size"), Range(5, 10), null,
            savedProperty.id, complexAspect.id, null)
        val objectValue = validator.checkedForCreation(valueData)

        assertEquals(valueData.scalarValue, objectValue.scalarValue, "scalar values must be equal")
        assertEquals(valueData.range, objectValue.range, "ranges must be equal")
        assertEquals(valueData.precision, objectValue.precision, "precisions must be equal")
        assertEquals(valueData.rootCharacteristicId, objectValue.rootCharacteristic.id, "root characteristics must be equal")
        assertEquals(valueData.objectPropertyId, objectValue.objectProperty.id, "root characteristics must be equal")
    }


    @Test
    fun objectValueValidatorSimpleStrTest() {
        val data = ObjectData(null, "objectValueValidatorTestSimpleStrName", "object descr", subject.id, emptyList())
        val savedObject = createObject(data)

        val objectPropertyData = ObjectPropertyData(null, name = "prop_objectPropertyValidatorSimpleStrTestName",
            cardinality = PropertyCardinality.INFINITY, objectId = savedObject.id, aspectId = aspect.id,
            valueIds = emptyList())
        val savedProperty  = createObjectProperty(objectPropertyData)

        val valueData = ObjectPropertyValueData(null, ScalarValue.StringValue("string-value", "string-type"), null, null,
            savedProperty.id, complexAspect.id, null)
        val objectValue = validator.checkedForCreation(valueData)

        assertEquals(valueData.scalarValue, objectValue.scalarValue, "scalar values must be equal")
        assertEquals(valueData.range, objectValue.range, "ranges must be equal")
        assertEquals(valueData.precision, objectValue.precision, "precisions must be equal")
        assertEquals(valueData.rootCharacteristicId, objectValue.rootCharacteristic.id, "root characteristics must be equal")
        assertEquals(valueData.objectPropertyId, objectValue.objectProperty.id, "root characteristics must be equal")
    }

    @Serializable
    data class CompoundSample(val intData: Int, val strData: String)

    @Test
    fun objectValueValidatorSimpleCompoundTest() {
        val data = ObjectData(null, "objectValueValidatorTestSimpleCompoundName", "object descr", subject.id, emptyList())
        val savedObject = createObject(data)

        val objectPropertyData = ObjectPropertyData(null, name = "prop_objectPropertyValidatorSimpleCompoundTestName",
            cardinality = PropertyCardinality.INFINITY, objectId = savedObject.id, aspectId = aspect.id,
            valueIds = emptyList())
        val savedProperty  = createObjectProperty(objectPropertyData)

        val valueData = ObjectPropertyValueData(null, ScalarValue.CompoundValue(CompoundSample(5, "str"),
            "compound-type"), null, null, savedProperty.id, complexAspect.id, null)
        val objectValue = validator.checkedForCreation(valueData)


        assertEquals(valueData.scalarValue, objectValue.scalarValue, "scalar values must be equal")
        assertEquals(valueData.range, objectValue.range, "ranges must be equal")
        assertEquals(valueData.precision, objectValue.precision, "precisions must be equal")
        assertEquals(valueData.rootCharacteristicId, objectValue.rootCharacteristic.id, "root characteristics must be equal")
        assertEquals(valueData.objectPropertyId, objectValue.objectProperty.id, "root characteristics must be equal")
    }

    @Test
    fun objectSecondPropertyValidatorTest() {
        val data = ObjectData(null, "objectSecondPropertyValidatorTestName", "object descr", subject.id, emptyList())
        val objectVertex = createObject(data)

        val objectPropertyData1 = ObjectPropertyData(null, name = "1:prop_objectSecondPropertyValidatorTestName",
            cardinality = PropertyCardinality.INFINITY, objectId = objectVertex.id, aspectId = aspect.id,
            valueIds = emptyList())
        val savedProperty  = createObjectProperty(objectPropertyData1)

        val objectPropertyData2 = ObjectPropertyData(null, name = "2:prop_objectSecondPropertyValidatorTestName",
            cardinality = PropertyCardinality.ONE, objectId = objectVertex.id, aspectId = complexAspect.id,
            valueIds = emptyList())

        val objectProperty2 = validator.checkedForCreation(objectPropertyData2)

        assertTrue(objectProperty2.id == null)
        assertEquals(0, objectProperty2.values.size, "values must be empty")
        assertEquals(objectPropertyData2.name, objectProperty2.name, "names must be equal")
        assertEquals(objectPropertyData2.cardinality, objectProperty2.cardinality, "cardinalities must be equal")
        assertEquals(objectPropertyData2.objectId, objectProperty2.objekt.id, "object id must keep the same")
        assertEquals(objectPropertyData2.aspectId, objectProperty2.aspect.id, "aspect id must keep the same")
    }

    private fun createObject(objekt: Objekt): ObjectVertex = transaction(db) {
        val newVertex = dao.newObjectVertex()
        return@transaction dao.saveObject(newVertex, objekt)
    }

    private fun createObject(objectData: ObjectData): ObjectVertex {
        val objekt = validator.checkedForCreation(objectData)
        return createObject(objekt)
    }

    private fun createObjectProperty(objectProperty: ObjectProperty): ObjectPropertyVertex = transaction(db) {
        val newPropertyVertex = dao.newObjectPropertyVertex()
        return@transaction dao.saveObjectProperty(newPropertyVertex, objectProperty)
    }

    private fun createObjectProperty(objectPropertyData: ObjectPropertyData): ObjectPropertyVertex {
        val data = validator.checkedForCreation(objectPropertyData)
        return createObjectProperty(data)
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