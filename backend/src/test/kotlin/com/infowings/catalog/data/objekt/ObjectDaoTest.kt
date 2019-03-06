package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.*
import com.infowings.catalog.data.MeasureService
import com.infowings.catalog.data.Subject
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectDaoService
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.reference.book.ReferenceBookService
import com.infowings.catalog.randomName
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.id.ORecordId
import org.junit.Assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

private const val INT_SAMPLE_VALUE = 123
private const val UPB_SAMPLE_VALUE = 130

@ExtendWith(SpringExtension::class)
@SpringBootTest
@Suppress("MagicNumber", "StringLiteralDuplication")
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
    private lateinit var intAspect: AspectData

    private lateinit var complexAspect: AspectData

    private val username = "admin"
    private val sampleDescription = "Some Description"

    @BeforeEach
    fun initTestData() {
        validator = MainObjectValidator(objectService, subjectService, measureService, refBookService, dao, aspectDao)
        subject = subjectService.createSubject(SubjectData(name = randomName(), description = "descr"), username)
        aspect = aspectService.save(
            AspectData(
                name = randomName(),
                description = "aspectDescr",
                baseType = BaseType.Text.name
            ), username
        )
        intAspect = aspectService.save(
            AspectData(
                name = randomName(),
                description = "aspectDescr",
                baseType = BaseType.Integer.name
            ), username
        )
        val property = AspectPropertyData("", "p", aspect.idStrict(), aspect.guidSoft(), PropertyCardinality.INFINITY.name, null)
        val complexAspectData = AspectData(
            "",
            randomName(),
            Kilometre.name,
            null,
            BaseType.Decimal.name,
            listOf(property)
        )
        complexAspect = aspectService.save(complexAspectData, username)
    }

    @Test
    fun saveTest() {
        val data = ObjectCreateRequest(randomName(), sampleDescription, subject.id)
        val objectCreateInfo = validator.checkedForCreation(data)
        val saved = createObject(objectCreateInfo)

        assertEquals(data.name, saved.name, "names must be equal")
        assertEquals(data.description, saved.description, "descriptions must be equal")
        assertTrue(saved.identity.clusterId >= 0, "cluster id must be non-negative: ${saved.identity}")
        assertTrue(saved.identity.clusterPosition >= 0, "cluster position must be non-negative: ${saved.identity}")
    }

    @Test
    fun saveTwiceTest() {
        val request1 = ObjectCreateRequest("saveTwiceName-1", "object descr-1", subject.id)
        val request2 = ObjectCreateRequest("saveTwiceName-2", "object descr-2", subject.id)

        val objekt1 = validator.checkedForCreation(request1)
        val saved1 = createObject(objekt1)

        val objekt2 = validator.checkedForCreation(request2)
        val saved2 = createObject(objekt2)

        assert(saved1.id != saved2.id)
    }

    private fun newObject(subjectId: String) = createObject(ObjectCreateRequest(randomName(), sampleDescription, subjectId))

    private fun newObject() = newObject(subject.id)

    @Test
    fun savePropertyTest() {
        val createdObject = newObject()
        val propertyData = PropertyCreateRequest(
            name = "savePropertyTestObjectPropertyName",
            description = null,
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
        val createdObject = newObject()
        val propertyRequest = PropertyCreateRequest(
            objectId = createdObject.id,
            name = randomName(),
            description = null,
            aspectId = intAspect.idStrict()
        )

        val propertyInfo = validator.checkedForCreation(propertyRequest)
        val createdProperty = createObjectProperty(propertyInfo)

        val valueRequest = ValueCreateRequest(
            value = ObjectValueData.IntegerValue(INT_SAMPLE_VALUE, null),
            description = null,
            objectPropertyId = createdProperty.id
        )

        val createdValue = transaction(db) {
            val valueInfo = validator.checkedForCreation(valueRequest)
            val result = createObjectPropertyValue(valueInfo)

            assertEquals(ScalarTypeTag.INTEGER, result.typeTag, "type tag must be integer")
            assertNotNull(result.intValue, "int value must be non-null")
            assertTrue(result.strValue == null, "str type must be null")
            assertEquals(INT_SAMPLE_VALUE, result.intValue)
            assertEquals(null, result.intUpb)
            result
        }


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
    fun savePropertyIntRangeValueTest() {
        val createdObject = newObject()
        val propertyRequest = PropertyCreateRequest(
            objectId = createdObject.id, name = "savePropertySimpleIntValueTest",
            description = null,
            aspectId = intAspect.idStrict()
        )

        val propertyInfo = validator.checkedForCreation(propertyRequest)
        val createdProperty = createObjectProperty(propertyInfo)

        val valueRequest = ValueCreateRequest(
            value = ObjectValueData.IntegerValue(INT_SAMPLE_VALUE, UPB_SAMPLE_VALUE, null),
            description = null,
            objectPropertyId = createdProperty.id
        )

        val createdValue = transaction(db) {
            val valueInfo = validator.checkedForCreation(valueRequest)
            val result = createObjectPropertyValue(valueInfo)

            assertEquals(ScalarTypeTag.INTEGER, result.typeTag, "type tag must be integer")
            assertNotNull(result.intValue, "int value must be non-null")
            assertTrue(result.strValue == null, "str type must be null")
            assertEquals(INT_SAMPLE_VALUE, result.intValue)
            assertEquals(UPB_SAMPLE_VALUE, result.intUpb)
            result
        }


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
        val createdObject = newObject()
        val propertyRequest = PropertyCreateRequest(
            objectId = createdObject.id,
            name = "savePropertySimpleStrValueTest",
            description = null,
            aspectId = aspect.idStrict()
        )

        val propertyInfo = validator.checkedForCreation(propertyRequest)
        val createdProperty = createObjectProperty(propertyInfo)

        val valueRequest = ValueCreateRequest(ObjectValueData.StringValue("some value"), null, createdProperty.id)

        transaction(db) {
            val valueInfo = validator.checkedForCreation(valueRequest)
            val createdValue = createObjectPropertyValue(valueInfo)

            assertEquals(ScalarTypeTag.STRING, createdValue.typeTag, "type tag must be string")
            assertNotNull(createdValue.strValue, "str value must be non-null")
            assertTrue(createdValue.intValue == null, "int value must be null")
            assertTrue(createdValue.intUpb == null, "int value must be null")
            assertEquals("some value", createdValue.strValue, "str value must be correct")
        }
    }

    @Test
    fun checkSaveObjectSameNameSameSubjectTest() {
        val objectRequest = ObjectCreateRequest(randomName("obj"), sampleDescription, subject.id)
        objectService.create(objectRequest, username)
        assertThrows<ObjectAlreadyExists> {
            objectService.create(objectRequest, username)
        }
    }

    @Test
    fun checkSaveObjectSameNameDiffSubjectTest() {
        val objectName = randomName("obj")
        val objectRequest1 = ObjectCreateRequest(objectName, sampleDescription, subject.id)
        val objResp1 = objectService.create(objectRequest1, username)
        val obj1 = objectService.findById(objResp1.id)
        val subj1 = session(db) { obj1.subject!! }

        val subject2 = subjectService.createSubject(SubjectData(name = randomName("sub2"), description = null), username)
        val objectRequest2 = ObjectCreateRequest(objectName, sampleDescription, subject2.id)
        val objResp2 = objectService.create(objectRequest2, username)
        val obj2 = objectService.findById(objResp2.id)
        val subj2 = session(db) { obj2.subject!! }

        assertTrue(obj1.name == obj2.name, "There are two objects with same name")
        assertTrue(subj1.id != subj2.id, "Objects have different subjects")
    }

    @Test
    fun checkSavePropertySameNameSameAspectTest() {
        val objVertex = newObject()
        val propertyRequest = PropertyCreateRequest(
            objectId = objVertex.id,
            name = "prop",
            description = null,
            aspectId = aspect.idStrict()
        )
        objectService.create(propertyRequest, username)
        assertThrows<ObjectPropertyAlreadyExistException> {
            objectService.create(propertyRequest, username)
        }
    }

    @Test
    fun checkSavePropertySameNameDiffAspectTest() {
        val objVertex = newObject()
        val propertyRequest = PropertyCreateRequest(
            objectId = objVertex.id,
            name = "prop",
            description = null,
            aspectId = aspect.idStrict()
        )
        val objPropResp1 = objectService.create(propertyRequest, username)
        val objProp1 = objectService.findPropertyById(objPropResp1.id)

        val anotherAspect = aspectService.save(AspectData(name = "another aspect", baseType = BaseType.Text.name), username)
        val propertyRequest2 = PropertyCreateRequest(
            objectId = objVertex.id,
            name = "prop",
            description = null,
            aspectId = anotherAspect.idStrict()
        )
        val objPropResp2 = objectService.create(propertyRequest2, username)
        val objProp2 = objectService.findPropertyById(objPropResp2.id)

        assertTrue(objProp1.name == objProp2.name, "There are two props with same name")
    }

    @Test
    fun checkSavePropertyDiffNamesSameAspectTest() {
        val objVertex = newObject()
        val propertyRequest = PropertyCreateRequest(
            objectId = objVertex.id,
            name = "prop",
            description = null,
            aspectId = aspect.idStrict()
        )
        val objPropResp1 = objectService.create(propertyRequest, username)
        val objProp1 = objectService.findPropertyById(objPropResp1.id)
        val aspectId1 = session(db) { objProp1.aspect?.id }

        val propertyRequest2 = PropertyCreateRequest(
            objectId = objVertex.id,
            name = "prop2",
            description = null,
            aspectId = aspect.idStrict()
        )
        val objPropResp2 = objectService.create(propertyRequest2, username)
        val objProp2 = objectService.findPropertyById(objPropResp2.id)
        val aspectId2 = session(db) { objProp2.aspect?.id }

        assertTrue(aspectId1 == aspectId2, "There are two props with same aspectId")
    }

    @Test
    fun objectUpdateTest() {
        val objVertex = newObject()
        Thread.sleep(1) // to test timestamp update
        objectService.update(ObjectUpdateRequest(objVertex.id, "new name", "new description", subject.id, subject.version), username)
        val updatedObject = objectService.findById(objVertex.id)
        Assert.assertEquals("Object must have new name", "new name", updatedObject.name)
        Assert.assertEquals("Object must have new description", "new description", updatedObject.description)
        Assert.assertNotEquals("Object must have new timestamp", objVertex.timestamp, updatedObject.timestamp)
    }

    @Test
    fun objectUpdateWrongBkTest() {
        val objVertex1 = newObject()
        val objVertex2 = newObject()
        assertThrows<ObjectAlreadyExists> {
            objectService.update(ObjectUpdateRequest(objVertex1.id, objVertex2.name, "new description", subject.id, subject.version), username)
        }
    }

    @Test
    fun objectUpdateDiffBaseSubjectTest() {
        val objVertex = newObject()
        val sbj2 = subjectService.createSubject(SubjectData(name = "name2", description = "descr"), username)
        val objVertex2 = newObject(sbj2.id)
        objectService.update(ObjectUpdateRequest(objVertex.id, objVertex2.name, "new description", subject.id, subject.version), username)
        val updatedObject = objectService.findById(objVertex.id)
        Assert.assertEquals("Object must have new name", objVertex2.name, updatedObject.name)
        Assert.assertEquals("Object must have new description", "new description", updatedObject.description)
        transaction(db) {
            Assert.assertEquals("Object must have new subject id", subject.id, updatedObject.subject?.id)
            Assert.assertEquals("Object must have new subject name", subject.name, updatedObject.subject?.name)
        }
    }

    @Test
    fun objectPropertyUpdateTest() {
        val objVertex = newObject()
        val aspectVertex = aspectDao.find(aspect.id!!)
        val objectPropertyVertex = createObjectProperty(PropertyWriteInfo("propName", null, objVertex, aspectVertex!!))
        objectService.update(PropertyUpdateRequest(objectPropertyVertex.id, "new name", null, objectPropertyVertex.version), username)
        val objProperty = objectService.findPropertyById(objectPropertyVertex.id)
        Assert.assertEquals("Property must have new name", "new name", objProperty.name)
    }

    @Test
    fun objectPropertyUpdateWrongBkTest() {
        val objVertex = newObject()
        val aspectVertex = aspectDao.find(aspect.id!!)
        val objectPropertyVertex = createObjectProperty(PropertyWriteInfo("propName", null, objVertex, aspectVertex!!))
        createObjectProperty(PropertyWriteInfo("propName2", null, objVertex, aspectVertex))
        assertThrows<ObjectPropertyAlreadyExistException> {
            objectService.update(PropertyUpdateRequest(objectPropertyVertex.id, "propName2", null, objectPropertyVertex.version), username)
        }
    }

    @Test
    fun objPropertyValueUpdateTest() {
        val objVertex = newObject()
        val aspectVertex = aspectDao.find(complexAspect.id!!)
        val objPropertyVertex = createObjectProperty(PropertyWriteInfo("propName", null, objVertex, aspectVertex!!))
        val valueRequest1 = ValueCreateRequest(ObjectValueData.DecimalValue.single("123.4"), null, objPropertyVertex.id, Kilometre.name)
        val objPropValueResponse = objectService.create(valueRequest1, username)
        val updatedValueResponse = objectService.update(
            ValueUpdateRequest(objPropValueResponse.id, ObjectValueData.DecimalValue.single("1123.4"), Kilometre.name, null, objPropValueResponse.version),
            username
        )

        Assert.assertEquals("PropertyValue must have new value", "1123.4", updatedValueResponse.value.decimalStrict())
    }

    @Test
    fun objPropertyValueUpdateWrongBkTest() {
        val objVertex = newObject()
        val aspectVertex = aspectDao.find(complexAspect.id!!)
        val objPropertyVertex = createObjectProperty(PropertyWriteInfo("propName", null, objVertex, aspectVertex!!))

        val valueRequest1 = ValueCreateRequest(ObjectValueData.DecimalValue.single("123.4"), null, objPropertyVertex.id, Kilometre.name)
        val objPropValueResponse = objectService.create(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(ObjectValueData.DecimalValue.single("234.5"), null, objPropertyVertex.id, Kilometre.name)
        objectService.create(valueRequest2, username)

        objectService.update(
            ValueUpdateRequest(
                objPropValueResponse.id,
                ObjectValueData.DecimalValue.single("234.5"),
                Kilometre.name,
                null,
                objPropValueResponse.version
            ), username
        )
    }

    @Test
    fun getSubValuesSingleTest() {
        val objVertex = newObject()
        val aspectVertex = aspectDao.find(complexAspect.id!!)
        val objPropertyVertex = createObjectProperty(PropertyWriteInfo("propName", null, objVertex, aspectVertex!!))
        val valueRequest = ValueCreateRequest(ObjectValueData.DecimalValue.single("123.4"), null, objPropertyVertex.id, Kilometre.name)
        val objPropValueResponse = objectService.create(valueRequest, username)

        val subvalueIds = dao.getSubValues(objPropValueResponse.id).map { it.id }
        assertEquals(listOf(objPropValueResponse.id), subvalueIds)
    }

    @Test
    fun getSubValuesTwoRootsTest() {
        val objVertex = newObject()
        val aspectVertex = aspectDao.find(complexAspect.id!!)
        val objPropertyVertex = createObjectProperty(PropertyWriteInfo("propName", null, objVertex, aspectVertex!!))
        val valueRequest1 = ValueCreateRequest(ObjectValueData.DecimalValue.single("123.4"), null, objPropertyVertex.id, Kilometre.name)
        val objPropValueResponse1 = objectService.create(valueRequest1, username)
        val valueRequest2 = ValueCreateRequest(ObjectValueData.DecimalValue.single("234.5"), null, objPropertyVertex.id, Kilometre.name)
        val objPropValueResponse2 = objectService.create(valueRequest2, username)

        val subvalueIds1 = dao.getSubValues(objPropValueResponse1.id).map { it.id }
        assertEquals(listOf(objPropValueResponse1.id), subvalueIds1)
        val subvalueIds2 = dao.getSubValues(objPropValueResponse2.id).map { it.id }
        assertEquals(listOf(objPropValueResponse2.id), subvalueIds2)
    }


    @Test
    fun getSubValuesChildTest() {
        val objVertex = newObject()
        val aspectVertex = aspectDao.find(complexAspect.id!!)
        val objPropertyVertex = createObjectProperty(PropertyWriteInfo("propName", null, objVertex, aspectVertex!!))
        val valueRequest1 = ValueCreateRequest(ObjectValueData.DecimalValue.single("123.4"), null, objPropertyVertex.id, Kilometre.name)
        val objPropValueResponse1 = objectService.create(valueRequest1, username)
        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("hello2"),
            description = null,
            objectPropertyId = objPropertyVertex.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = objPropValueResponse1.id
        )
        val objPropValueResponse2 = objectService.create(valueRequest2, username)

        val subvalueIds1 = dao.getSubValues(objPropValueResponse1.id).map { it.id }
        assertEquals(listOf(objPropValueResponse1.id, objPropValueResponse2.id).toSet(), subvalueIds1.toSet())
        val subvalueIds2 = dao.getSubValues(objPropValueResponse2.id).map { it.id }
        assertEquals(listOf(objPropValueResponse2.id), subvalueIds2)
    }

    @Test
    fun getSubValuesTwoChildrenTest() {
        val objVertex = newObject()
        val aspectVertex = aspectDao.find(complexAspect.id!!)
        val objPropertyVertex = createObjectProperty(PropertyWriteInfo("propName", null, objVertex, aspectVertex!!))

        val valueRequest1 = ValueCreateRequest(ObjectValueData.DecimalValue.single("123.4"), null, objPropertyVertex.id, Kilometre.name)
        val objPropValueResponse1 = objectService.create(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("hello2"),
            description = null,
            objectPropertyId = objPropertyVertex.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = objPropValueResponse1.id
        )
        val objPropValue2 = objectService.create(valueRequest2, username)

        val valueRequest3 = ValueCreateRequest(
            value = ObjectValueData.StringValue("hello3"),
            description = null,
            objectPropertyId = objPropertyVertex.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = objPropValueResponse1.id
        )
        val objPropValue3 = objectService.create(valueRequest3, username)

        val subvalueIds1 = dao.getSubValues(objPropValueResponse1.id).map { it.id }
        assertEquals(listOf(objPropValueResponse1.id, objPropValue2.id, objPropValue3.id).toSet(), subvalueIds1.toSet())
        val subvalueIds2 = dao.getSubValues(objPropValue2.id).map { it.id }
        assertEquals(listOf(objPropValue2.id), subvalueIds2)
        val subvalueIds3 = dao.getSubValues(objPropValue3.id).map { it.id }
        assertEquals(listOf(objPropValue3.id), subvalueIds3)
    }

    @Test
    fun getSubValuesTwoGrandChildrenTest() {
        val objVertex = newObject()
        val aspectVertex = aspectDao.find(complexAspect.id!!)
        val objPropertyVertex = createObjectProperty(PropertyWriteInfo("propName", null, objVertex, aspectVertex!!))
        val valueRequest1 = ValueCreateRequest(ObjectValueData.DecimalValue.single("123.4"), null, objPropertyVertex.id, Kilometre.name)

        val objPropValueResponse1 = objectService.create(valueRequest1, username)
        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("hello"),
            description = null,
            objectPropertyId = objPropertyVertex.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = objPropValueResponse1.id
        )
        val objPropValueResponse2 = objectService.create(valueRequest2, username)
        val valueRequest3 = ValueCreateRequest(
            value = ObjectValueData.StringValue("hello2"),
            description = null,
            objectPropertyId = objPropertyVertex.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = objPropValueResponse2.id
        )
        val objPropValueResponse3 = objectService.create(valueRequest3, username)
        val valueRequest4 = ValueCreateRequest(
            value = ObjectValueData.StringValue("hello3"),
            description = null,
            objectPropertyId = objPropertyVertex.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = objPropValueResponse2.id
        )
        val objPropValue4 = objectService.create(valueRequest4, username)

        val subvalueIds1 = dao.getSubValues(objPropValueResponse1.id).map { it.id }
        assertEquals(setOf(objPropValueResponse1.id, objPropValueResponse2.id, objPropValueResponse3.id, objPropValue4.id), subvalueIds1.toSet())
        val subvalueIds2 = dao.getSubValues(objPropValueResponse2.id).map { it.id }
        assertEquals(setOf(objPropValueResponse2.id, objPropValueResponse3.id, objPropValue4.id), subvalueIds2.toSet())
        val subvalueIds3 = dao.getSubValues(objPropValueResponse3.id).map { it.id }
        assertEquals(listOf(objPropValueResponse3.id), subvalueIds3)
        val subvalueIds4 = dao.getSubValues(objPropValue4.id).map { it.id }
        assertEquals(listOf(objPropValue4.id), subvalueIds4)
    }

    @Test
    fun getPropValuesSingleTest() {
        val objVertex = newObject()
        val aspectVertex = aspectDao.find(complexAspect.id!!)
        val objPropertyVertex = createObjectProperty(PropertyWriteInfo("propName", null, objVertex, aspectVertex!!))
        val valueRequest = ValueCreateRequest(ObjectValueData.DecimalValue.single("123.4"), null, objPropertyVertex.id, Kilometre.name)
        val objPropValueResponse = objectService.create(valueRequest, username)

        val valueIds = dao.valuesOfProperty(objPropertyVertex.id).map { it.id }
        assertEquals(listOf(objPropValueResponse.id), valueIds)
    }

    @Test
    fun getPropValuesTwoRootsTest() {
        val objVertex = newObject()
        val aspectVertex = aspectDao.find(complexAspect.id!!)
        val objPropertyVertex = createObjectProperty(PropertyWriteInfo("propName", null, objVertex, aspectVertex!!))
        val valueRequest1 = ValueCreateRequest(ObjectValueData.DecimalValue.single("123.4"), null, objPropertyVertex.id, Kilometre.name)
        val objPropValueResponse1 = objectService.create(valueRequest1, username)
        val valueRequest2 = ValueCreateRequest(ObjectValueData.DecimalValue.single("234.5"), null, objPropertyVertex.id, Kilometre.name)
        val objPropValueResponse2 = objectService.create(valueRequest2, username)

        val valueIds = dao.valuesOfProperty(objPropertyVertex.id).map { it.id }
        assertEquals(setOf(objPropValueResponse1.id, objPropValueResponse2.id), valueIds.toSet())
    }

    @Test
    fun getPropValuesChildTest() {
        val objVertex = newObject()
        val aspectVertex = aspectDao.find(complexAspect.id!!)
        val objPropertyVertex = createObjectProperty(PropertyWriteInfo("propName", null, objVertex, aspectVertex!!))
        val valueRequest1 = ValueCreateRequest(ObjectValueData.DecimalValue.single("123.4"), null, objPropertyVertex.id, Kilometre.name)
        val objPropValueResponse1 = objectService.create(valueRequest1, username)
        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("hello2"),
            description = null,
            objectPropertyId = objPropertyVertex.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = objPropValueResponse1.id
        )
        val objPropValueResponse2 = objectService.create(valueRequest2, username)

        val valueIds = dao.valuesOfProperty(objPropertyVertex.id).map { it.id }
        assertEquals(setOf(objPropValueResponse1.id, objPropValueResponse2.id), valueIds.toSet())
    }

    @Test
    fun getPropValuesTwoChildrenTest() {
        val objVertex = newObject()
        val aspectVertex = aspectDao.find(complexAspect.id!!)
        val objPropertyVertex = createObjectProperty(PropertyWriteInfo("propName", null, objVertex, aspectVertex!!))
        val valueRequest1 = ValueCreateRequest(ObjectValueData.DecimalValue.single("123.4"), null, objPropertyVertex.id, Kilometre.name)

        val objPropValueResponse1 = objectService.create(valueRequest1, username)
        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("hello2"),
            description = null,
            objectPropertyId = objPropertyVertex.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = objPropValueResponse1.id
        )
        val objPropValueResponse2 = objectService.create(valueRequest2, username)
        val valueRequest3 = ValueCreateRequest(
            value = ObjectValueData.StringValue("hello3"),
            description = null,
            objectPropertyId = objPropertyVertex.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = objPropValueResponse1.id
        )
        val objPropValueResponse3 = objectService.create(valueRequest3, username)

        val valueIds = dao.valuesOfProperty(objPropertyVertex.id).map { it.id }
        assertEquals(setOf(objPropValueResponse1.id, objPropValueResponse2.id, objPropValueResponse3.id), valueIds.toSet())
    }

    @Test
    fun getPropValuesTwoGrandChildrenTest() {
        val objVertex = newObject()
        val aspectVertex = aspectDao.find(complexAspect.id!!)
        val objPropertyVertex = createObjectProperty(PropertyWriteInfo("propName", null, objVertex, aspectVertex!!))
        val valueRequest1 = ValueCreateRequest(ObjectValueData.DecimalValue.single("123.4"), null, objPropertyVertex.id, Kilometre.name)
        val objPropValueResponse1 = objectService.create(valueRequest1, username)
        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("hello2"),
            description = null,
            objectPropertyId = objPropertyVertex.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = objPropValueResponse1.id
        )
        val objPropValueResponse2 = objectService.create(valueRequest2, username)
        val valueRequest3 = ValueCreateRequest(
            value = ObjectValueData.StringValue("hello3"),
            description = null,
            objectPropertyId = objPropertyVertex.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = objPropValueResponse2.id
        )
        val objPropValueResponse3 = objectService.create(valueRequest3, username)
        val valueRequest4 = ValueCreateRequest(
            value = ObjectValueData.StringValue("hello4"),
            description = null,
            objectPropertyId = objPropertyVertex.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = objPropValueResponse2.id
        )
        val objPropValue4 = objectService.create(valueRequest4, username)

        val valueIds = dao.valuesOfProperty(objPropertyVertex.id).map { it.id }
        assertEquals(setOf(objPropValueResponse1.id, objPropValueResponse2.id, objPropValueResponse3.id, objPropValue4.id), valueIds.toSet())
    }

    @Test
    fun getPropValuesTwoPropsTest() {
        val objVertex = newObject()
        val aspectVertex = aspectDao.find(complexAspect.id!!)
        val objPropertyVertex1 = createObjectProperty(PropertyWriteInfo("propName", null, objVertex, aspectVertex!!))
        val valueRequest1 = ValueCreateRequest(ObjectValueData.DecimalValue.single("123.4"), null, objPropertyVertex1.id, Kilometre.name)
        val objPropValueResponse1 = objectService.create(valueRequest1, username)

        // objVertex should be reloaded or update object timestamp will fail with OConcurrentModificationException, because objVertex already updated
        val reloaded = dao.getObjectVertex(objVertex.id)!!
        // pretty strange issue here, seems like a bug in orient
        // next line fails with com.orientechnologies.orient.core.exception.ORecordNotFoundException: The record with id '#73:0' was not found
        // but The record with id '#73:0' has been loaded into reloaded
        // val willFail = objVertex.reload<OVertex>()
        val objPropertyVertex2 = createObjectProperty(PropertyWriteInfo("propName2", null, reloaded, aspectVertex))

        val valueRequest2 = ValueCreateRequest(ObjectValueData.DecimalValue.single("234.5"), null, objPropertyVertex2.id, Kilometre.name)
        objectService.create(valueRequest2, username)

        val valueIds = dao.valuesOfProperty(objPropertyVertex1.id).map { it.id }
        assertEquals(listOf(objPropValueResponse1.id), valueIds)
    }

    @Test
    fun valuesBetweenSingleTest() {
        val objVertex = newObject()
        val aspectVertex = aspectDao.find(complexAspect.id!!)
        val objPropertyVertex = createObjectProperty(PropertyWriteInfo("propName", null, objVertex, aspectVertex!!))
        val valueRequest = ValueCreateRequest(ObjectValueData.DecimalValue.single("123.4"), null, objPropertyVertex.id, Kilometre.name)
        val objPropValueResponse = objectService.create(valueRequest, username)

        val between = dao.valuesBetween(setOf(ORecordId(objPropValueResponse.id)), setOf(ORecordId(objPropValueResponse.id)))
        assertEquals(emptySet(), between)
    }

    @Test
    fun valuesBetweenTwoRootsTest() {
        val objVertex = newObject()
        val aspectVertex = aspectDao.find(complexAspect.id!!)
        val objPropertyVertex = createObjectProperty(PropertyWriteInfo("propName", null, objVertex, aspectVertex!!))

        val valueRequest1 = ValueCreateRequest(ObjectValueData.DecimalValue.single("12.3"), null, objPropertyVertex.id, Kilometre.name)
        val objPropValueResponse1 = objectService.create(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(ObjectValueData.DecimalValue.single("13.4"), null, objPropertyVertex.id, Kilometre.name)
        val objPropValueResponse2 = objectService.create(valueRequest2, username)

        val between1 = dao.valuesBetween(setOf(ORecordId(objPropValueResponse1.id)), setOf(ORecordId(objPropValueResponse2.id)))
        assertEquals(setOf(objPropValueResponse1.id), between1.map { it.id }.toSet())
        val between2 = dao.valuesBetween(setOf(ORecordId(objPropValueResponse2.id)), setOf(ORecordId(objPropValueResponse1.id)))
        assertEquals(setOf(objPropValueResponse2.id), between2.map { it.id }.toSet())
    }

    @Test
    fun valuesBetweenChildTest() {
        val objVertex = newObject()
        val aspectVertex = aspectDao.find(complexAspect.id!!)
        val objPropertyVertex = createObjectProperty(PropertyWriteInfo("propName", null, objVertex, aspectVertex!!))

        val valueRequest1 = ValueCreateRequest(ObjectValueData.DecimalValue.single("123.4"), null, objPropertyVertex.id, Kilometre.name)
        val objPropValueResponse1 = objectService.create(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("hello2"),
            description = null,
            objectPropertyId = objPropertyVertex.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = objPropValueResponse1.id
        )
        val objPropValueResponse2 = objectService.create(valueRequest2, username)

        val subvalueIds1 = dao.valuesBetween(setOf(ORecordId(objPropValueResponse1.id)), setOf(ORecordId(objPropValueResponse2.id)))
        assertEquals(setOf(objPropValueResponse1.id), subvalueIds1.map { it.id }.toSet())
        val subvalueIds2 = dao.valuesBetween(setOf(ORecordId(objPropValueResponse2.id)), setOf(ORecordId(objPropValueResponse1.id)))
        assertEquals(setOf(objPropValueResponse2.id), subvalueIds2.map { it.id }.toSet())
    }

    @Test
    fun valuesBetweenTwoChildrenTest() {
        val objVertex = newObject()
        val aspectVertex = aspectDao.find(complexAspect.id!!)
        val objPropertyVertex = createObjectProperty(PropertyWriteInfo("propName", null, objVertex, aspectVertex!!))

        val valueRequest1 = ValueCreateRequest(ObjectValueData.DecimalValue.single("12.3"), null, objPropertyVertex.id, Kilometre.name)
        val objPropValueResponse1 = objectService.create(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("hello2"),
            description = null,
            objectPropertyId = objPropertyVertex.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = objPropValueResponse1.id
        )
        val objPropValueResponse2 = objectService.create(valueRequest2, username)
        val valueRequest3 = ValueCreateRequest(
            value = ObjectValueData.StringValue("hello3"),
            description = null,
            objectPropertyId = objPropertyVertex.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = objPropValueResponse1.id
        )
        val objPropValueResponse3 = objectService.create(valueRequest3, username)

        val subvalueIds1 = dao.valuesBetween(setOf(ORecordId(objPropValueResponse1.id)), setOf(ORecordId(objPropValueResponse2.id)))
        assertEquals(setOf(objPropValueResponse1.id), subvalueIds1.map { it.id }.toSet())
        val subvalueIds2 = dao.valuesBetween(setOf(ORecordId(objPropValueResponse2.id)), setOf(ORecordId(objPropValueResponse1.id)))
        assertEquals(setOf(objPropValueResponse2.id), subvalueIds2.map { it.id }.toSet())
        val subvalueIds3 = dao.valuesBetween(setOf(ORecordId(objPropValueResponse3.id)), setOf(ORecordId(objPropValueResponse1.id)))
        assertEquals(setOf(objPropValueResponse3.id), subvalueIds3.map { it.id }.toSet())
        val subvalueIds4 = dao.valuesBetween(setOf(ORecordId(objPropValueResponse3.id)), setOf(ORecordId(objPropValueResponse2.id)))
        assertEquals(setOf(objPropValueResponse3.id, objPropValueResponse1.id), subvalueIds4.map { it.id }.toSet())
        val subvalueIds5 =
            dao.valuesBetween(setOf(ORecordId(objPropValueResponse3.id), ORecordId(objPropValueResponse2.id)), setOf(ORecordId(objPropValueResponse1.id)))
        assertEquals(setOf(objPropValueResponse3.id, objPropValueResponse2.id), subvalueIds5.map { it.id }.toSet())
        val subvalueIds6 = dao.valuesBetween(
            setOf(ORecordId(objPropValueResponse3.id), ORecordId(objPropValueResponse2.id)),
            setOf(ORecordId(objPropValueResponse1.id), ORecordId(objPropValueResponse2.id))
        )
        assertEquals(setOf(objPropValueResponse3.id), subvalueIds6.map { it.id }.toSet())
    }

    @Test
    fun valuesBetweenTwoGrandChildrenTest() {
        val objVertex = newObject()
        val aspectVertex = aspectDao.find(complexAspect.id!!)
        val objPropertyVertex = createObjectProperty(PropertyWriteInfo("propName", null, objVertex, aspectVertex!!))

        val valueRequest1 = ValueCreateRequest(ObjectValueData.DecimalValue.single("12.3"), null, objPropertyVertex.id, Kilometre.name)
        val objPropValueResponse1 = objectService.create(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("hello2"),
            description = null,
            objectPropertyId = objPropertyVertex.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = objPropValueResponse1.id
        )
        val objPropValueResponse2 = objectService.create(valueRequest2, username)

        val valueRequest3 = ValueCreateRequest(
            value = ObjectValueData.StringValue("hello3"),
            description = null,
            objectPropertyId = objPropertyVertex.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = objPropValueResponse2.id
        )
        val objPropValueResponse3 = objectService.create(valueRequest3, username)

        val valueRequest4 = ValueCreateRequest(
            value = ObjectValueData.StringValue("hello4"),
            description = null,
            objectPropertyId = objPropertyVertex.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = objPropValueResponse2.id
        )
        val objPropValueResponse4 = objectService.create(valueRequest4, username)

        val subvalueIds1 =
            dao.valuesBetween(setOf(ORecordId(objPropValueResponse3.id), ORecordId(objPropValueResponse4.id)), setOf(ORecordId(objPropValueResponse1.id)))
        assertEquals(setOf(objPropValueResponse2.id, objPropValueResponse3.id, objPropValueResponse4.id), subvalueIds1.map { it.id }.toSet())
        val subvalueIds2 =
            dao.valuesBetween(setOf(ORecordId(objPropValueResponse3.id), ORecordId(objPropValueResponse4.id)), setOf(ORecordId(objPropValueResponse2.id)))
        assertEquals(setOf(objPropValueResponse3.id, objPropValueResponse4.id), subvalueIds2.map { it.id }.toSet())
        val subvalueIds3 = dao.valuesBetween(
            setOf(ORecordId(objPropValueResponse3.id), ORecordId(objPropValueResponse4.id)),
            setOf(ORecordId(objPropValueResponse1.id), ORecordId(objPropValueResponse3.id))
        )
        assertEquals(setOf(objPropValueResponse2.id, objPropValueResponse4.id), subvalueIds3.map { it.id }.toSet())
    }

    @Test
    fun linkedFromEmptyTest() {
        val objVertex = newObject()
        val aspectVertex = aspectDao.find(complexAspect.id!!)
        val objPropertyVertex = createObjectProperty(PropertyWriteInfo("propName", null, objVertex, aspectVertex!!))
        ValueCreateRequest(
            value = ObjectValueData.IntegerValue(INT_SAMPLE_VALUE, null),
            description = null,
            objectPropertyId = objPropertyVertex.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null
        )
        val objPropValue =
            createObjectPropertyValue(ValueWriteInfo(ObjectValue.IntegerValue(INT_SAMPLE_VALUE, null), null, objPropertyVertex, null, null, null))

        val objLinks = dao.linkedFrom(setOf(objVertex.identity), emptySet())
        val propLinks = dao.linkedFrom(setOf(objPropertyVertex.identity), emptySet())
        val valueLinks = dao.linkedFrom(setOf(objPropValue.identity), emptySet())

        assertEquals(emptyMap(), objLinks)
        assertEquals(emptyMap(), propLinks)
        assertEquals(emptyMap(), valueLinks)
    }

    @Test
    fun linkedFromObjPropTest() {
        val objVertex = createObject(ObjectCreateRequest(randomName(), sampleDescription, subject.id))
        val aspectVertex = aspectDao.find(complexAspect.id!!)
        val objPropertyVertex = createObjectProperty(PropertyWriteInfo("propName", null, objVertex, aspectVertex!!))
        val objPropValue =
            createObjectPropertyValue(ValueWriteInfo(ObjectValue.IntegerValue(INT_SAMPLE_VALUE, null), null, objPropertyVertex, null, null, null))

        val objLinks = dao.linkedFrom(setOf(objVertex.identity), setOf(OBJECT_OBJECT_PROPERTY_EDGE))
        val propLinks = dao.linkedFrom(setOf(objPropertyVertex.identity), setOf(OBJECT_OBJECT_PROPERTY_EDGE))
        val valueLinks = dao.linkedFrom(setOf(objPropValue.identity), setOf(OBJECT_OBJECT_PROPERTY_EDGE))

        assertEquals(mapOf(objVertex.identity to setOf(objPropertyVertex.identity)), objLinks)
        assertEquals(emptyMap(), propLinks)
        assertEquals(emptyMap(), valueLinks)
    }

    @Test
    fun linkedFromPropValueTest() {
        val objVertex = createObject(ObjectCreateRequest(randomName(), sampleDescription, subject.id))
        val aspectVertex = aspectDao.find(complexAspect.id!!)
        val objPropertyVertex = createObjectProperty(PropertyWriteInfo("propName", null, objVertex, aspectVertex!!))
        val objPropValue =
            createObjectPropertyValue(ValueWriteInfo(ObjectValue.IntegerValue(INT_SAMPLE_VALUE, null), null, objPropertyVertex, null, null, null))

        val objLinks = dao.linkedFrom(setOf(objVertex.identity), setOf(OBJECT_VALUE_OBJECT_PROPERTY_EDGE))
        val propLinks = dao.linkedFrom(setOf(objPropertyVertex.identity), setOf(OBJECT_VALUE_OBJECT_PROPERTY_EDGE))
        val valueLinks = dao.linkedFrom(setOf(objPropValue.identity), setOf(OBJECT_VALUE_OBJECT_PROPERTY_EDGE))

        assertEquals(emptyMap(), objLinks)
        assertEquals(mapOf(objPropertyVertex.identity to setOf(objPropValue.identity)), propLinks)
        assertEquals(emptyMap(), valueLinks)
    }

    @Test
    fun linkedFromValueValueTest() {
        val objVertex = createObject(ObjectCreateRequest(randomName(), sampleDescription, subject.id))
        val aspectVertex = aspectDao.find(complexAspect.id!!)
        val objPropertyVertex = createObjectProperty(PropertyWriteInfo("propName", null, objVertex, aspectVertex!!))
        val objPropValue1 =
            createObjectPropertyValue(ValueWriteInfo(ObjectValue.IntegerValue(INT_SAMPLE_VALUE, null), null, objPropertyVertex, null, null, null))
        val objPropValue2 = createObjectPropertyValue(ValueWriteInfo(ObjectValue.StringValue("123"), null, objPropertyVertex, null, objPropValue1, null))

        val objLinks = dao.linkedFrom(setOf(objVertex.identity), setOf(OBJECT_VALUE_OBJECT_VALUE_EDGE))
        val propLinks = dao.linkedFrom(setOf(objPropertyVertex.identity), setOf(OBJECT_VALUE_OBJECT_VALUE_EDGE))
        val valueLinks = dao.linkedFrom(setOf(objPropValue1.identity), setOf(OBJECT_VALUE_OBJECT_VALUE_EDGE))

        assertEquals(emptyMap(), objLinks)
        assertEquals(emptyMap(), propLinks)
        assertEquals(mapOf(objPropValue1.identity to setOf(objPropValue2.identity)), valueLinks)
    }

    @Test
    fun linkedFromTwoEdgesTest() {
        val objVertex = createObject(ObjectCreateRequest(randomName(), sampleDescription, subject.id))
        val aspectVertex = aspectDao.find(complexAspect.id!!)
        val objPropertyVertex = createObjectProperty(PropertyWriteInfo("propName", null, objVertex, aspectVertex!!))
        val objPropValue1 =
            createObjectPropertyValue(ValueWriteInfo(ObjectValue.IntegerValue(INT_SAMPLE_VALUE, null), null, objPropertyVertex, null, null, null))
        val objPropValue2 = createObjectPropertyValue(ValueWriteInfo(ObjectValue.StringValue("123"), null, objPropertyVertex, null, objPropValue1, null))

        val objLinks = dao.linkedFrom(setOf(objVertex.identity), setOf(OBJECT_VALUE_OBJECT_PROPERTY_EDGE, OBJECT_VALUE_OBJECT_VALUE_EDGE))
        val propLinks = dao.linkedFrom(setOf(objPropertyVertex.identity), setOf(OBJECT_VALUE_OBJECT_PROPERTY_EDGE, OBJECT_VALUE_OBJECT_VALUE_EDGE))
        val valueLinks1 = dao.linkedFrom(setOf(objPropValue1.identity), setOf(OBJECT_VALUE_OBJECT_PROPERTY_EDGE, OBJECT_VALUE_OBJECT_VALUE_EDGE))
        val valueLinks2 = dao.linkedFrom(setOf(objPropValue2.identity), setOf(OBJECT_VALUE_OBJECT_PROPERTY_EDGE, OBJECT_VALUE_OBJECT_VALUE_EDGE))

        assertEquals(emptyMap(), objLinks)
        assertEquals(mapOf(objPropertyVertex.identity to setOf(objPropValue1.identity, objPropValue2.identity)), propLinks)
        assertEquals(mapOf(objPropValue1.identity to setOf(objPropValue2.identity)), valueLinks1)
        assertEquals(emptyMap(), valueLinks2)
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