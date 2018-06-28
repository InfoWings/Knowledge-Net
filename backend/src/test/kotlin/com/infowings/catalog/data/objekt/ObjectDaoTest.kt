package com.infowings.catalog.data.objekt

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.ObjectCreateRequest
import com.infowings.catalog.common.objekt.PropertyCreateRequest
import com.infowings.catalog.common.objekt.ValueCreateRequest
import com.infowings.catalog.data.MeasureService
import com.infowings.catalog.data.Subject
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectDaoService
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.reference.book.ReferenceBookService
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.id
import com.infowings.catalog.storage.session
import com.infowings.catalog.storage.transaction
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail


@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ObjectDaoTest {
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
    fun saveTest() {
        val data = ObjectCreateRequest("saveTestName", "object descr", subject.id, subject.version)
        val objectCreateInfo = validator.checkedForCreation(data)
        val saved = createObject(objectCreateInfo)

        assertEquals(data.name, saved.name, "names must be equal")
        assertEquals(data.description, saved.description, "descriptions must be equal")
        assertTrue("cluster id must be non-negative: ${saved.identity}", saved.identity.clusterId >= 0)
        assertTrue("cluster position must be non-negative: ${saved.identity}", saved.identity.clusterPosition >= 0)
    }

    @Test
    fun saveTwiceTest() {
        val request1 = ObjectCreateRequest("saveTwiceName-1", "object descr-1", subject.id, subject.version)
        val request2 = ObjectCreateRequest("saveTwiceName-2", "object descr-2", subject.id, subject.version)

        val objekt1 = validator.checkedForCreation(request1)
        val saved1 = createObject(objekt1)

        val objekt2 = validator.checkedForCreation(request2)
        val saved2 = createObject(objekt2)

        assert(saved1.id != saved2.id)
    }

    @Test
    fun savePropertyTest() {
        val objectRequest = ObjectCreateRequest("savePropertyTestObjectName", "some descr", subject.id, subject.version)
        val createdObject = createObject(objectRequest)
        val propertyData = PropertyCreateRequest(
            name = "savePropertyTestObjectPropertyName",
            cardinality = PropertyCardinality.ONE.name,
            objectId = createdObject.id,
            aspectId = aspect.idStrict()
        )


        val propertyCreateInfo = validator.checkedForCreation(propertyData)
        val createdProperty = createObjectProperty(propertyCreateInfo)

        transaction(db) {
            val objectOfProperty = createdProperty.objekt
            if (objectOfProperty == null) {
                fail("no reference to object from property")
            } else {
                val foundObject = objectService.findById(objectOfProperty.id)

                assertEquals(propertyCreateInfo.name, createdProperty.name, "names must be equal")
                assertEquals(createdObject.id, createdProperty.objekt?.id, "objekt must point to parent object")
                assertEquals(1, objectOfProperty.properties.size, "updated object must contain one property")
                assertEquals(1, foundObject.properties.size, "found object must contain one property")


                assertEquals(
                    objectOfProperty.properties.first().id,
                    createdProperty.id,
                    "updated object's property must link to new one"
                )
                assertEquals(
                    foundObject.properties.first().id,
                    createdProperty.id,
                    "found object's property must link to new one"
                )
            }
        }
    }

    @Test
    fun savePropertySimpleIntValueTest() {
        val objectRequest =
            ObjectCreateRequest("savePropertySimpleIntValueTest", "some descr", subject.id, subject.version)
        val createdObject = createObject(objectRequest)
        val propertyRequest = PropertyCreateRequest(
            objectId = createdObject.id,
            name = "savePropertySimpleIntValueTest",
            cardinality = PropertyCardinality.ONE.name,
            aspectId = aspect.idStrict()
        )

        val propertyInfo = validator.checkedForCreation(propertyRequest)
        val createdProperty = createObjectProperty(propertyInfo)

        val valueRequest = ValueCreateRequest(
            value = ObjectValueData.IntegerValue(123, null),
            objectPropertyId = createdProperty.id,
            aspectPropertyId = complexAspect.properties[0].id,
            measureId = null,
            parentValueId = null
        )
        val valueInfo = validator.checkedForCreation(valueRequest)
        val createdValue = createObjectPropertyValue(valueInfo)

        assertEquals(ScalarTypeTag.INTEGER, createdValue.typeTag, "type tag must be integer")
        assertNotNull(createdValue.intValue, "int value must be non-null")
        assertTrue("str type must be null", createdValue.strValue == null)
        assertEquals(123, createdValue.intValue, "int value must be 123")


        val propertyOfValue = transaction(db) { createdValue.objectProperty }
        val foundObjectProperty = objectService.findPropertyById(createdProperty.id)

        if (propertyOfValue != null) {
            transaction(db) {
                assertEquals(
                    propertyOfValue.id,
                    createdValue.objectProperty?.id,
                    "object property id must point to parent"
                )
                assertEquals(
                    foundObjectProperty.id,
                    createdValue.objectProperty?.id,
                    "object property id must point to found property"
                )

                assertEquals(1, propertyOfValue.values.size, "property must contain 1 value")
                assertEquals(1, foundObjectProperty.values.size, "property must contain 1 value")
            }
        } else {
            fail("object property is null")
        }
    }

    @Test
    fun savePropertySimpleStrValueTest() {
        val objectRequest =
            ObjectCreateRequest("savePropertySimpleStrValueTest", "some descr", subject.id, subject.version)
        val createdObject = createObject(objectRequest)
        val propertyRequest = PropertyCreateRequest(
            objectId = createdObject.id,
            name = "savePropertySimpleStrValueTest",
            cardinality = PropertyCardinality.ONE.name,
            aspectId = aspect.idStrict()
        )

        val propertyInfo = validator.checkedForCreation(propertyRequest)
        val createdProperty = createObjectProperty(propertyInfo)

        val valueRequest = ValueCreateRequest(
            value = ObjectValueData.StringValue("some value"),
            objectPropertyId = createdProperty.id,
            aspectPropertyId = complexAspect.id,
            measureId = null,
            parentValueId = null
        )
        val valueInfo = validator.checkedForCreation(valueRequest)
        val createdValue = createObjectPropertyValue(valueInfo)

        assertEquals(ScalarTypeTag.STRING, createdValue.typeTag, "type tag must be string")
        assertNotNull(createdValue.strValue, "str value must be non-null")
        assertTrue("int value must be null", createdValue.intValue == null)
        assertEquals("some value", createdValue.strValue, "str value must be correct")
    }

    @Test(expected = ObjectAlreadyExists::class)
    fun checkSaveObjectSameNameSameSubjectTest() {
        val objectRequest =
            ObjectCreateRequest("obj", "some descr", subject.id, subject.version)
        objectService.create(objectRequest, username)
        objectService.create(objectRequest, username)
    }

    @Test
    fun checkSaveObjectSameNameDiffSubjectTest() {
        val objectRequest1 = ObjectCreateRequest("obj", "some descr", subject.id, subject.version)
        val objId1 = objectService.create(objectRequest1, username)
        val obj1 = objectService.findById(objId1)
        val subj1 = session(db) { obj1.subject!! }

        val subject2 = subjectService.createSubject(SubjectData(name = "sub2", description = null), username)
        val objectRequest2 = ObjectCreateRequest("obj", "some descr", subject2.id, subject2.version)
        val objId2 = objectService.create(objectRequest2, username)
        val obj2 = objectService.findById(objId2)
        val subj2 = session(db) { obj2.subject!! }

        assertTrue("There are two objects with same name", obj1.name == obj2.name)
        assertTrue("Objects have different subjects", subj1.id != subj2.id)
    }

    @Test(expected = ObjectPropertyAlreadyExistException::class)
    fun checkSavePropertySameNameSameAspectTest() {
        val objVertex = createObject(ObjectCreateRequest("obj", "some descr", subject.id, subject.version))
        val propertyRequest = PropertyCreateRequest(
            objectId = objVertex.id,
            name = "prop",
            cardinality = PropertyCardinality.ONE.name,
            aspectId = aspect.idStrict()
        )
        objectService.create(propertyRequest, username)
        objectService.create(propertyRequest, username)
    }

    @Test
    fun checkSavePropertySameNameDiffAspectTest() {
        val objVertex = createObject(ObjectCreateRequest("obj", "some descr", subject.id, subject.version))
        val propertyRequest = PropertyCreateRequest(
            objectId = objVertex.id,
            name = "prop",
            cardinality = PropertyCardinality.ONE.name,
            aspectId = aspect.idStrict()
        )
        val objPropId1 = objectService.create(propertyRequest, username)
        val objProp1 = objectService.findPropertyById(objPropId1)

        val anotherAspect = aspectService.save(AspectData(name = "another aspect", baseType = BaseType.Text.name), username)
        val propertyRequest2 = PropertyCreateRequest(
            objectId = objVertex.id,
            name = "prop",
            cardinality = PropertyCardinality.ONE.name,
            aspectId = anotherAspect.idStrict()
        )
        val objPropId2 = objectService.create(propertyRequest2, username)
        val objProp2 = objectService.findPropertyById(objPropId2)

        assertTrue("There are two props with same name", objProp1.name == objProp2.name)
    }

    @Test
    fun checkSavePropertyDiffNamesSameAspectTest() {
        val objVertex = createObject(ObjectCreateRequest("obj", "some descr", subject.id, subject.version))
        val propertyRequest = PropertyCreateRequest(
            objectId = objVertex.id,
            name = "prop",
            cardinality = PropertyCardinality.ONE.name,
            aspectId = aspect.idStrict()
        )
        val objPropId1 = objectService.create(propertyRequest, username)
        val objProp1 = objectService.findPropertyById(objPropId1)
        val aspectId1 = session(db) { objProp1.aspect?.id }

        val propertyRequest2 = PropertyCreateRequest(
            objectId = objVertex.id,
            name = "prop2",
            cardinality = PropertyCardinality.ONE.name,
            aspectId = aspect.idStrict()
        )
        val objPropId2 = objectService.create(propertyRequest2, username)
        val objProp2 = objectService.findPropertyById(objPropId2)
        val aspectId2 = session(db) { objProp2.aspect?.id }

        assertTrue("There are two props with same aspectId", aspectId1 == aspectId2)
    }

    private fun createObject(objekt: ObjectWriteInfo): ObjectVertex = transaction(db) {
        val newVertex = dao.newObjectVertex()
        return@transaction dao.saveObject(newVertex, objekt, emptyList())
    }

    private fun createObject(request: ObjectCreateRequest): ObjectVertex {
        val objekt = validator.checkedForCreation(request)
        return createObject(objekt)
    }


    private fun createObjectProperty(property: PropertyWriteInfo): ObjectPropertyVertex = transaction(db) {
        val newVertex = dao.newObjectPropertyVertex()
        return@transaction dao.saveObjectProperty(newVertex, property, emptyList())
    }

    private fun createObjectPropertyValue(valueInfo: ValueWriteInfo): ObjectPropertyValueVertex = transaction(db) {
        val newVertex = dao.newObjectValueVertex()
        return@transaction dao.saveObjectValue(newVertex, valueInfo)
    }
}