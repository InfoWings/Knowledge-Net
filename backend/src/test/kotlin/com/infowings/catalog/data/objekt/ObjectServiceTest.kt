package com.infowings.catalog.data.objekt

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.ObjectCreateRequest
import com.infowings.catalog.common.objekt.PropertyCreateRequest
import com.infowings.catalog.common.objekt.ValueCreateRequest
import com.infowings.catalog.data.MeasureService
import com.infowings.catalog.data.Subject
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.id
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
import kotlin.test.fail


@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ObjectServiceTest {
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
    @Autowired
    private lateinit var historyService: HistoryService

    private lateinit var subject: Subject

    private lateinit var aspect: AspectData

    private lateinit var complexAspect: AspectData

    private val username = "admin"

    @Before
    fun initTestData() {
        subject = subjectService.createSubject(SubjectData(name = "subjectName", description = "descr"), username)
        aspect = aspectService.save(
            AspectData(name = "aspectName", description = "aspectDescr", baseType = BaseType.Text.name), username
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
    fun createObjectTest() {
        val request = ObjectCreateRequest("createObjectTestName", "object descr", subject.id, subject.version)
        val createdObjectId = objectService.create(request, "user")

        assertTrue(createdObjectId != null)
        val objectVertex = objectService.findById(createdObjectId)
        assertEquals(request.name, objectVertex.name, "names must be equal")
        assertEquals(request.description, objectVertex.description, "descriptions must be equal")
        transaction(db) {
            assertEquals(request.subjectId, objectVertex.subject?.id, "subjects must be equal")
        }
    }


    @Test
    fun createObjectWithPropertyTest() {
        val objectRequest =
            ObjectCreateRequest("createObjectWithPropertyTestName", "object descr", subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")

        val propertyRequest = PropertyCreateRequest(
            name = "prop_createObjectWithPropertyTestName",
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )

        val createdPropertyId = objectService.create(propertyRequest, username)
        assertTrue(createdPropertyId != null)

        val foundObject = objectService.findById(createdObjectId)
        val foundProperty = objectService.findPropertyById(createdPropertyId)

        assertEquals(propertyRequest.name, foundProperty.name, "name is incorrect")

        transaction(db) {
            assertEquals(PropertyCardinality.ZERO.name, foundProperty.cardinality.name, "cardinality is incorrect")

            val objectOfProperty = foundProperty.objekt

            if (objectOfProperty == null) {
                fail("object of property is null")
            } else {
                assertEquals(foundObject.id, objectOfProperty.id, "ids must be same")
                assertEquals(propertyRequest.aspectId, foundProperty.aspect?.id, "aspect id is incorrect is incorrect")

                assertEquals(1, foundObject.properties.size, "found parent object must contain 1 property")
                assertEquals(1, objectOfProperty.properties.size, "updated parent object must contain 1 property")
                assertEquals(
                    createdPropertyId, foundObject.properties.first().id,
                    "found parent object must contain correct property"
                )
                assertEquals(
                    createdPropertyId, objectOfProperty.properties.first().id,
                    "found parent object must contain correct property"
                )
            }
        }
    }


    @Test
    fun createObjectWithValueTest() {
        val objectRequest =
            ObjectCreateRequest("createObjectWithValueTestName", "object descr", subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")

        val propertyRequest = PropertyCreateRequest(
            name = "prop_createObjectWithValueTestName",
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId = objectService.create(propertyRequest, username)

        val scalarInt = 123

        val valueRequest = ValueCreateRequest(
            value = ObjectValueData.IntegerValue(scalarInt, null),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue: ObjectPropertyValue = objectService.create(valueRequest, username)

        val objectValue = createdValue.value

        when (objectValue) {
            is ObjectValue.IntegerValue -> {
                assertEquals(scalarInt, objectValue.value, "scalar value must be with correct type name")
                assertEquals(
                    valueRequest.objectPropertyId, createdValue.objectProperty.id,
                    "object property must point to parent property"
                )
                assertEquals(
                    valueRequest.aspectPropertyId, createdValue.aspectProperty?.id,
                    "object property must point to proper root characteristic"
                )
                assertTrue(createdValue.parentValue == null)
            }
            else ->
                fail("value must be integer")
        }

    }

    private fun checkValueAbsense(id: String) = try {
        val found = objectService.findPropertyValueById(id)
        fail("object property value is found after deletion: $found")
    } catch (e: ObjectPropertyValueNotFoundException) {
    }

    private fun checkValuesAbsense(ids: List<String>) = ids.forEach { checkValueAbsense(it) }

    private fun checkValueSoftAbsense(id: String) = {
        val found = objectService.findPropertyValueById(id)
        assertEquals(true, found.deleted)
    }

    private fun checkValuesSoftAbsense(ids: List<String>) = ids.forEach { checkValueSoftAbsense(it) }

    private fun checkPropertyAbsense(id: String) = try {
        val found = objectService.findPropertyById(id)
        fail("object property is found after deletion: $found")
    } catch (e: ObjectPropertyNotFoundException) {
    }

    private fun checkPropertiesAbsense(ids: List<String>) = ids.forEach { checkPropertyAbsense(it) }

    private fun checkPropertySoftAbsense(id: String) = {
        val found = objectService.findPropertyById(id)
        assertEquals(true, found.deleted)
    }

    private fun checkPropertiesSoftAbsense(ids: List<String>) = ids.forEach { checkPropertySoftAbsense(it) }

    private fun checkObjectAbsense(id: String) = try {
        val found = objectService.findById(id)
        fail("object is found after deletion: $found")
    } catch (e: ObjectNotFoundException) {
    }

    private fun checkObjectSoftAbsense(id: String) {
        val found = objectService.findById(id)
        assertEquals(true, found.deleted)
    }

    @Test
    fun deleteObjectTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")

        objectService.deleteObject(createdObjectId, username)

        checkObjectAbsense(createdObjectId)
    }

    @Test
    fun softDeleteObjectTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")

        objectService.softDeleteObject(createdObjectId, username)

        checkObjectAbsense(createdObjectId)
    }

    @Test
    fun deleteObjectWithPropTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")
        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId = objectService.create(propertyRequest, username)

        objectService.deleteObject(createdObjectId, username)

        checkObjectAbsense(createdObjectId)
        checkPropertyAbsense(createdPropertyId)
    }

    @Test
    fun softDeleteObjectWithPropTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")
        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId = objectService.create(propertyRequest, username)

        objectService.softDeleteObject(createdObjectId, username)

        checkObjectAbsense(createdObjectId)
        checkPropertyAbsense(createdPropertyId)
    }

    @Test
    fun deleteObjectWithTwoPropsTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")
        val propertyName1 = "prop1_$objectName"
        val propertyName2 = "prop2_$objectName"

        val propertyRequest = PropertyCreateRequest(name = propertyName1, objectId = createdObjectId, aspectId = aspect.idStrict())
        val createdPropertyId1 = objectService.create(propertyRequest, username)
        val createdPropertyId2 = objectService.create(propertyRequest.copy(name = propertyName2), username)

        objectService.deleteObject(createdObjectId, username)

        checkObjectAbsense(createdObjectId)
        checkPropertiesAbsense(listOf(createdPropertyId1, createdPropertyId2))
    }

    @Test
    fun softDeleteObjectWithTwoPropsTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")
        val propertyName1 = "prop1_$objectName"
        val propertyName2 = "prop2_$objectName"

        val propertyRequest = PropertyCreateRequest(name = propertyName1, objectId = createdObjectId, aspectId = aspect.idStrict())
        val createdPropertyId1 = objectService.create(propertyRequest, username)
        val createdPropertyId2 = objectService.create(propertyRequest.copy(name = propertyName2), username)

        objectService.deleteObject(createdObjectId, username)

        checkObjectAbsense(createdObjectId)
        checkPropertiesAbsense(listOf(createdPropertyId1, createdPropertyId2))
    }

    @Test
    fun deleteObjectWithValueTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")
        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(name = propertyName, objectId = createdObjectId, aspectId = aspect.idStrict())
        val createdPropertyId = objectService.create(propertyRequest, username)

        val valueRequest = ValueCreateRequest(ObjectValueData.StringValue("123"), createdPropertyId)
        val createdValue = objectService.create(valueRequest, username)

        objectService.deleteObject(createdObjectId, username)

        checkObjectAbsense(createdObjectId)
        checkPropertyAbsense(createdPropertyId)
        checkValueAbsense(createdValue.id.toString())
    }

    @Test
    fun softDeleteObjectWithValueTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")
        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(name = propertyName, objectId = createdObjectId, aspectId = aspect.idStrict())
        val createdPropertyId = objectService.create(propertyRequest, username)

        val valueRequest = ValueCreateRequest(ObjectValueData.StringValue("123"), createdPropertyId)
        val createdValue = objectService.create(valueRequest, username)

        objectService.softDeleteObject(createdObjectId, username)

        checkObjectAbsense(createdObjectId)
        checkPropertyAbsense(createdPropertyId)
        checkValueAbsense(createdValue.id.toString())
    }

    @Test
    fun deleteObjectInternallyLinkedTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")
        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(name = propertyName, objectId = createdObjectId, aspectId = aspect.idStrict())
        val createdPropertyId = objectService.create(propertyRequest, username)

        val valueRequest = ValueCreateRequest(ObjectValueData.Link(LinkValueData.Object(createdObjectId)), createdPropertyId)
        val createdValue = objectService.create(valueRequest, username)


        objectService.deleteObject(createdObjectId, username)

        checkObjectAbsense(createdObjectId)
        checkPropertyAbsense(createdPropertyId)
        checkValueAbsense(createdValue.id.toString())
    }

    @Test
    fun softDeleteObjectInternallyLinkedTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")
        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(name = propertyName, objectId = createdObjectId, aspectId = aspect.idStrict())
        val createdPropertyId = objectService.create(propertyRequest, username)

        val valueRequest = ValueCreateRequest(ObjectValueData.Link(LinkValueData.Object(createdObjectId)), createdPropertyId)
        val createdValue = objectService.create(valueRequest, username)


        objectService.softDeleteObject(createdObjectId, username)

        checkObjectAbsense(createdObjectId)
        checkPropertyAbsense(createdPropertyId)
        checkValueAbsense(createdValue.id.toString())
    }

    @Test
    fun deleteObjectExternallyLinkedTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")

        val objectRequest2 =
            ObjectCreateRequest("$objectName-2", objectDescription, subject.id, subject.version)
        val createdObjectId2 = objectService.create(objectRequest2, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(name = propertyName, objectId = createdObjectId2, aspectId = aspect.idStrict())
        val createdPropertyId = objectService.create(propertyRequest, username)

        val valueRequest = ValueCreateRequest(ObjectValueData.Link(LinkValueData.Object(createdObjectId)), createdPropertyId)
        val createdValue = objectService.create(valueRequest, username)


        try {
            objectService.deleteObject(createdObjectId, username)
            fail("Expected exception is not thrown")
        } catch (e: ObjectIsLinkedException) {
        }
    }

    @Test
    fun softDeleteObjectExternallyLinkedTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")

        val objectRequest2 =
            ObjectCreateRequest("$objectName-2", objectDescription, subject.id, subject.version)
        val createdObjectId2 = objectService.create(objectRequest2, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(name = propertyName, objectId = createdObjectId2, aspectId = aspect.idStrict())
        val createdPropertyId = objectService.create(propertyRequest, username)

        val valueRequest = ValueCreateRequest(ObjectValueData.Link(LinkValueData.Object(createdObjectId)), createdPropertyId)
        val createdValue = objectService.create(valueRequest, username)

        objectService.softDeleteObject(createdObjectId, username)

        checkObjectSoftAbsense(createdObjectId)
    }

    @Test
    fun deleteObjectWithTwoRootValuesTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")
        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(name = propertyName, objectId = createdObjectId, aspectId = aspect.idStrict())
        val createdPropertyId = objectService.create(propertyRequest, username)

        val valueRequest1 = ValueCreateRequest(ObjectValueData.StringValue("123"), createdPropertyId)
        val createdValue1 = objectService.create(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(ObjectValueData.IntegerValue(123, null), createdPropertyId)
        val createdValue2 = objectService.create(valueRequest2, username)


        objectService.deleteObject(createdObjectId, username)

        checkObjectAbsense(createdObjectId)
        checkPropertyAbsense(createdPropertyId)
        checkValuesAbsense(listOf(createdValue1, createdValue2).map { it.id.toString() })
    }

    @Test
    fun softDeleteObjectWithTwoRootValuesTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")
        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(name = propertyName, objectId = createdObjectId, aspectId = aspect.idStrict())
        val createdPropertyId = objectService.create(propertyRequest, username)

        val valueRequest1 = ValueCreateRequest(ObjectValueData.StringValue("123"), createdPropertyId)
        val createdValue1 = objectService.create(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(ObjectValueData.IntegerValue(123, null), createdPropertyId)
        val createdValue2 = objectService.create(valueRequest2, username)


        objectService.softDeleteObject(createdObjectId, username)

        checkObjectAbsense(createdObjectId)
        checkPropertyAbsense(createdPropertyId)
        checkValuesAbsense(listOf(createdValue1, createdValue2).map { it.id.toString() })
    }

    @Test
    fun deleteObjectWithChildValueTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")
        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(name = propertyName, objectId = createdObjectId, aspectId = aspect.idStrict())
        val createdPropertyId = objectService.create(propertyRequest, username)

        val valueRequest1 = ValueCreateRequest(ObjectValueData.StringValue("123"), createdPropertyId)
        val createdValue1 = objectService.create(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.IntegerValue(123, null), objectPropertyId = createdPropertyId,
            measureId = null, aspectPropertyId = null, parentValueId = createdValue1.id.toString()
        )
        val createdValue2 = objectService.create(valueRequest2, username)

        objectService.deleteObject(createdObjectId, username)

        checkObjectAbsense(createdObjectId)
        checkPropertyAbsense(createdPropertyId)
        checkValuesAbsense(listOf(createdValue1, createdValue2).map { it.id.toString() })
    }

    @Test
    fun softDeleteObjectWithChildValueTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")
        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(name = propertyName, objectId = createdObjectId, aspectId = aspect.idStrict())
        val createdPropertyId = objectService.create(propertyRequest, username)

        val valueRequest1 = ValueCreateRequest(ObjectValueData.StringValue("123"), createdPropertyId)
        val createdValue1 = objectService.create(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.IntegerValue(123, null), objectPropertyId = createdPropertyId,
            measureId = null, aspectPropertyId = null, parentValueId = createdValue1.id.toString()
        )
        val createdValue2 = objectService.create(valueRequest2, username)

        objectService.softDeleteObject(createdObjectId, username)

        checkObjectAbsense(createdObjectId)
        checkPropertyAbsense(createdPropertyId)
        checkValuesAbsense(listOf(createdValue1, createdValue2).map { it.id.toString() })
    }

    @Test
    fun deletePropertyTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId = objectService.create(propertyRequest, username)

        val createdObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        objectService.deleteProperty(createdPropertyId, username)

        val updatedObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(0, updatedObject.properties.size)
        }
        checkPropertyAbsense(createdPropertyId)
    }

    @Test
    fun softDeletePropertyTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId = objectService.create(propertyRequest, username)

        val createdObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        objectService.softDeleteProperty(createdPropertyId, username)

        val updatedObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(0, updatedObject.properties.size)
        }
        checkPropertyAbsense(createdPropertyId)
    }

    @Test
    fun deletePropertyWithValueTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId = objectService.create(propertyRequest, username)

        val createdObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        val scalarInt = 123

        val valueRequest = ValueCreateRequest(
            value = ObjectValueData.IntegerValue(scalarInt, null),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue: ObjectPropertyValue = objectService.create(valueRequest, username)

        objectService.deleteProperty(createdPropertyId, username)

        val updatedObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(0, updatedObject.properties.size)
        }
        checkPropertyAbsense(createdPropertyId)
        checkValueAbsense(createdValue.id.toString())
    }

    @Test
    fun softDeletePropertyWithValueTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId = objectService.create(propertyRequest, username)

        val createdObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        val scalarInt = 123

        val valueRequest = ValueCreateRequest(
            value = ObjectValueData.IntegerValue(scalarInt, null),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue: ObjectPropertyValue = objectService.create(valueRequest, username)

        objectService.softDeleteProperty(createdPropertyId, username)

        val updatedObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(0, updatedObject.properties.size)
        }
        checkPropertyAbsense(createdPropertyId)
        checkValueAbsense(createdValue.id.toString())
    }

    @Test
    fun deletePropertyWithTwoRootsTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId = objectService.create(propertyRequest, username)

        val createdObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        val valueRequest1 = ValueCreateRequest(
            value = ObjectValueData.IntegerValue(123, null),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue1: ObjectPropertyValue = objectService.create(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("hello"),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue2: ObjectPropertyValue = objectService.create(valueRequest2, username)

        objectService.deleteProperty(createdPropertyId, username)

        val updatedObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(0, updatedObject.properties.size)
        }
        checkPropertyAbsense(createdPropertyId)
        checkValuesAbsense(listOf(createdValue1, createdValue2).map { it.id.toString() })
    }

    @Test
    fun softDeletePropertyWithTwoRootsTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId = objectService.create(propertyRequest, username)

        val createdObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        val valueRequest1 = ValueCreateRequest(
            value = ObjectValueData.IntegerValue(123, null),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue1: ObjectPropertyValue = objectService.create(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("hello"),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue2: ObjectPropertyValue = objectService.create(valueRequest2, username)

        objectService.softDeleteProperty(createdPropertyId, username)

        val updatedObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(0, updatedObject.properties.size)
        }
        checkPropertyAbsense(createdPropertyId)
        checkValuesAbsense(listOf(createdValue1, createdValue2).map { it.id.toString() })
    }


    @Test
    fun deletePropertyWithChildTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId = objectService.create(propertyRequest, username)

        val createdObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        val valueRequest1 = ValueCreateRequest(
            value = ObjectValueData.IntegerValue(123, null),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue1: ObjectPropertyValue = objectService.create(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("hello"),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = createdValue1.id.toString(),
            measureId = null
        )
        val createdValue2: ObjectPropertyValue = objectService.create(valueRequest2, username)

        objectService.deleteProperty(createdPropertyId, username)

        val updatedObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(0, updatedObject.properties.size)
        }
        checkPropertyAbsense(createdPropertyId)
        checkValuesAbsense(listOf(createdValue1, createdValue2).map { it.id.toString() })
    }

    @Test
    fun softDeletePropertyWithChildTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId = objectService.create(propertyRequest, username)

        val createdObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        val valueRequest1 = ValueCreateRequest(
            value = ObjectValueData.IntegerValue(123, null),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue1: ObjectPropertyValue = objectService.create(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("hello"),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = createdValue1.id.toString(),
            measureId = null
        )
        val createdValue2: ObjectPropertyValue = objectService.create(valueRequest2, username)

        objectService.softDeleteProperty(createdPropertyId, username)

        val updatedObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(0, updatedObject.properties.size)
        }
        checkPropertyAbsense(createdPropertyId)
        checkValuesAbsense(listOf(createdValue1, createdValue2).map { it.id.toString() })
    }

    @Test
    fun deletePropertyInternallyLinkedTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId = objectService.create(propertyRequest, username)

        val createdObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        val valueRequest1 = ValueCreateRequest(
            value = ObjectValueData.Link(LinkValueData.ObjectProperty(createdPropertyId)),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue1: ObjectPropertyValue = objectService.create(valueRequest1, username)

        objectService.deleteProperty(createdPropertyId, username)

        val updatedObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(0, updatedObject.properties.size)
        }
        checkPropertyAbsense(createdPropertyId)
        checkValuesAbsense(listOf(createdValue1).map { it.id.toString() })
    }

    @Test
    fun softDeletePropertyInternallyLinkedTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId = objectService.create(propertyRequest, username)

        val createdObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        val valueRequest1 = ValueCreateRequest(
            value = ObjectValueData.Link(LinkValueData.ObjectProperty(createdPropertyId)),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue1: ObjectPropertyValue = objectService.create(valueRequest1, username)

        objectService.softDeleteProperty(createdPropertyId, username)

        val updatedObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(0, updatedObject.properties.size)
        }
        checkPropertyAbsense(createdPropertyId)
        checkValuesAbsense(listOf(createdValue1).map { it.id.toString() })
    }

    @Test
    fun deletePropertyExternallyLinkedTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")

        val propertyName1 = "prop1_$objectName"
        val propertyRequest1 = PropertyCreateRequest(
            name = propertyName1,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId1 = objectService.create(propertyRequest1, username)

        val propertyName2= "prop2_$objectName"
        val propertyRequest2 = PropertyCreateRequest(
            name = propertyName2,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId2 = objectService.create(propertyRequest2, username)

        val createdObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(2, createdObject.properties.size)
        }

        val valueRequest1 = ValueCreateRequest(
            value = ObjectValueData.Link(LinkValueData.ObjectProperty(createdPropertyId2)),
            objectPropertyId = createdPropertyId1,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue1: ObjectPropertyValue = objectService.create(valueRequest1, username)

        try {
            objectService.deleteProperty(createdPropertyId2, username)
            fail("no exception thrown")
        } catch (e: ObjectPropertyIsLinkedException) {
            assertEquals(createdPropertyId2, e.propertyId)
        }

        val updatedObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(2, updatedObject.properties.size)
        }
    }

    @Test
    fun softDeletePropertyExternallyLinkedTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")

        val propertyName1 = "prop1_$objectName"
        val propertyRequest1 = PropertyCreateRequest(
            name = propertyName1,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId1 = objectService.create(propertyRequest1, username)

        val propertyName2= "prop2_$objectName"
        val propertyRequest2 = PropertyCreateRequest(
            name = propertyName2,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId2 = objectService.create(propertyRequest2, username)

        val createdObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(2, createdObject.properties.size)
        }

        val valueRequest1 = ValueCreateRequest(
            value = ObjectValueData.Link(LinkValueData.ObjectProperty(createdPropertyId2)),
            objectPropertyId = createdPropertyId1,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue1: ObjectPropertyValue = objectService.create(valueRequest1, username)

        objectService.softDeleteProperty(createdPropertyId2, username)

        val updatedObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(1, updatedObject.properties.size)
        }
    }

    @Test
    fun deleteValueTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId = objectService.create(propertyRequest, username)

        val scalarInt = 123

        val valueRequest = ValueCreateRequest(
            value = ObjectValueData.IntegerValue(scalarInt, null),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue: ObjectPropertyValue = objectService.create(valueRequest, username)


        val createdProperty = objectService.findPropertyById(createdPropertyId)
        transaction(db) {
            assertEquals(1, createdProperty.values.size)
        }

        objectService.deleteValue(createdValue.id.toString(), username)

        val updatedProperty = objectService.findPropertyById(createdPropertyId)

        transaction(db) {
            assertEquals(0, updatedProperty.values.size)
        }

        checkValueAbsense(createdValue.id.toString())
    }

    @Test
    fun softDeleteValueTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId = objectService.create(propertyRequest, username)

        val scalarInt = 123

        val valueRequest = ValueCreateRequest(
            value = ObjectValueData.IntegerValue(scalarInt, null),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue: ObjectPropertyValue = objectService.create(valueRequest, username)


        val createdProperty = objectService.findPropertyById(createdPropertyId)
        transaction(db) {
            assertEquals(1, createdProperty.values.size)
        }

        objectService.softDeleteValue(createdValue.id.toString(), username)

        val updatedProperty = objectService.findPropertyById(createdPropertyId)

        transaction(db) {
            assertEquals(0, updatedProperty.values.size)
        }

        checkValueAbsense(createdValue.id.toString())
    }

    @Test
    fun deleteValueWithChildTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId = objectService.create(propertyRequest, username)

        val scalarInt = 123

        val valueRequest = ValueCreateRequest(
            value = ObjectValueData.IntegerValue(scalarInt, null),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue: ObjectPropertyValue = objectService.create(valueRequest, username)

        val valueChildRequest = ValueCreateRequest(
            value = ObjectValueData.IntegerValue(scalarInt + 1, null),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = createdValue.id.toString(),
            measureId = null
        )
        val createdValueChild: ObjectPropertyValue = objectService.create(valueChildRequest, username)


        val createdProperty = objectService.findPropertyById(createdPropertyId)
        transaction(db) {
            assertEquals(2, createdProperty.values.size)
        }

        objectService.deleteValue(createdValue.id.toString(), username)

        val updatedProperty = objectService.findPropertyById(createdPropertyId)

        transaction(db) {
            assertEquals(0, updatedProperty.values.size)
        }

        checkValuesAbsense(listOf(createdValue, createdValueChild).map { it.id.toString() })
    }

    @Test
    fun softDeleteValueWithChildTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId = objectService.create(propertyRequest, username)

        val scalarInt = 123

        val valueRequest = ValueCreateRequest(
            value = ObjectValueData.IntegerValue(scalarInt, null),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue: ObjectPropertyValue = objectService.create(valueRequest, username)

        val valueChildRequest = ValueCreateRequest(
            value = ObjectValueData.IntegerValue(scalarInt + 1, null),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = createdValue.id.toString(),
            measureId = null
        )
        val createdValueChild: ObjectPropertyValue = objectService.create(valueChildRequest, username)


        val createdProperty = objectService.findPropertyById(createdPropertyId)
        transaction(db) {
            assertEquals(2, createdProperty.values.size)
        }

        objectService.softDeleteValue(createdValue.id.toString(), username)

        val updatedProperty = objectService.findPropertyById(createdPropertyId)

        transaction(db) {
            assertEquals(0, updatedProperty.values.size)
        }

        checkValuesAbsense(listOf(createdValue, createdValueChild).map { it.id.toString() })
    }

    @Test
    fun deleteChildOfValueTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId = objectService.create(propertyRequest, username)

        val scalarInt = 123

        val valueRequest = ValueCreateRequest(
            value = ObjectValueData.IntegerValue(scalarInt, null),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue: ObjectPropertyValue = objectService.create(valueRequest, username)

        val valueChildRequest = ValueCreateRequest(
            value = ObjectValueData.IntegerValue(scalarInt + 1, null),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = createdValue.id.toString(),
            measureId = null
        )
        val createdValueChild: ObjectPropertyValue = objectService.create(valueChildRequest, username)


        val createdProperty = objectService.findPropertyById(createdPropertyId)
        transaction(db) {
            assertEquals(2, createdProperty.values.size)
        }

        objectService.deleteValue(createdValueChild.id.toString(), username)

        val updatedProperty = objectService.findPropertyById(createdPropertyId)

        transaction(db) {
            assertEquals(1, updatedProperty.values.size)
            assertEquals(createdValue.id, updatedProperty.values[0].identity)
        }

        checkValuesAbsense(listOf(createdValueChild).map { it.id.toString() })
    }

    @Test
    fun softDeleteChildOfValueTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId = objectService.create(propertyRequest, username)

        val scalarInt = 123

        val valueRequest = ValueCreateRequest(
            value = ObjectValueData.IntegerValue(scalarInt, null),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue: ObjectPropertyValue = objectService.create(valueRequest, username)

        val valueChildRequest = ValueCreateRequest(
            value = ObjectValueData.IntegerValue(scalarInt + 1, null),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = createdValue.id.toString(),
            measureId = null
        )
        val createdValueChild: ObjectPropertyValue = objectService.create(valueChildRequest, username)


        val createdProperty = objectService.findPropertyById(createdPropertyId)
        transaction(db) {
            assertEquals(2, createdProperty.values.size)
        }

        objectService.softDeleteValue(createdValueChild.id.toString(), username)

        val updatedProperty = objectService.findPropertyById(createdPropertyId)

        transaction(db) {
            assertEquals(1, updatedProperty.values.size)
            assertEquals(createdValue.id, updatedProperty.values[0].identity)
        }

        checkValuesAbsense(listOf(createdValueChild).map { it.id.toString() })
    }

    @Test
    fun deleteRootValueInternallyLinkedTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId = objectService.create(propertyRequest, username)

        val createdObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        val valueRequest1 = ValueCreateRequest(
            value = ObjectValueData.StringValue(""),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue1: ObjectPropertyValue = objectService.create(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.Link(LinkValueData.ObjectValue(createdValue1.id.toString())),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = createdValue1.id.toString(),
            measureId = null
        )
        val createdValue2: ObjectPropertyValue = objectService.create(valueRequest2, username)


        objectService.deleteValue(createdValue1.id.toString(), username)

        val updatedProperty = objectService.findPropertyById(createdPropertyId)
        transaction(db) {
            assertEquals(0, updatedProperty.values.size)
        }
        checkValuesAbsense(listOf(createdValue1, createdValue2).map { it.id.toString() })
    }

    @Test
    fun softDeleteRootValueInternallyLinkedTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId = objectService.create(propertyRequest, username)

        val createdObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        val valueRequest1 = ValueCreateRequest(
            value = ObjectValueData.StringValue(""),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue1: ObjectPropertyValue = objectService.create(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.Link(LinkValueData.ObjectValue(createdValue1.id.toString())),
            objectPropertyId = createdPropertyId,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = createdValue1.id.toString(),
            measureId = null
        )
        val createdValue2: ObjectPropertyValue = objectService.create(valueRequest2, username)


        objectService.softDeleteValue(createdValue1.id.toString(), username)

        val updatedProperty = objectService.findPropertyById(createdPropertyId)
        transaction(db) {
            assertEquals(0, updatedProperty.values.size)
        }
        checkValuesAbsense(listOf(createdValue1, createdValue2).map { it.id.toString() })
    }

    @Test
    fun deleteRootValueExternallyLinkedTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")

        val propertyName1 = "prop1_$objectName"
        val propertyRequest1 = PropertyCreateRequest(
            name = propertyName1,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId1 = objectService.create(propertyRequest1, username)

        val createdObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        val valueRequest1 = ValueCreateRequest(
            value = ObjectValueData.StringValue("1111"),
            objectPropertyId = createdPropertyId1,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue1: ObjectPropertyValue = objectService.create(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("222"),
            objectPropertyId = createdPropertyId1,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = createdValue1.id.toString(),
            measureId = null
        )
        val createdValue2: ObjectPropertyValue = objectService.create(valueRequest2, username)

        val propertyName2 = "prop2_$objectName"
        val propertyRequest2 = PropertyCreateRequest(
            name = propertyName2,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId2 = objectService.create(propertyRequest2, username)

        val valueRequest3 = ValueCreateRequest(
            value = ObjectValueData.Link(LinkValueData.ObjectValue(createdValue1.id.toString())),
            objectPropertyId = createdPropertyId2,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue3: ObjectPropertyValue = objectService.create(valueRequest3, username)

        try {
            objectService.deleteValue(createdValue1.id.toString(), username)
            fail("no exception thrown")
        } catch (e: ObjectValueIsLinkedException) {

        }

        val updatedProperty1 = objectService.findPropertyById(createdPropertyId1)
        transaction(db) {
            assertEquals(2, updatedProperty1.values.size)
        }
    }

    @Test
    fun softDeleteRootValueExternallyLinkedTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")

        val propertyName1 = "prop1_$objectName"
        val propertyRequest1 = PropertyCreateRequest(
            name = propertyName1,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId1 = objectService.create(propertyRequest1, username)

        val createdObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        val valueRequest1 = ValueCreateRequest(
            value = ObjectValueData.StringValue("1111"),
            objectPropertyId = createdPropertyId1,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue1: ObjectPropertyValue = objectService.create(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("222"),
            objectPropertyId = createdPropertyId1,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = createdValue1.id.toString(),
            measureId = null
        )
        val createdValue2: ObjectPropertyValue = objectService.create(valueRequest2, username)

        val propertyName2 = "prop2_$objectName"
        val propertyRequest2 = PropertyCreateRequest(
            name = propertyName2,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId2 = objectService.create(propertyRequest2, username)

        val valueRequest3 = ValueCreateRequest(
            value = ObjectValueData.Link(LinkValueData.ObjectValue(createdValue1.id.toString())),
            objectPropertyId = createdPropertyId2,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue3: ObjectPropertyValue = objectService.create(valueRequest3, username)

        objectService.softDeleteValue(createdValue1.id.toString(), username)

        val updatedProperty = objectService.findPropertyById(createdPropertyId1)
        transaction(db) {
            assertEquals(0, updatedProperty.values.size)
        }
        checkValuesSoftAbsense(listOf(createdValue1).map { it.id.toString() })
        checkValuesAbsense(listOf(createdValue2).map { it.id.toString() })
    }

    @Test
    fun deleteChildValueExternallyLinkedTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")

        val propertyName1 = "prop1_$objectName"
        val propertyRequest1 = PropertyCreateRequest(
            name = propertyName1,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId1 = objectService.create(propertyRequest1, username)

        val createdObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        val valueRequest1 = ValueCreateRequest(
            value = ObjectValueData.StringValue("1111"),
            objectPropertyId = createdPropertyId1,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue1: ObjectPropertyValue = objectService.create(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("222"),
            objectPropertyId = createdPropertyId1,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = createdValue1.id.toString(),
            measureId = null
        )
        val createdValue2: ObjectPropertyValue = objectService.create(valueRequest2, username)

        val propertyName2 = "prop2_$objectName"
        val propertyRequest2 = PropertyCreateRequest(
            name = propertyName2,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId2 = objectService.create(propertyRequest2, username)

        val valueRequest3 = ValueCreateRequest(
            value = ObjectValueData.Link(LinkValueData.ObjectValue(createdValue2.id.toString())),
            objectPropertyId = createdPropertyId2,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue3: ObjectPropertyValue = objectService.create(valueRequest3, username)

        try {
            objectService.deleteValue(createdValue1.id.toString(), username)
            fail("no exception thrown")
        } catch (e: ObjectValueIsLinkedException) {

        }

        val updatedProperty1 = objectService.findPropertyById(createdPropertyId1)
        transaction(db) {
            assertEquals(2, updatedProperty1.values.size)
        }
    }

    @Test
    fun softDeleteChildValueExternallyLinkedTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")

        val propertyName1 = "prop1_$objectName"
        val propertyRequest1 = PropertyCreateRequest(
            name = propertyName1,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId1 = objectService.create(propertyRequest1, username)

        val createdObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        val valueRequest1 = ValueCreateRequest(
            value = ObjectValueData.StringValue("1111"),
            objectPropertyId = createdPropertyId1,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue1: ObjectPropertyValue = objectService.create(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("222"),
            objectPropertyId = createdPropertyId1,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = createdValue1.id.toString(),
            measureId = null
        )
        val createdValue2: ObjectPropertyValue = objectService.create(valueRequest2, username)

        val propertyName2 = "prop2_$objectName"
        val propertyRequest2 = PropertyCreateRequest(
            name = propertyName2,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId2 = objectService.create(propertyRequest2, username)

        val valueRequest3 = ValueCreateRequest(
            value = ObjectValueData.Link(LinkValueData.ObjectValue(createdValue2.id.toString())),
            objectPropertyId = createdPropertyId2,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue3: ObjectPropertyValue = objectService.create(valueRequest3, username)

        objectService.softDeleteValue(createdValue1.id.toString(), username)

        val updatedProperty1 = objectService.findPropertyById(createdPropertyId1)
        transaction(db) {
            assertEquals(0, updatedProperty1.values.size)
        }

        checkValuesSoftAbsense(listOf(createdValue1, createdValue2).map { it.id.toString() })
    }

    @Test
    fun deletePropWithRootValueExternallyLinkedTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")

        val propertyName1 = "prop1_$objectName"
        val propertyRequest1 = PropertyCreateRequest(
            name = propertyName1,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId1 = objectService.create(propertyRequest1, username)

        val createdObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        val valueRequest1 = ValueCreateRequest(
            value = ObjectValueData.StringValue("1111"),
            objectPropertyId = createdPropertyId1,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue1: ObjectPropertyValue = objectService.create(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("222"),
            objectPropertyId = createdPropertyId1,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = createdValue1.id.toString(),
            measureId = null
        )
        val createdValue2: ObjectPropertyValue = objectService.create(valueRequest2, username)

        val propertyName2 = "prop2_$objectName"
        val propertyRequest2 = PropertyCreateRequest(
            name = propertyName2,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId2 = objectService.create(propertyRequest2, username)

        val valueRequest3 = ValueCreateRequest(
            value = ObjectValueData.Link(LinkValueData.ObjectValue(createdValue1.id.toString())),
            objectPropertyId = createdPropertyId2,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue3: ObjectPropertyValue = objectService.create(valueRequest3, username)

        try {
            objectService.deleteProperty(createdPropertyId1, username)
            fail("no exception thrown")
        } catch (e: ObjectPropertyIsLinkedException) {

        }

        val updatedObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(2, updatedObject.properties.size)
        }
    }

    @Test
    fun softDeletePropWithRootValueExternallyLinkedTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")

        val propertyName1 = "prop1_$objectName"
        val propertyRequest1 = PropertyCreateRequest(
            name = propertyName1,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId1 = objectService.create(propertyRequest1, username)

        val createdObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        val valueRequest1 = ValueCreateRequest(
            value = ObjectValueData.StringValue("1111"),
            objectPropertyId = createdPropertyId1,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue1: ObjectPropertyValue = objectService.create(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("222"),
            objectPropertyId = createdPropertyId1,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = createdValue1.id.toString(),
            measureId = null
        )
        val createdValue2: ObjectPropertyValue = objectService.create(valueRequest2, username)

        val propertyName2 = "prop2_$objectName"
        val propertyRequest2 = PropertyCreateRequest(
            name = propertyName2,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId2 = objectService.create(propertyRequest2, username)

        val valueRequest3 = ValueCreateRequest(
            value = ObjectValueData.Link(LinkValueData.ObjectValue(createdValue1.id.toString())),
            objectPropertyId = createdPropertyId2,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue3: ObjectPropertyValue = objectService.create(valueRequest3, username)

        objectService.softDeleteProperty(createdPropertyId1, username)

        val updatedObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(1, updatedObject.properties.size)
        }
        checkPropertySoftAbsense(createdPropertyId1)
    }

    @Test
    fun deletePropWithChildValueExternallyLinkedTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")

        val propertyName1 = "prop1_$objectName"
        val propertyRequest1 = PropertyCreateRequest(
            name = propertyName1,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId1 = objectService.create(propertyRequest1, username)

        val createdObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        val valueRequest1 = ValueCreateRequest(
            value = ObjectValueData.StringValue("1111"),
            objectPropertyId = createdPropertyId1,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue1: ObjectPropertyValue = objectService.create(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("222"),
            objectPropertyId = createdPropertyId1,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = createdValue1.id.toString(),
            measureId = null
        )
        val createdValue2: ObjectPropertyValue = objectService.create(valueRequest2, username)

        val propertyName2 = "prop2_$objectName"
        val propertyRequest2 = PropertyCreateRequest(
            name = propertyName2,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId2 = objectService.create(propertyRequest2, username)

        val valueRequest3 = ValueCreateRequest(
            value = ObjectValueData.Link(LinkValueData.ObjectValue(createdValue2.id.toString())),
            objectPropertyId = createdPropertyId2,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue3: ObjectPropertyValue = objectService.create(valueRequest3, username)

        try {
            objectService.deleteProperty(createdPropertyId1, username)
            fail("no exception thrown")
        } catch (e: ObjectPropertyIsLinkedException) {

        }

        val updatedObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(2, updatedObject.properties.size)
        }
    }

    @Test
    fun softDeletePropWithChildValueExternallyLinkedTest() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val createdObjectId = objectService.create(objectRequest, "user")

        val propertyName1 = "prop1_$objectName"
        val propertyRequest1 = PropertyCreateRequest(
            name = propertyName1,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId1 = objectService.create(propertyRequest1, username)

        val createdObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        val valueRequest1 = ValueCreateRequest(
            value = ObjectValueData.StringValue("1111"),
            objectPropertyId = createdPropertyId1,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue1: ObjectPropertyValue = objectService.create(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("222"),
            objectPropertyId = createdPropertyId1,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = createdValue1.id.toString(),
            measureId = null
        )
        val createdValue2: ObjectPropertyValue = objectService.create(valueRequest2, username)

        val propertyName2 = "prop2_$objectName"
        val propertyRequest2 = PropertyCreateRequest(
            name = propertyName2,
            objectId = createdObjectId, aspectId = aspect.idStrict()
        )
        val createdPropertyId2 = objectService.create(propertyRequest2, username)

        val valueRequest3 = ValueCreateRequest(
            value = ObjectValueData.Link(LinkValueData.ObjectValue(createdValue2.id.toString())),
            objectPropertyId = createdPropertyId2,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue3: ObjectPropertyValue = objectService.create(valueRequest3, username)

        objectService.softDeleteProperty(createdPropertyId1, username)

        val updatedObject = objectService.findById(createdObjectId)
        transaction(db) {
            assertEquals(1, updatedObject.properties.size)
        }
    }

    @Test
    fun deleteObjectWithRootValueExternallyLinkedTest() {
        val objectName1 = "object"
        val objectDescription1 = "object description"
        val objectRequest1 =
            ObjectCreateRequest(objectName1, objectDescription1, subject.id, subject.version)
        val createdObjectId1 = objectService.create(objectRequest1, "user")

        val objectName2 = "object2"
        val objectDescription2 = "object2 description"
        val objectRequest2 =
            ObjectCreateRequest(objectName2, objectDescription2, subject.id, subject.version)
        val createdObjectId2 = objectService.create(objectRequest2, "user")

        val propertyName1 = "prop1_$objectName1"
        val propertyRequest1 = PropertyCreateRequest(
            name = propertyName1,
            objectId = createdObjectId1, aspectId = aspect.idStrict()
        )
        val createdPropertyId1 = objectService.create(propertyRequest1, username)

        val createdObject1 = objectService.findById(createdObjectId1)
        transaction(db) {
            assertEquals(1, createdObject1.properties.size)
        }

        val valueRequest1 = ValueCreateRequest(
            value = ObjectValueData.StringValue("1111"),
            objectPropertyId = createdPropertyId1,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue1: ObjectPropertyValue = objectService.create(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("222"),
            objectPropertyId = createdPropertyId1,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = createdValue1.id.toString(),
            measureId = null
        )
        val createdValue2: ObjectPropertyValue = objectService.create(valueRequest2, username)

        val propertyName2 = "prop2_$objectName2"
        val propertyRequest2 = PropertyCreateRequest(
            name = propertyName2,
            objectId = createdObjectId2, aspectId = aspect.idStrict()
        )
        val createdPropertyId2 = objectService.create(propertyRequest2, username)

        val valueRequest3 = ValueCreateRequest(
            value = ObjectValueData.Link(LinkValueData.ObjectValue(createdValue1.id.toString())),
            objectPropertyId = createdPropertyId2,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue3: ObjectPropertyValue = objectService.create(valueRequest3, username)

        try {
            objectService.deleteObject(createdObjectId1, username)
            fail("no exception thrown")
        } catch (e: ObjectIsLinkedException) {

        }

        val updatedObject = objectService.findById(createdObjectId1)
        transaction(db) {
            assertEquals(1, updatedObject.properties.size)
        }
    }

    @Test
    fun softDeleteObjectWithRootValueExternallyLinkedTest() {
        val objectName1 = "object"
        val objectDescription1 = "object description"
        val objectRequest1 =
            ObjectCreateRequest(objectName1, objectDescription1, subject.id, subject.version)
        val createdObjectId1 = objectService.create(objectRequest1, "user")

        val objectName2 = "object2"
        val objectDescription2 = "object2 description"
        val objectRequest2 =
            ObjectCreateRequest(objectName2, objectDescription2, subject.id, subject.version)
        val createdObjectId2 = objectService.create(objectRequest2, "user")

        val propertyName1 = "prop1_$objectName1"
        val propertyRequest1 = PropertyCreateRequest(
            name = propertyName1,
            objectId = createdObjectId1, aspectId = aspect.idStrict()
        )
        val createdPropertyId1 = objectService.create(propertyRequest1, username)

        val createdObject1 = objectService.findById(createdObjectId1)
        transaction(db) {
            assertEquals(1, createdObject1.properties.size)
        }

        val valueRequest1 = ValueCreateRequest(
            value = ObjectValueData.StringValue("1111"),
            objectPropertyId = createdPropertyId1,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue1: ObjectPropertyValue = objectService.create(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("222"),
            objectPropertyId = createdPropertyId1,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = createdValue1.id.toString(),
            measureId = null
        )
        val createdValue2: ObjectPropertyValue = objectService.create(valueRequest2, username)

        val propertyName2 = "prop2_$objectName2"
        val propertyRequest2 = PropertyCreateRequest(
            name = propertyName2,
            objectId = createdObjectId2, aspectId = aspect.idStrict()
        )
        val createdPropertyId2 = objectService.create(propertyRequest2, username)

        val valueRequest3 = ValueCreateRequest(
            value = ObjectValueData.Link(LinkValueData.ObjectValue(createdValue1.id.toString())),
            objectPropertyId = createdPropertyId2,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue3: ObjectPropertyValue = objectService.create(valueRequest3, username)

        objectService.softDeleteObject(createdObjectId1, username)

        val updatedObject = objectService.findById(createdObjectId1)
        transaction(db) {
            assertEquals(0, updatedObject.properties.size)
        }
    }

    @Test
    fun deleteObjectWithChildValueExternallyLinkedTest() {
        val objectName1 = "object"
        val objectDescription1 = "object description"
        val objectRequest1 =
            ObjectCreateRequest(objectName1, objectDescription1, subject.id, subject.version)
        val createdObjectId1 = objectService.create(objectRequest1, "user")

        val objectName2 = "object2"
        val objectDescription2 = "object2 description"
        val objectRequest2 =
            ObjectCreateRequest(objectName2, objectDescription2, subject.id, subject.version)
        val createdObjectId2 = objectService.create(objectRequest2, "user")

        val propertyName1 = "prop1_$objectName1"
        val propertyRequest1 = PropertyCreateRequest(
            name = propertyName1,
            objectId = createdObjectId1, aspectId = aspect.idStrict()
        )
        val createdPropertyId1 = objectService.create(propertyRequest1, username)

        val createdObject1 = objectService.findById(createdObjectId1)
        transaction(db) {
            assertEquals(1, createdObject1.properties.size)
        }

        val valueRequest1 = ValueCreateRequest(
            value = ObjectValueData.StringValue("1111"),
            objectPropertyId = createdPropertyId1,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue1: ObjectPropertyValue = objectService.create(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("222"),
            objectPropertyId = createdPropertyId1,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = createdValue1.id.toString(),
            measureId = null
        )
        val createdValue2: ObjectPropertyValue = objectService.create(valueRequest2, username)

        val propertyName2 = "prop2_$objectName2"
        val propertyRequest2 = PropertyCreateRequest(
            name = propertyName2,
            objectId = createdObjectId2, aspectId = aspect.idStrict()
        )
        val createdPropertyId2 = objectService.create(propertyRequest2, username)

        val valueRequest3 = ValueCreateRequest(
            value = ObjectValueData.Link(LinkValueData.ObjectValue(createdValue2.id.toString())),
            objectPropertyId = createdPropertyId2,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = null,
            measureId = null
        )
        val createdValue3: ObjectPropertyValue = objectService.create(valueRequest3, username)

        try {
            objectService.deleteObject(createdObjectId1, username)
            fail("no exception thrown")
        } catch (e: ObjectIsLinkedException) {

        }

        val updatedObject = objectService.findById(createdObjectId1)
        transaction(db) {
            assertEquals(1, updatedObject.properties.size)
        }
    }

}