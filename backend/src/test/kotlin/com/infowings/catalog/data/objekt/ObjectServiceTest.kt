@file:Suppress("UNUSED_VARIABLE")

package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.*
import com.infowings.catalog.data.Subject
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.randomName
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.id
import com.infowings.catalog.storage.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail


@ExtendWith(SpringExtension::class)
@SpringBootTest
@Suppress("StringLiteralDuplication")
class ObjectServiceTest {
    @Autowired
    private lateinit var db: OrientDatabase
    @Autowired
    private lateinit var subjectService: SubjectService
    @Autowired
    private lateinit var aspectService: AspectService
    @Autowired
    private lateinit var objectService: ObjectService

    private lateinit var subject: Subject

    private lateinit var aspect: AspectData
    private lateinit var referenceAspect: AspectData
    private lateinit var complexAspect: AspectData
    private lateinit var aspectInt: AspectData

    private val username = "admin"
    private val sampleDescription = "object description"

    @BeforeEach
    fun initTestData() {
        subject = subjectService.createSubject(SubjectData(name = randomName(), description = "descr"), username)
        aspect = aspectService.save(AspectData(name = randomName(), description = "aspectDescr", baseType = BaseType.Text.name), username)
        aspectInt = aspectService.save(AspectData(name = randomName(), description = "aspectDescr", baseType = BaseType.Integer.name), username)
        referenceAspect = aspectService.save(
            AspectData(name = randomName(), description = "aspect with reference base type", baseType = BaseType.Reference.name), username
        )
        val property = AspectPropertyData("", "p", aspect.idStrict(), PropertyCardinality.INFINITY.name, null)
        val referenceProperty = AspectPropertyData("", "p", referenceAspect.idStrict(), PropertyCardinality.INFINITY.name, null)
        val complexAspectData = AspectData("", randomName(), Kilometre.name, null, BaseType.Decimal.name, listOf(property, referenceProperty))
        complexAspect = aspectService.save(complexAspectData, username)
    }

    private fun createObject(request: ObjectCreateRequest) = objectService.create(request, username)
    private fun newObject(name: String, subjectId: String) = createObject(ObjectCreateRequest(name, sampleDescription, subjectId))
    private fun newObject(name: String) = newObject(name, subject.id)
    private fun newObject() = newObject(randomName(), subject.id)


    @Test
    fun `Create object`() {
        val name = randomName()
        val objectCreateResponse = newObject(name)

        val objectVertex = objectService.findById(objectCreateResponse.id)
        assertEquals(name, objectVertex.name, "names must be equal")
        assertEquals(sampleDescription, objectVertex.description, "descriptions must be equal")
        transaction(db) {
            assertEquals(subject.id, objectVertex.subject?.id, "subjects must be equal")
        }
    }


    @Test
    fun `Create object with property and default root value`() {
        val objectRequest =
            ObjectCreateRequest("createObjectWithPropertyTestName", "object descr", subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")

        val propertyRequest = PropertyCreateRequest(
            name = "prop_createObjectWithPropertyTestName",
            description = null, objectId = objectCreateResponse.id, aspectId = aspect.idStrict()
        )

        val propertyCreateResponse = objectService.create(propertyRequest, username)

        val foundObject = objectService.findById(objectCreateResponse.id)
        val foundProperty = objectService.findPropertyById(propertyCreateResponse.id)

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
                    propertyCreateResponse.id, foundObject.properties.first().id,
                    "found parent object must contain correct property"
                )
                assertEquals(
                    propertyCreateResponse.id, objectOfProperty.properties.first().id,
                    "updated parent object must contain correct property"
                )
            }
        }
    }

    @Test
    fun `Update object property with default root value`() {
        val objectRequest = ObjectCreateRequest("updateObjectPropertyWithValueTest", null, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, username)

        val propertyRequest = PropertyCreateRequest(
            name = "prop_updateObjectPropertyWithValueTest",
            description = null, objectId = objectCreateResponse.id, aspectId = complexAspect.idStrict()
        )

        val propertyCreateResponse = objectService.create(propertyRequest, username)

        val defaultRootValueId = propertyCreateResponse.rootValue.id

        val childValueRequest = ValueCreateRequest(
            value = ObjectValueData.StringValue("Text Value"),
            description = null,
            objectPropertyId = propertyCreateResponse.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = defaultRootValueId
        )
        val childValueCreateResponse = objectService.create(childValueRequest, username)
        val childValueId = childValueCreateResponse.id

        val propertyUpdateRequest = PropertyUpdateRequest(
            propertyCreateResponse.id,
            "newProp_updateObjectPropertyWithValueTest",
            null,
            childValueCreateResponse.objectProperty.version
        )
        val propertyUpdateResponse = objectService.update(propertyUpdateRequest, username)

        transaction(db) { _ ->
            assertEquals(propertyCreateResponse.id, propertyUpdateResponse.id, "Created and updated property ids are not equal")

            val foundPropertyInDb = objectService.findPropertyById(propertyUpdateResponse.id)

            assertEquals("newProp_updateObjectPropertyWithValueTest", foundPropertyInDb.name, "Updated property has different name than in update request")
            assertEquals(2, foundPropertyInDb.values.size, "Updated property has different amount of values attached")
            val propertyValueIds = foundPropertyInDb.values.map { it.id }
            listOf(defaultRootValueId, childValueId).forEach {
                assertTrue(propertyValueIds.contains(it), "Property values does not contain previously created value")
            }
        }
    }


    @Test
    fun `Create object with property and update root value`() {
        val objectRequest =
            ObjectCreateRequest("createObjectWithValueTestName", "object descr", subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")

        val propertyRequest = PropertyCreateRequest(
            name = "prop_createObjectWithValueTestName",
            description = null,
            objectId = objectCreateResponse.id, aspectId = aspect.idStrict()
        )
        val propertyCreateResponse = objectService.create(propertyRequest, username)

        val strValue = "hello"
        val valueRequest = ValueUpdateRequest(
            valueId = propertyCreateResponse.rootValue.id,
            value = ObjectValueData.StringValue(strValue),
            measureName = null,
            description = null,
            version = propertyCreateResponse.rootValue.version
        )
        val rootValueUpdateResponse = objectService.update(valueRequest, username)

        val objectValue = rootValueUpdateResponse.value.toData()

        when (objectValue) {
            is ObjectValueData.StringValue -> {
                assertEquals(strValue, objectValue.value, "scalar value must be with correct type name")
                assertEquals(
                    propertyCreateResponse.id, rootValueUpdateResponse.objectProperty.id,
                    "object property must point to parent property"
                )
                assertTrue(rootValueUpdateResponse.parentValue == null)
            }
            else ->
                fail("value must be string")
        }

    }

    @Test
    fun `Create ranged integer value`() {
        val objectRequest =
            ObjectCreateRequest(randomName(), "object descr", subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")

        val propertyRequest = PropertyCreateRequest(
            name = randomName(),
            description = null,
            objectId = objectCreateResponse.id, aspectId = aspectInt.idStrict()
        )
        val propertyCreateResponse = objectService.create(propertyRequest, username)

        val lwb = 2
        val upb = 5

        val valueRequest = ValueCreateRequest(
            value = ObjectValueData.IntegerValue(lwb, upb, null),
            description = null,
            measureName = null,
            objectPropertyId = propertyCreateResponse.id
        )
        val response = objectService.create(valueRequest, username)

        val objectValue = response.value.toData()

        when (objectValue) {
            is ObjectValueData.IntegerValue -> {
                assertEquals(lwb, objectValue.value)
                assertEquals(upb, objectValue.upb)
            }
            else ->
                fail("value must be string")
        }

    }

    @Test
    fun `Create ranged decimal value`() {
        val objectRequest =
            ObjectCreateRequest(randomName(), "object descr", subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")

        val propertyRequest = PropertyCreateRequest(
            name = randomName(),
            description = null,
            objectId = objectCreateResponse.id, aspectId = complexAspect.idStrict()
        )
        val propertyCreateResponse = objectService.create(propertyRequest, username)

        val lwb = "123.45"
        val upb = "234.56"

        val valueRequest = ValueCreateRequest(
            value = ObjectValueData.DecimalValue(lwb, upb),
            description = null,
            measureName = Millimetre.name,
            objectPropertyId = propertyCreateResponse.id
        )
        val response = objectService.create(valueRequest, username)

        val objectValue = response.value.toData()

        when (objectValue) {
            is ObjectValueData.DecimalValue -> {
                assertEquals(lwb, objectValue.valueRepr)
                assertEquals(upb, objectValue.upbRepr)
            }
            else ->
                fail("value must be string")
        }

    }

    private fun checkValueAbsence(id: String) = try {
        val found = objectService.findPropertyValueById(id)
        fail("object property value is found after deletion: $found")
    } catch (e: ObjectPropertyValueNotFoundException) {
    }

    private fun checkValuesAbsence(ids: List<String>) = ids.forEach { checkValueAbsence(it) }

    private fun checkValueSoftAbsence(id: String) {
        val found = objectService.findPropertyValueById(id)
        assertTrue(found.deleted)
    }

    private fun checkValuesSoftAbsence(ids: List<String>) {
        ids.forEach { checkValueSoftAbsence(it) }
    }

    private fun checkPropertyAbsence(id: String) = try {
        val found = objectService.findPropertyById(id)
        fail("object property is found after deletion: $found")
    } catch (e: ObjectPropertyNotFoundException) {
    }

    private fun checkPropertiesAbsence(ids: List<String>) = ids.forEach { checkPropertyAbsence(it) }

    private fun checkPropertySoftAbsence(id: String) {
        val found = objectService.findPropertyById(id)
        assertEquals(true, found.deleted)
    }

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
    fun `Delete object`() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest = ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")

        objectService.deleteObject(objectCreateResponse.id, username)

        checkObjectAbsense(objectCreateResponse.id)
    }

    @Test
    fun `Soft delete object`() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest = ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")

        objectService.softDeleteObject(objectCreateResponse.id, username)

        checkObjectAbsense(objectCreateResponse.id)
    }

    @Test
    fun `Delete object with property and default root value`() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest = ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")
        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName,
            description = null,
            objectId = objectCreateResponse.id, aspectId = aspect.idStrict()
        )
        val propertyCreateResponse = objectService.create(propertyRequest, username)

        objectService.deleteObject(objectCreateResponse.id, username)

        checkObjectAbsense(objectCreateResponse.id)
        checkPropertyAbsence(propertyCreateResponse.id)
        checkValueAbsence(propertyCreateResponse.rootValue.id)
    }

    @Test
    fun `Soft delete object with property and default root value`() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest = ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")
        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName,
            description = null,
            objectId = objectCreateResponse.id, aspectId = aspect.idStrict()
        )
        val propertyCreateResponse = objectService.create(propertyRequest, username)

        objectService.softDeleteObject(objectCreateResponse.id, username)

        checkObjectAbsense(objectCreateResponse.id)
        checkPropertyAbsence(propertyCreateResponse.id)
        checkValueAbsence(propertyCreateResponse.rootValue.id)
    }

    @Test
    fun `Delete object with two properties and default root values`() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest = ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")
        val propertyName1 = "prop1_$objectName"
        val propertyName2 = "prop2_$objectName"

        val propertyRequest = PropertyCreateRequest(name = propertyName1, description = null, objectId = objectCreateResponse.id, aspectId = aspect.idStrict())
        val propertyCreateResponse1 = objectService.create(propertyRequest, username)
        val propertyCreateResponse2 = objectService.create(propertyRequest.copy(name = propertyName2), username)

        objectService.deleteObject(objectCreateResponse.id, username)

        checkObjectAbsense(objectCreateResponse.id)
        checkPropertiesAbsence(listOf(propertyCreateResponse1.id, propertyCreateResponse2.id))
        checkValuesAbsence(listOf(propertyCreateResponse1.rootValue.id, propertyCreateResponse2.rootValue.id))
    }

    @Test
    fun `Soft delete object with two properties and default root values`() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest = ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")
        val propertyName1 = "prop1_$objectName"
        val propertyName2 = "prop2_$objectName"

        val propertyRequest = PropertyCreateRequest(name = propertyName1, description = null, objectId = objectCreateResponse.id, aspectId = aspect.idStrict())
        val propertyCreateResponse1 = objectService.create(propertyRequest, username)
        val propertyCreateResponse2 = objectService.create(propertyRequest.copy(name = propertyName2), username)

        objectService.deleteObject(objectCreateResponse.id, username)

        checkObjectAbsense(objectCreateResponse.id)
        checkPropertiesAbsence(listOf(propertyCreateResponse1.id, propertyCreateResponse2.id))
        checkValuesAbsence(listOf(propertyCreateResponse1.rootValue.id, propertyCreateResponse2.rootValue.id))
    }

    @Test
    fun `Delete object with property and updated root value`() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest = ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")
        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(name = propertyName, description = null, objectId = objectCreateResponse.id, aspectId = aspect.idStrict())
        val propertyCreateResponse = objectService.create(propertyRequest, username)

        val valueRequest =
            ValueUpdateRequest(propertyCreateResponse.rootValue.id, ObjectValueData.StringValue("123"), null, null, propertyCreateResponse.rootValue.version)
        val rootValueUpdateResponse = objectService.update(valueRequest, username)

        objectService.deleteObject(objectCreateResponse.id, username)

        checkObjectAbsense(objectCreateResponse.id)
        checkPropertyAbsence(propertyCreateResponse.id)
        checkValueAbsence(rootValueUpdateResponse.id)
    }

    @Test
    fun `Soft delete object with property and updated root value`() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")
        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(name = propertyName, description = null, objectId = objectCreateResponse.id, aspectId = aspect.idStrict())
        val propertyCreateResponse = objectService.create(propertyRequest, username)

        val valueRequest =
            ValueUpdateRequest(propertyCreateResponse.rootValue.id, ObjectValueData.StringValue("123"), null, null, propertyCreateResponse.rootValue.version)
        val rootValueUpdateResponse = objectService.update(valueRequest, username)

        objectService.softDeleteObject(objectCreateResponse.id, username)

        checkObjectAbsense(objectCreateResponse.id)
        checkPropertyAbsence(propertyCreateResponse.id)
        checkValueAbsence(rootValueUpdateResponse.id)
    }

    @Test
    fun `Delete object that is internally linked to itself`() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest = ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")
        val propertyName = "prop_$objectName"

        val propertyRequest =
            PropertyCreateRequest(name = propertyName, description = null, objectId = objectCreateResponse.id, aspectId = referenceAspect.idStrict())
        val propertyCreateResponse = objectService.create(propertyRequest, username)

        val valueRequest = ValueUpdateRequest(
            propertyCreateResponse.rootValue.id,
            ObjectValueData.Link(LinkValueData.Object(objectCreateResponse.id)),
            null,
            null,
            propertyCreateResponse.rootValue.version
        )
        val valueUpdateResponse = objectService.update(valueRequest, username)

        objectService.deleteObject(objectCreateResponse.id, username)

        checkObjectAbsense(objectCreateResponse.id)
        checkPropertyAbsence(propertyCreateResponse.id)
        checkValueAbsence(valueUpdateResponse.id)
    }

    @Test
    fun `Soft delete object that is internally linked to itself`() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest = ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")
        val propertyName = "prop_$objectName"

        val propertyRequest =
            PropertyCreateRequest(name = propertyName, description = null, objectId = objectCreateResponse.id, aspectId = referenceAspect.idStrict())
        val propertyCreateResponse = objectService.create(propertyRequest, username)

        val valueRequest = ValueUpdateRequest(
            propertyCreateResponse.rootValue.id,
            ObjectValueData.Link(LinkValueData.Object(objectCreateResponse.id)),
            null,
            null,
            propertyCreateResponse.version
        )
        val createdValue = objectService.update(valueRequest, username)

        objectService.softDeleteObject(objectCreateResponse.id, username)

        checkObjectAbsense(objectCreateResponse.id)
        checkPropertyAbsence(propertyCreateResponse.id)
        checkValueAbsence(createdValue.id)
    }

    @Test
    fun `Delete object that is externally linked`() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest = ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse1 = objectService.create(objectRequest, "user")

        val objectRequest2 = ObjectCreateRequest("$objectName-2", objectDescription, subject.id)
        val objectCreateResponse2 = objectService.create(objectRequest2, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest =
            PropertyCreateRequest(name = propertyName, description = null, objectId = objectCreateResponse2.id, aspectId = referenceAspect.idStrict())
        val propertyCreateResponse = objectService.create(propertyRequest, username)

        val valueRequest = ValueUpdateRequest(
            propertyCreateResponse.rootValue.id,
            ObjectValueData.Link(LinkValueData.Object(objectCreateResponse1.id)),
            null,
            null,
            propertyCreateResponse.rootValue.version
        )
        val valueUpdateResponse = objectService.update(valueRequest, username)

        try {
            objectService.deleteObject(objectCreateResponse1.id, username)
            fail("Expected exception is not thrown")
        } catch (e: ObjectIsLinkedException) {
        }
    }

    @Test
    fun `Soft delete object that is externally linked`() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest = ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")

        val objectRequest2 = ObjectCreateRequest("$objectName-2", objectDescription, subject.id)
        val objectCreateResponse2 = objectService.create(objectRequest2, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest =
            PropertyCreateRequest(name = propertyName, description = null, objectId = objectCreateResponse2.id, aspectId = referenceAspect.idStrict())
        val propertyCreateResponse = objectService.create(propertyRequest, username)

        val valueRequest = ValueUpdateRequest(
            propertyCreateResponse.rootValue.id,
            ObjectValueData.Link(LinkValueData.Object(objectCreateResponse.id)),
            null,
            null,
            propertyCreateResponse.rootValue.version
        )
        val valueUpdateResponse = objectService.update(valueRequest, username)

        objectService.softDeleteObject(objectCreateResponse.id, username)

        checkObjectSoftAbsense(objectCreateResponse.id)
    }

    @Test
    fun `Delete object with two root values`() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")
        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(name = propertyName, description = null, objectId = objectCreateResponse.id, aspectId = aspect.idStrict())
        val propertyCreateResponse = objectService.create(propertyRequest, username)

        val valueRequest1 =
            ValueUpdateRequest(propertyCreateResponse.rootValue.id, ObjectValueData.StringValue("123"), null, null, propertyCreateResponse.rootValue.version)
        val rootValueUpdateResponse = objectService.update(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(ObjectValueData.StringValue("234"), null, propertyCreateResponse.id)
        val rootValueCreateResponse = objectService.create(valueRequest2, username)


        objectService.deleteObject(objectCreateResponse.id, username)

        checkObjectAbsense(objectCreateResponse.id)
        checkPropertyAbsence(propertyCreateResponse.id)
        checkValuesAbsence(listOf(rootValueUpdateResponse.id, rootValueCreateResponse.id))
    }

    @Test
    fun `Soft delete object with two root values`() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest = ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")
        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(name = propertyName, description = null, objectId = objectCreateResponse.id, aspectId = aspect.idStrict())
        val propertyCreateResponse = objectService.create(propertyRequest, username)

        val valueRequest1 =
            ValueUpdateRequest(propertyCreateResponse.rootValue.id, ObjectValueData.StringValue("123"), null, null, propertyCreateResponse.rootValue.version)
        val valueUpdateResponse = objectService.update(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(ObjectValueData.StringValue("234"), null, propertyCreateResponse.id)
        val valueCreateResponse = objectService.create(valueRequest2, username)


        objectService.softDeleteObject(objectCreateResponse.id, username)

        checkObjectAbsense(objectCreateResponse.id)
        checkPropertyAbsence(propertyCreateResponse.id)
        checkValuesAbsence(listOf(valueUpdateResponse.id, valueCreateResponse.id))
    }

    @Test
    fun `Delete object with child value`() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest = ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")
        val propertyName = "prop_$objectName"

        val propertyRequest =
            PropertyCreateRequest(name = propertyName, description = null, objectId = objectCreateResponse.id, aspectId = complexAspect.idStrict())
        val propertyCreateResponse = objectService.create(propertyRequest, username)

        val valueRequest1 =
            ValueUpdateRequest(
                propertyCreateResponse.rootValue.id,
                ObjectValueData.DecimalValue.single("123"),
                Kilometre.name,
                null,
                propertyCreateResponse.rootValue.version
            )
        val rootValueUpdateResponse = objectService.update(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            ObjectValueData.StringValue("hello"),
            null,
            propertyCreateResponse.id,
            null,
            complexAspect.properties[0].id,
            rootValueUpdateResponse.id
        )
        val childValueCreateResponse = objectService.create(valueRequest2, username)

        objectService.deleteObject(objectCreateResponse.id, username)

        checkObjectAbsense(objectCreateResponse.id)
        checkPropertyAbsence(propertyCreateResponse.id)
        checkValuesAbsence(listOf(rootValueUpdateResponse.id, childValueCreateResponse.id))
    }

    @Test
    fun `Soft delete object with child value`() {
        val objectName = "object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")
        val propertyName = "prop_$objectName"

        val propertyRequest =
            PropertyCreateRequest(name = propertyName, description = null, objectId = objectCreateResponse.id, aspectId = complexAspect.idStrict())
        val propertyCreateResponse = objectService.create(propertyRequest, username)

        val valueRequest1 =
            ValueUpdateRequest(
                propertyCreateResponse.rootValue.id,
                ObjectValueData.DecimalValue.single("123"),
                Kilometre.name,
                null,
                propertyCreateResponse.rootValue.version
            )
        val valueUpdateResponse = objectService.update(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            ObjectValueData.StringValue("hello"),
            null,
            propertyCreateResponse.id,
            null,
            complexAspect.properties[0].id,
            valueUpdateResponse.id
        )
        val childValueCreateResponse = objectService.create(valueRequest2, username)

        objectService.softDeleteObject(objectCreateResponse.id, username)

        checkObjectAbsense(objectCreateResponse.id)
        checkPropertyAbsence(propertyCreateResponse.id)
        checkValuesAbsence(listOf(valueUpdateResponse.id, childValueCreateResponse.id))
    }

    @Test
    fun `Deleted property is reflected on object properties count`() {
        val objectName = "deletePropertyTest-object"
        val objectDescription = "object description"
        val objectRequest = ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName, description = null,
            objectId = objectCreateResponse.id, aspectId = aspect.idStrict()
        )
        val propertyCreateResponse = objectService.create(propertyRequest, username)

        val createdObject = objectService.findById(objectCreateResponse.id)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        objectService.deleteProperty(propertyCreateResponse.id, username)

        val updatedObject = objectService.findById(objectCreateResponse.id)
        transaction(db) {
            assertEquals(0, updatedObject.properties.size)
        }
        checkPropertyAbsence(propertyCreateResponse.id)
    }

    @Test
    fun `Soft deleted property is reflected on object properties count`() {
        val objectName = "softDeletePropertyTest-object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName, description = null,
            objectId = objectCreateResponse.id, aspectId = aspect.idStrict()
        )
        val propertyCreateResponse = objectService.create(propertyRequest, username)

        val createdObject = objectService.findById(objectCreateResponse.id)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        objectService.softDeleteProperty(propertyCreateResponse.id, username)

        val updatedObject = objectService.findById(objectCreateResponse.id)
        transaction(db) {
            assertEquals(0, updatedObject.properties.size)
        }
        checkPropertyAbsence(propertyCreateResponse.id)
    }

    @Test
    fun `Delete property with two root values is reflected on object properties count`() {
        val objectName = "deletePropertyWithTwoRootsTest-object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName, description = null,
            objectId = objectCreateResponse.id, aspectId = aspect.idStrict()
        )
        val propertyCreateResponse = objectService.create(propertyRequest, username)

        val createdObject = objectService.findById(objectCreateResponse.id)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        val valueRequest1 =
            ValueUpdateRequest(propertyCreateResponse.rootValue.id, ObjectValueData.StringValue("hello1"), null, null, propertyCreateResponse.rootValue.version)
        val valueUpdateResponse = objectService.update(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(ObjectValueData.StringValue("hello2"), null, propertyCreateResponse.id)
        val valueCreateRequest = objectService.create(valueRequest2, username)

        objectService.deleteProperty(propertyCreateResponse.id, username)

        val updatedObject = objectService.findById(objectCreateResponse.id)
        transaction(db) {
            assertEquals(0, updatedObject.properties.size)
        }
        checkPropertyAbsence(propertyCreateResponse.id)
        checkValuesAbsence(listOf(valueUpdateResponse.id, valueCreateRequest.id))
    }

    @Test
    fun `Soft delete property with two root values is reflected on object properties count`() {
        val objectName = "softDeletePropertyWithTwoRootsTest-object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName, description = null,
            objectId = objectCreateResponse.id, aspectId = aspect.idStrict()
        )
        val propertyCreateResponse = objectService.create(propertyRequest, username)

        val createdObject = objectService.findById(objectCreateResponse.id)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        val valueRequest1 =
            ValueUpdateRequest(propertyCreateResponse.rootValue.id, ObjectValueData.StringValue("hello1"), null, null, propertyCreateResponse.rootValue.version)
        val valueUpdateResponse = objectService.update(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(ObjectValueData.StringValue("hello2"), null, propertyCreateResponse.id)
        val valueCreateResponse = objectService.create(valueRequest2, username)

        objectService.softDeleteProperty(propertyCreateResponse.id, username)

        val updatedObject = objectService.findById(objectCreateResponse.id)
        transaction(db) {
            assertEquals(0, updatedObject.properties.size)
        }
        checkPropertyAbsence(propertyCreateResponse.id)
        checkValuesAbsence(listOf(valueUpdateResponse.id, valueCreateResponse.id))
    }


    @Test
    fun `Delete property with child value`() {
        val objectName = "deletePropertyWithChildTest-object"
        val objectDescription = "object description"
        val objectRequest = ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName, description = null,
            objectId = objectCreateResponse.id, aspectId = aspect.idStrict()
        )
        val propertyCreateResponse = objectService.create(propertyRequest, username)

        val createdObject = objectService.findById(objectCreateResponse.id)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        val valueRequest1 =
            ValueUpdateRequest(propertyCreateResponse.rootValue.id, ObjectValueData.StringValue("hello1"), null, null, propertyCreateResponse.rootValue.version)
        val valueUpdateResponse = objectService.update(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("hello"),
            description = null,
            objectPropertyId = propertyCreateResponse.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = valueUpdateResponse.id
        )
        val valueCreateResponse = objectService.create(valueRequest2, username)

        objectService.deleteProperty(propertyCreateResponse.id, username)

        val updatedObject = objectService.findById(objectCreateResponse.id)
        transaction(db) {
            assertEquals(0, updatedObject.properties.size)
        }
        checkPropertyAbsence(propertyCreateResponse.id)
        checkValuesAbsence(listOf(valueUpdateResponse.id, valueCreateResponse.id))
    }

    @Test
    fun `Soft delete property with child value`() {
        val objectName = "softDeletePropertyWithChildTest-object"
        val objectDescription = "object description"
        val objectRequest = ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName, description = null,
            objectId = objectCreateResponse.id, aspectId = aspect.idStrict()
        )
        val propertyCreateResponse = objectService.create(propertyRequest, username)

        val createdObject = objectService.findById(objectCreateResponse.id)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        val valueRequest1 =
            ValueUpdateRequest(propertyCreateResponse.rootValue.id, ObjectValueData.StringValue("hello1"), null, null, propertyCreateResponse.rootValue.version)
        val valueUpdateResponse = objectService.update(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("hello"),
            description = null,
            objectPropertyId = propertyCreateResponse.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = valueUpdateResponse.id
        )
        val valueCreateResponse = objectService.create(valueRequest2, username)

        objectService.softDeleteProperty(propertyCreateResponse.id, username)

        val updatedObject = objectService.findById(objectCreateResponse.id)
        transaction(db) {
            assertEquals(0, updatedObject.properties.size)
        }
        checkPropertyAbsence(propertyCreateResponse.id)
        checkValuesAbsence(listOf(valueUpdateResponse.id, valueCreateResponse.id))
    }

    @Test
    fun `Delete property that is internally linked`() {
        val objectName = "deletePropertyInternallyLinkedTest-object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName, description = null,
            objectId = objectCreateResponse.id, aspectId = referenceAspect.idStrict()
        )
        val propertyCreateResponse = objectService.create(propertyRequest, username)

        val createdObject = objectService.findById(objectCreateResponse.id)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        val valueRequest1 = ValueUpdateRequest(
            propertyCreateResponse.rootValue.id,
            ObjectValueData.Link(LinkValueData.ObjectProperty(propertyCreateResponse.id)),
            null,
            null,
            propertyCreateResponse.rootValue.version
        )
        val valueUpdateResponse = objectService.update(valueRequest1, username)

        objectService.deleteProperty(propertyCreateResponse.id, username)

        val updatedObject = objectService.findById(objectCreateResponse.id)
        transaction(db) {
            assertEquals(0, updatedObject.properties.size)
        }
        checkPropertyAbsence(propertyCreateResponse.id)
        checkValueAbsence(valueUpdateResponse.id)
    }

    @Test
    fun `Soft delete property that is internally linked`() {
        val objectName = "softDeletePropertyInternallyLinkedTest-object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName, description = null,
            objectId = objectCreateResponse.id, aspectId = referenceAspect.idStrict()
        )
        val propertyCreateResponse = objectService.create(propertyRequest, username)

        val createdObject = objectService.findById(objectCreateResponse.id)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        val valueRequest1 = ValueUpdateRequest(
            propertyCreateResponse.rootValue.id,
            ObjectValueData.Link(LinkValueData.ObjectProperty(propertyCreateResponse.id)),
            null,
            null,
            propertyCreateResponse.rootValue.version
        )
        val valueUpdateResponse = objectService.update(valueRequest1, username)

        objectService.softDeleteProperty(propertyCreateResponse.id, username)

        val updatedObject = objectService.findById(objectCreateResponse.id)
        transaction(db) {
            assertEquals(0, updatedObject.properties.size)
        }
        checkPropertyAbsence(propertyCreateResponse.id)
        checkValueAbsence(valueUpdateResponse.id)
    }

    @Test
    fun `Delete property that is externally linked`() {
        val objectName = "deletePropertyExternallyLinkedTest-object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")

        val propertyName1 = "prop1_$objectName"
        val propertyRequest1 = PropertyCreateRequest(
            name = propertyName1, description = null,
            objectId = objectCreateResponse.id, aspectId = referenceAspect.idStrict()
        )
        val propertyCreateResponse1 = objectService.create(propertyRequest1, username)

        val propertyName2 = "prop2_$objectName"
        val propertyRequest2 = PropertyCreateRequest(
            name = propertyName2, description = null,
            objectId = objectCreateResponse.id, aspectId = aspect.idStrict()
        )
        val propertyCreateResponse2 = objectService.create(propertyRequest2, username)

        val createdObject = objectService.findById(objectCreateResponse.id)
        transaction(db) {
            assertEquals(2, createdObject.properties.size)
        }

        val valueRequest1 = ValueUpdateRequest(
            propertyCreateResponse1.rootValue.id,
            ObjectValueData.Link(LinkValueData.ObjectProperty(propertyCreateResponse2.id)),
            null,
            null,
            propertyCreateResponse1.rootValue.version
        )
        val valueUpdateResponse = objectService.update(valueRequest1, username)

        try {
            objectService.deleteProperty(propertyCreateResponse2.id, username)
            fail("no exception thrown")
        } catch (e: ObjectPropertyIsLinkedException) {
            assertEquals(propertyCreateResponse2.id, e.propertyId)
        }

        val updatedObject = objectService.findById(objectCreateResponse.id)
        transaction(db) {
            assertEquals(2, updatedObject.properties.size)
        }
    }

    @Test
    fun `Soft delete property that is externally linked`() {
        val objectName = "softDeletePropertyExternallyLinkedTest-object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")

        val propertyName1 = "prop1_$objectName"
        val propertyRequest1 = PropertyCreateRequest(
            name = propertyName1, description = null,
            objectId = objectCreateResponse.id, aspectId = referenceAspect.idStrict()
        )
        val propertyCreateResponse1 = objectService.create(propertyRequest1, username)


        val propertyName2 = "prop2_$objectName"
        val propertyRequest2 = PropertyCreateRequest(
            name = propertyName2, description = null,
            objectId = objectCreateResponse.id, aspectId = aspect.idStrict()
        )
        val propertyCreateResponse2 = objectService.create(propertyRequest2, username)

        val createdObject = objectService.findById(objectCreateResponse.id)
        transaction(db) {
            assertEquals(2, createdObject.properties.size)
        }

        val valueRequest1 = ValueUpdateRequest(
            propertyCreateResponse1.rootValue.id,
            ObjectValueData.Link(LinkValueData.ObjectProperty(propertyCreateResponse2.id)),
            null,
            null,
            propertyCreateResponse1.rootValue.version
        )
        val valueUpdateResponse = objectService.update(valueRequest1, username)

        objectService.softDeleteProperty(propertyCreateResponse2.id, username)

        val updatedObject = objectService.findById(objectCreateResponse.id)
        transaction(db) {
            assertEquals(1, updatedObject.properties.size)
        }
    }

    @Test
    fun `Delete value`() {
        val objectName = "deleteValueTest-object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName, description = null,
            objectId = objectCreateResponse.id, aspectId = aspect.idStrict()
        )
        val propertyCreateResponse = objectService.create(propertyRequest, username)

        val valueRequest =
            ValueUpdateRequest(propertyCreateResponse.rootValue.id, ObjectValueData.StringValue("hello"), null, null, propertyCreateResponse.rootValue.version)
        val valueUpdateResponse = objectService.update(valueRequest, username)

        val createdProperty = objectService.findPropertyById(propertyCreateResponse.id)
        transaction(db) {
            assertEquals(1, createdProperty.values.size)
        }

        objectService.deleteValue(valueUpdateResponse.id, username)

        val updatedProperty = objectService.findPropertyById(propertyCreateResponse.id)

        transaction(db) {
            assertEquals(0, updatedProperty.values.size)
        }

        checkValueAbsence(valueUpdateResponse.id)
    }

    @Test
    fun `Soft delete value`() {
        val objectName = "softDeleteValueTest-object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName, description = null,
            objectId = objectCreateResponse.id, aspectId = aspect.idStrict()
        )
        val propertyCreateResponse = objectService.create(propertyRequest, username)

        val valueRequest =
            ValueUpdateRequest(propertyCreateResponse.rootValue.id, ObjectValueData.StringValue("hello"), null, null, propertyCreateResponse.rootValue.version)
        val valueUpdateResponse = objectService.update(valueRequest, username)

        val createdProperty = objectService.findPropertyById(propertyCreateResponse.id)
        transaction(db) {
            assertEquals(1, createdProperty.values.size)
        }

        objectService.softDeleteValue(valueUpdateResponse.id, username)

        val updatedProperty = objectService.findPropertyById(propertyCreateResponse.id)

        transaction(db) {
            assertEquals(0, updatedProperty.values.size)
        }

        checkValueAbsence(valueUpdateResponse.id)
    }

    @Test
    fun `Delete value with child`() {
        val objectName = "deleteValueWithChildTest-object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName, description = null,
            objectId = objectCreateResponse.id, aspectId = aspect.idStrict()
        )
        val propertyCreateResponse = objectService.create(propertyRequest, username)

        val valueRequest = ValueUpdateRequest(
            propertyCreateResponse.rootValue.id,
            ObjectValueData.StringValue("hello1"),
            null,
            null,
            propertyCreateResponse.rootValue.version
        )
        val valueUpdateResponse = objectService.update(valueRequest, username)

        val valueChildRequest = ValueCreateRequest(
            value = ObjectValueData.StringValue("hello2"),
            description = null,
            objectPropertyId = propertyCreateResponse.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = valueUpdateResponse.id
        )
        val childValueCreateResponse = objectService.create(valueChildRequest, username)

        val createdProperty = objectService.findPropertyById(propertyCreateResponse.id)
        transaction(db) {
            assertEquals(2, createdProperty.values.size)
        }

        objectService.deleteValue(valueUpdateResponse.id, username)

        val updatedProperty = objectService.findPropertyById(propertyCreateResponse.id)

        transaction(db) {
            assertEquals(0, updatedProperty.values.size)
        }

        checkValuesAbsence(listOf(valueUpdateResponse.id, childValueCreateResponse.id))
    }

    @Test
    fun `Soft delete value with child`() {
        val objectName = "softDeleteValueWithChildTest-object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName, description = null,
            objectId = objectCreateResponse.id, aspectId = aspect.idStrict()
        )
        val propertyCreateResponse = objectService.create(propertyRequest, username)

        val valueRequest =
            ValueUpdateRequest(propertyCreateResponse.rootValue.id, ObjectValueData.StringValue("hello1"), null, null, propertyCreateResponse.rootValue.version)
        val valueUpdateResponse = objectService.update(valueRequest, username)

        val valueChildRequest = ValueCreateRequest(
            value = ObjectValueData.StringValue("hello2"),
            description = null,
            objectPropertyId = propertyCreateResponse.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = valueUpdateResponse.id
        )
        val childValueCreateResponse = objectService.create(valueChildRequest, username)

        val createdProperty = objectService.findPropertyById(propertyCreateResponse.id)
        transaction(db) {
            assertEquals(2, createdProperty.values.size)
        }

        objectService.softDeleteValue(valueUpdateResponse.id, username)

        val updatedProperty = objectService.findPropertyById(propertyCreateResponse.id)

        transaction(db) {
            assertEquals(0, updatedProperty.values.size)
        }

        checkValuesAbsence(listOf(valueUpdateResponse.id, childValueCreateResponse.id))
    }

    @Test
    fun `Delete child of value`() {
        val objectName = "deleteChildOfValueTest-object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName, description = null,
            objectId = objectCreateResponse.id, aspectId = aspect.idStrict()
        )
        val propertyCreateResponse = objectService.create(propertyRequest, username)

        val valueRequest =
            ValueUpdateRequest(propertyCreateResponse.rootValue.id, ObjectValueData.StringValue("hello1"), null, null, propertyCreateResponse.rootValue.version)
        val valueUpdateResponse = objectService.update(valueRequest, username)

        val valueChildRequest = ValueCreateRequest(
            value = ObjectValueData.StringValue("hello2"),
            description = null,
            objectPropertyId = propertyCreateResponse.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = valueUpdateResponse.id
        )
        val childValueCreateResponse = objectService.create(valueChildRequest, username)


        val createdProperty = objectService.findPropertyById(propertyCreateResponse.id)
        transaction(db) {
            assertEquals(2, createdProperty.values.size)
        }

        objectService.deleteValue(childValueCreateResponse.id, username)

        val updatedProperty = objectService.findPropertyById(propertyCreateResponse.id)

        transaction(db) {
            assertEquals(1, updatedProperty.values.size)
            assertEquals(valueUpdateResponse.id, updatedProperty.values[0].id)
        }

        checkValuesAbsence(listOf(childValueCreateResponse.id))
    }

    @Test
    fun `Soft delete child of value`() {
        val objectName = "softDeleteChildOfValueTest-object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName, description = null,
            objectId = objectCreateResponse.id, aspectId = aspect.idStrict()
        )
        val propertyCreateResponse = objectService.create(propertyRequest, username)

        val valueRequest = ValueUpdateRequest(
            propertyCreateResponse.rootValue.id,
            ObjectValueData.StringValue("hello1"),
            null,
            null,
            propertyCreateResponse.rootValue.version
        )
        val valueUpdateResponse = objectService.update(valueRequest, username)

        val valueChildRequest = ValueCreateRequest(
            value = ObjectValueData.StringValue("hello2"),
            description = null,
            objectPropertyId = propertyCreateResponse.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = valueUpdateResponse.id
        )
        val childValueCreateResponse = objectService.create(valueChildRequest, username)


        val createdProperty = objectService.findPropertyById(propertyCreateResponse.id)
        transaction(db) {
            assertEquals(2, createdProperty.values.size)
        }

        objectService.softDeleteValue(childValueCreateResponse.id, username)

        val updatedProperty = objectService.findPropertyById(propertyCreateResponse.id)

        transaction(db) {
            assertEquals(1, updatedProperty.values.size)
            assertEquals(valueUpdateResponse.id, updatedProperty.values[0].id)
        }

        checkValuesAbsence(listOf(childValueCreateResponse.id))
    }

    @Test
    fun `Delete root value that is internally linked`() {
        val objectName = "deleteRootValueInternallyLinkedTest-object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName, description = null,
            objectId = objectCreateResponse.id, aspectId = aspect.idStrict()
        )
        val propertyCreateResponse = objectService.create(propertyRequest, username)

        val createdObject = objectService.findById(objectCreateResponse.id)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        val valueRequest1 =
            ValueUpdateRequest(propertyCreateResponse.rootValue.id, ObjectValueData.StringValue(""), null, null, propertyCreateResponse.rootValue.version)
        val valueUpdateResponse = objectService.update(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.Link(LinkValueData.ObjectValue(valueUpdateResponse.id)),
            description = null,
            objectPropertyId = propertyCreateResponse.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[1].id,
            parentValueId = valueUpdateResponse.id
        )
        val valueCreateResponse = objectService.create(valueRequest2, username)


        objectService.deleteValue(valueUpdateResponse.id, username)

        val updatedProperty = objectService.findPropertyById(propertyCreateResponse.id)
        transaction(db) {
            assertEquals(0, updatedProperty.values.size)
        }
        checkValuesAbsence(listOf(valueUpdateResponse.id, valueCreateResponse.id))
    }

    @Test
    fun `Soft delete root value that is internally linked`() {
        val objectName = "softDeleteRootValueInternallyLinkedTest-object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            name = propertyName, description = null,
            objectId = objectCreateResponse.id, aspectId = aspect.idStrict()
        )
        val propertyCreateResponse = objectService.create(propertyRequest, username)

        val createdObject = objectService.findById(objectCreateResponse.id)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        val valueRequest1 =
            ValueUpdateRequest(propertyCreateResponse.rootValue.id, ObjectValueData.StringValue(""), null, null, propertyCreateResponse.rootValue.version)
        val valueUpdateResponse = objectService.update(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.Link(LinkValueData.ObjectValue(valueUpdateResponse.id)),
            description = null,
            objectPropertyId = propertyCreateResponse.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[1].id,
            parentValueId = valueUpdateResponse.id
        )
        val valueCreateResponse = objectService.create(valueRequest2, username)

        objectService.softDeleteValue(valueUpdateResponse.id, username)

        val updatedProperty = objectService.findPropertyById(propertyCreateResponse.id)
        transaction(db) {
            assertEquals(0, updatedProperty.values.size)
        }
        checkValuesAbsence(listOf(valueUpdateResponse.id, valueCreateResponse.id))
    }

    @Test
    fun `Delete root value that is externally linked`() {
        val objectName = "deleteRootValueExternallyLinkedTest-object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")

        val propertyName1 = "prop1_$objectName"
        val propertyRequest1 = PropertyCreateRequest(
            name = propertyName1, description = null,
            objectId = objectCreateResponse.id, aspectId = aspect.idStrict()
        )
        val propertyCreateResponse1 = objectService.create(propertyRequest1, username)

        val createdObject = objectService.findById(objectCreateResponse.id)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        val valueRequest1 = ValueUpdateRequest(
            propertyCreateResponse1.rootValue.id,
            ObjectValueData.StringValue("1111"),
            null,
            null,
            propertyCreateResponse1.rootValue.version
        )
        val valueUpdateResponse1 = objectService.update(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("222"),
            description = null,
            objectPropertyId = propertyCreateResponse1.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = valueUpdateResponse1.id
        )
        val valueCreateResponse = objectService.create(valueRequest2, username)

        val propertyName2 = "prop2_$objectName"
        val propertyRequest2 = PropertyCreateRequest(
            name = propertyName2, description = null,
            objectId = objectCreateResponse.id, aspectId = referenceAspect.idStrict()
        )
        val propertyCreateResponse2 = objectService.create(propertyRequest2, username)

        val valueRequest3 = ValueUpdateRequest(
            propertyCreateResponse2.rootValue.id,
            ObjectValueData.Link(LinkValueData.ObjectValue(valueUpdateResponse1.id)),
            null,
            null,
            propertyCreateResponse2.rootValue.version
        )
        val valueUpdateResponse2 = objectService.update(valueRequest3, username)

        try {
            objectService.deleteValue(valueUpdateResponse1.id, username)
            fail("no exception thrown")
        } catch (e: ObjectValueIsLinkedException) {

        }

        val updatedProperty1 = objectService.findPropertyById(propertyCreateResponse1.id)
        transaction(db) {
            assertEquals(2, updatedProperty1.values.size)
        }
    }

    @Test
    fun `Soft delete root value that is externally linked`() {
        val objectName = "softDeleteRootValueExternallyLinkedTest-object"
        val objectDescription = "object description"
        val objectRequest = ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")

        val propertyName1 = "prop1_$objectName"
        val propertyRequest1 = PropertyCreateRequest(
            name = propertyName1, description = null,
            objectId = objectCreateResponse.id, aspectId = aspect.idStrict()
        )
        val propertyCreateResponse1 = objectService.create(propertyRequest1, username)

        val createdObject = objectService.findById(objectCreateResponse.id)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        val valueRequest1 =
            ValueUpdateRequest(propertyCreateResponse1.rootValue.id, ObjectValueData.StringValue("1111"), null, null, propertyCreateResponse1.rootValue.version)
        val valueUpdateResponse1 = objectService.update(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("222"),
            description = null,
            objectPropertyId = propertyCreateResponse1.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = valueUpdateResponse1.id
        )
        val valueCreateResponse = objectService.create(valueRequest2, username)

        val propertyName2 = "prop2_$objectName"
        val propertyRequest2 = PropertyCreateRequest(
            name = propertyName2, description = null,
            objectId = objectCreateResponse.id, aspectId = referenceAspect.idStrict()
        )
        val propertyCreateResponse2 = objectService.create(propertyRequest2, username)

        val valueRequest3 = ValueUpdateRequest(
            propertyCreateResponse2.rootValue.id,
            ObjectValueData.Link(LinkValueData.ObjectValue(valueUpdateResponse1.id)),
            null,
            null,
            propertyCreateResponse2.rootValue.version
        )
        val valueUpdateResponse2 = objectService.update(valueRequest3, username)

        objectService.softDeleteValue(valueUpdateResponse1.id, username)

        val updatedProperty = objectService.findPropertyById(propertyCreateResponse1.id)
        transaction(db) {
            assertEquals(0, updatedProperty.values.size)
        }
        checkValuesSoftAbsence(listOf(valueUpdateResponse1.id))
        checkValuesAbsence(listOf(valueCreateResponse.id))
    }

    @Test
    fun `Delete child value that is externally linked`() {
        val objectName = "deleteChildValueExternallyLinkedTest-object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")

        val propertyName1 = "prop1_$objectName"
        val propertyRequest1 = PropertyCreateRequest(
            name = propertyName1, description = null,
            objectId = objectCreateResponse.id, aspectId = aspect.idStrict()
        )
        val propertyCreateResponse1 = objectService.create(propertyRequest1, username)

        val createdObject = objectService.findById(objectCreateResponse.id)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        val valueRequest1 =
            ValueUpdateRequest(propertyCreateResponse1.rootValue.id, ObjectValueData.StringValue("1111"), null, null, propertyCreateResponse1.rootValue.version)
        val valueUpdateResponse1 = objectService.update(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("222"),
            description = null,
            objectPropertyId = propertyCreateResponse1.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = valueUpdateResponse1.id
        )
        val valueCreateResponse = objectService.create(valueRequest2, username)

        val propertyName2 = "prop2_$objectName"
        val propertyRequest2 = PropertyCreateRequest(
            name = propertyName2, description = null,
            objectId = objectCreateResponse.id, aspectId = referenceAspect.idStrict()
        )
        val propertyCreateResponse2 = objectService.create(propertyRequest2, username)

        val valueRequest3 = ValueUpdateRequest(
            propertyCreateResponse2.rootValue.id,
            ObjectValueData.Link(LinkValueData.ObjectValue(valueCreateResponse.id)),
            null,
            null,
            propertyCreateResponse2.rootValue.version
        )
        val valueUpdateResponse2 = objectService.update(valueRequest3, username)

        try {
            objectService.deleteValue(valueUpdateResponse1.id, username)
            fail("no exception thrown")
        } catch (e: ObjectValueIsLinkedException) {

        }

        val updatedProperty1 = objectService.findPropertyById(propertyCreateResponse1.id)
        transaction(db) {
            assertEquals(2, updatedProperty1.values.size)
        }
    }

    @Test
    fun softDeleteChildValueExternallyLinkedTest() {
        val objectName = "softDeleteChildValueExternallyLinkedTest-object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")

        val propertyName1 = "prop1_$objectName"
        val propertyRequest1 = PropertyCreateRequest(
            name = propertyName1, description = null,
            objectId = objectCreateResponse.id, aspectId = aspect.idStrict()
        )
        val propertyCreateResponse1 = objectService.create(propertyRequest1, username)

        val createdObject = objectService.findById(objectCreateResponse.id)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        val valueRequest1 =
            ValueUpdateRequest(propertyCreateResponse1.rootValue.id, ObjectValueData.StringValue("1111"), null, null, propertyCreateResponse1.rootValue.version)
        val valueUpdateResponse1 = objectService.update(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("222"),
            description = null,
            objectPropertyId = propertyCreateResponse1.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = valueUpdateResponse1.id
        )
        val valueCreateResponse = objectService.create(valueRequest2, username)

        val propertyName2 = "prop2_$objectName"
        val propertyRequest2 = PropertyCreateRequest(
            name = propertyName2, description = null,
            objectId = objectCreateResponse.id, aspectId = referenceAspect.idStrict()
        )
        val propertyCreateResponse2 = objectService.create(propertyRequest2, username)

        val valueRequest3 = ValueUpdateRequest(
            propertyCreateResponse2.rootValue.id,
            ObjectValueData.Link(LinkValueData.ObjectValue(valueCreateResponse.id)),
            null,
            null,
            propertyCreateResponse2.rootValue.version
        )
        val valueUpdateResponse2 = objectService.update(valueRequest3, username)

        objectService.softDeleteValue(valueUpdateResponse1.id, username)

        val updatedProperty1 = objectService.findPropertyById(propertyCreateResponse1.id)
        transaction(db) {
            assertEquals(0, updatedProperty1.values.size)
        }

        checkValuesSoftAbsence(listOf(valueCreateResponse.id))
    }

    @Test
    fun `Delete property which root value is externally linked`() {
        val objectName = "deletePropWithRootValueExternallyLinkedTest-object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")

        val propertyName1 = "prop1_$objectName"
        val propertyRequest1 = PropertyCreateRequest(
            name = propertyName1, description = null,
            objectId = objectCreateResponse.id, aspectId = aspect.idStrict()
        )
        val propertyCreateResponse1 = objectService.create(propertyRequest1, username)

        val createdObject = objectService.findById(objectCreateResponse.id)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        val valueRequest1 =
            ValueUpdateRequest(propertyCreateResponse1.rootValue.id, ObjectValueData.StringValue("1111"), null, null, propertyCreateResponse1.rootValue.version)
        val valueUpdateResponse1 = objectService.update(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("222"),
            description = null,
            objectPropertyId = propertyCreateResponse1.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = valueUpdateResponse1.id
        )
        val valueCreateResponse = objectService.create(valueRequest2, username)

        val propertyName2 = "prop2_$objectName"
        val propertyRequest2 = PropertyCreateRequest(
            name = propertyName2, description = null,
            objectId = objectCreateResponse.id, aspectId = referenceAspect.idStrict()
        )
        val propertyCreateResponse2 = objectService.create(propertyRequest2, username)

        val valueRequest3 = ValueUpdateRequest(
            propertyCreateResponse2.rootValue.id,
            ObjectValueData.Link(LinkValueData.ObjectValue(valueUpdateResponse1.id)),
            null,
            null,
            propertyCreateResponse2.rootValue.version
        )
        val valueUpdateResponse2 = objectService.update(valueRequest3, username)

        try {
            objectService.deleteProperty(propertyCreateResponse1.id, username)
            fail("no exception thrown")
        } catch (e: ObjectPropertyIsLinkedException) {

        }

        val updatedObject = objectService.findById(objectCreateResponse.id)
        transaction(db) {
            assertEquals(2, updatedObject.properties.size)
        }
    }

    @Test
    fun `Soft delete property which root value is externally linked`() {
        val objectName = "softDeletePropWithRootValueExternallyLinkedTest-object"
        val objectDescription = "object description"
        val objectRequest = ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")

        val propertyName1 = "prop1_$objectName"
        val propertyRequest1 = PropertyCreateRequest(
            name = propertyName1, description = null,
            objectId = objectCreateResponse.id, aspectId = aspect.idStrict()
        )
        val propertyCreateResponse1 = objectService.create(propertyRequest1, username)

        val createdObject = objectService.findById(objectCreateResponse.id)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        val valueRequest1 =
            ValueUpdateRequest(propertyCreateResponse1.rootValue.id, ObjectValueData.StringValue("1111"), null, null, propertyCreateResponse1.rootValue.version)
        val valueUpdateResponse1 = objectService.update(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("222"),
            description = null,
            objectPropertyId = propertyCreateResponse1.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = valueUpdateResponse1.id
        )
        val valueCreateResponse = objectService.create(valueRequest2, username)

        val propertyName2 = "prop2_$objectName"
        val propertyRequest2 = PropertyCreateRequest(
            name = propertyName2, description = null,
            objectId = objectCreateResponse.id, aspectId = referenceAspect.idStrict()
        )
        val propertyCreateResponse2 = objectService.create(propertyRequest2, username)

        val valueRequest3 = ValueUpdateRequest(
            propertyCreateResponse2.rootValue.id,
            ObjectValueData.Link(LinkValueData.ObjectValue(valueUpdateResponse1.id)),
            null,
            null,
            propertyCreateResponse2.rootValue.version
        )
        val valueUpdateResponse2 = objectService.update(valueRequest3, username)

        objectService.softDeleteProperty(propertyCreateResponse1.id, username)

        val updatedObject = objectService.findById(objectCreateResponse.id)
        transaction(db) {
            assertEquals(1, updatedObject.properties.size)
        }
        checkPropertySoftAbsence(propertyCreateResponse1.id)
    }

    @Test
    fun `Delete property which non-root value has is externally linked`() {
        val objectName = "deletePropWithChildValueExternallyLinkedTest-object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")

        val propertyName1 = "prop1_$objectName"
        val propertyRequest1 = PropertyCreateRequest(
            name = propertyName1, description = null,
            objectId = objectCreateResponse.id, aspectId = aspect.idStrict()
        )
        val propertyCreateResponse1 = objectService.create(propertyRequest1, username)

        val createdObject = objectService.findById(objectCreateResponse.id)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        val valueRequest1 =
            ValueUpdateRequest(propertyCreateResponse1.rootValue.id, ObjectValueData.StringValue("1111"), null, null, propertyCreateResponse1.rootValue.version)
        val valueUpdateResponse1 = objectService.update(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("222"),
            description = null,
            objectPropertyId = propertyCreateResponse1.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = valueUpdateResponse1.id
        )
        val valueCreateResponse = objectService.create(valueRequest2, username)

        val propertyName2 = "prop2_$objectName"
        val propertyRequest2 = PropertyCreateRequest(
            name = propertyName2, description = null,
            objectId = objectCreateResponse.id, aspectId = referenceAspect.idStrict()
        )
        val propertyCreateResponse2 = objectService.create(propertyRequest2, username)

        val valueRequest3 = ValueUpdateRequest(
            propertyCreateResponse2.rootValue.id,
            ObjectValueData.Link(LinkValueData.ObjectValue(valueCreateResponse.id)),
            null,
            null,
            propertyCreateResponse2.rootValue.version
        )
        val valueUpdateResponse2 = objectService.update(valueRequest3, username)

        try {
            objectService.deleteProperty(propertyCreateResponse1.id, username)
            fail("no exception thrown")
        } catch (e: ObjectPropertyIsLinkedException) {

        }

        val updatedObject = objectService.findById(objectCreateResponse.id)
        transaction(db) {
            assertEquals(2, updatedObject.properties.size)
        }
    }

    @Test
    fun `Soft delete property which non-root value is externally linked`() {
        val objectName = "softDeletePropWithChildValueExternallyLinkedTest-object"
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")

        val propertyName1 = "prop1_$objectName"
        val propertyRequest1 = PropertyCreateRequest(
            name = propertyName1, description = null,
            objectId = objectCreateResponse.id, aspectId = aspect.idStrict()
        )
        val propertyCreateResponse1 = objectService.create(propertyRequest1, username)

        val createdObject = objectService.findById(objectCreateResponse.id)
        transaction(db) {
            assertEquals(1, createdObject.properties.size)
        }

        val valueRequest1 =
            ValueUpdateRequest(propertyCreateResponse1.rootValue.id, ObjectValueData.StringValue("1111"), null, null, propertyCreateResponse1.rootValue.version)
        val valueUpdateResponse1 = objectService.update(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("222"),
            description = null,
            objectPropertyId = propertyCreateResponse1.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = valueUpdateResponse1.id
        )
        val valueCreateResponse = objectService.create(valueRequest2, username)

        val propertyName2 = "prop2_$objectName"
        val propertyRequest2 = PropertyCreateRequest(
            name = propertyName2, description = null,
            objectId = objectCreateResponse.id, aspectId = referenceAspect.idStrict()
        )
        val propertyCreateResponse2 = objectService.create(propertyRequest2, username)

        val valueRequest3 = ValueUpdateRequest(
            propertyCreateResponse2.rootValue.id,
            ObjectValueData.Link(LinkValueData.ObjectValue(valueCreateResponse.id)),
            null,
            null,
            propertyCreateResponse2.rootValue.version
        )
        val valueUpdateResponse2 = objectService.update(valueRequest3, username)

        objectService.softDeleteProperty(propertyCreateResponse1.id, username)

        val updatedObject = objectService.findById(objectCreateResponse.id)
        transaction(db) {
            assertEquals(1, updatedObject.properties.size)
        }
    }


    @Test
    fun `Delete object whose root value is externally linked`() {
        val objectName1 = "deleteObjectWithRootValueExternallyLinkedTest-object"
        val objectDescription1 = "object description"
        val objectRequest1 =
            ObjectCreateRequest(objectName1, objectDescription1, subject.id)
        val objectCreateResponse1 = objectService.create(objectRequest1, "user")

        val objectName2 = "object2"
        val objectDescription2 = "object2 description"
        val objectRequest2 =
            ObjectCreateRequest(objectName2, objectDescription2, subject.id)
        val objectCreateResponse2 = objectService.create(objectRequest2, "user")

        val propertyName1 = "prop1_$objectName1"
        val propertyRequest1 = PropertyCreateRequest(
            name = propertyName1, description = null,
            objectId = objectCreateResponse1.id, aspectId = aspect.idStrict()
        )
        val propertyCreateResponse1 = objectService.create(propertyRequest1, username)

        val createdObject1 = objectService.findById(objectCreateResponse1.id)
        transaction(db) {
            assertEquals(1, createdObject1.properties.size)
        }

        val valueRequest1 =
            ValueUpdateRequest(propertyCreateResponse1.rootValue.id, ObjectValueData.StringValue("1111"), null, null, propertyCreateResponse1.rootValue.version)
        val valueUpdateResponse1 = objectService.update(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("222"),
            description = null,
            objectPropertyId = propertyCreateResponse1.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = valueUpdateResponse1.id
        )
        val valueCreateResponse = objectService.create(valueRequest2, username)

        val propertyName2 = "prop2_$objectName2"
        val propertyRequest2 = PropertyCreateRequest(
            name = propertyName2, description = null,
            objectId = objectCreateResponse2.id, aspectId = referenceAspect.idStrict()
        )
        val propertyCreateResponse2 = objectService.create(propertyRequest2, username)

        val valueRequest3 = ValueUpdateRequest(
            propertyCreateResponse2.rootValue.id,
            ObjectValueData.Link(LinkValueData.ObjectValue(valueUpdateResponse1.id)),
            null,
            null,
            propertyCreateResponse2.rootValue.version
        )
        val valueUpdateResponse2 = objectService.update(valueRequest3, username)

        try {
            objectService.deleteObject(objectCreateResponse1.id, username)
            fail("no exception thrown")
        } catch (e: ObjectIsLinkedException) {

        }

        val updatedObject = objectService.findById(objectCreateResponse1.id)
        transaction(db) {
            assertEquals(1, updatedObject.properties.size)
        }
    }

    @Test
    fun `Soft delete object which root value is externally linked`() {
        val objectName1 = "softDeleteObjectWithRootValueExternallyLinkedTest-object"
        val objectDescription1 = "object description"
        val objectRequest1 =
            ObjectCreateRequest(objectName1, objectDescription1, subject.id)
        val objectCreateResponse1 = objectService.create(objectRequest1, "user")

        val objectName2 = "object2"
        val objectDescription2 = "object2 description"
        val objectRequest2 =
            ObjectCreateRequest(objectName2, objectDescription2, subject.id)
        val objectCreateResponse2 = objectService.create(objectRequest2, "user")

        val propertyName1 = "prop1_$objectName1"
        val propertyRequest1 = PropertyCreateRequest(
            name = propertyName1, description = null,
            objectId = objectCreateResponse1.id, aspectId = aspect.idStrict()
        )
        val propertyCreateResponse1 = objectService.create(propertyRequest1, username)

        val createdObject1 = objectService.findById(objectCreateResponse1.id)
        transaction(db) {
            assertEquals(1, createdObject1.properties.size)
        }

        val valueRequest1 =
            ValueUpdateRequest(propertyCreateResponse1.rootValue.id, ObjectValueData.StringValue("1111"), null, null, propertyCreateResponse1.rootValue.version)
        val valueUpdateResponse1 = objectService.update(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("222"),
            description = null,
            objectPropertyId = propertyCreateResponse1.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = valueUpdateResponse1.id
        )
        val valueCreateResponse = objectService.create(valueRequest2, username)

        val propertyName2 = "prop2_$objectName2"
        val propertyRequest2 = PropertyCreateRequest(
            name = propertyName2, description = null,
            objectId = objectCreateResponse2.id, aspectId = referenceAspect.idStrict()
        )
        val propertyCreateResponse2 = objectService.create(propertyRequest2, username)

        val valueRequest3 = ValueUpdateRequest(
            propertyCreateResponse2.rootValue.id,
            ObjectValueData.Link(LinkValueData.ObjectValue(valueUpdateResponse1.id)),
            null,
            null,
            propertyCreateResponse2.rootValue.version
        )
        val valueUpdateResponse2 = objectService.update(valueRequest3, username)

        objectService.softDeleteObject(objectCreateResponse1.id, username)

        val updatedObject = objectService.findById(objectCreateResponse1.id)
        transaction(db) {
            assertEquals(0, updatedObject.properties.size)
        }
    }

    @Test
    fun `Delete object which non-root value is externally linked`() {
        val objectName1 = "deleteObjectWithChildValueExternallyLinkedTest-object"
        val objectDescription1 = "object description"
        val objectRequest1 =
            ObjectCreateRequest(objectName1, objectDescription1, subject.id)
        val objectCreateResponse1 = objectService.create(objectRequest1, "user")

        val objectName2 = "object2"
        val objectDescription2 = "object2 description"
        val objectRequest2 =
            ObjectCreateRequest(objectName2, objectDescription2, subject.id)
        val objectCreateResponse2 = objectService.create(objectRequest2, "user")

        val propertyName1 = "prop1_$objectName1"
        val propertyRequest1 = PropertyCreateRequest(
            name = propertyName1, description = null,
            objectId = objectCreateResponse1.id, aspectId = aspect.idStrict()
        )
        val propertyCreateResponse1 = objectService.create(propertyRequest1, username)

        val createdObject1 = objectService.findById(objectCreateResponse1.id)
        transaction(db) {
            assertEquals(1, createdObject1.properties.size)
        }

        val valueRequest1 =
            ValueUpdateRequest(propertyCreateResponse1.rootValue.id, ObjectValueData.StringValue("1111"), null, null, propertyCreateResponse1.rootValue.version)
        val valueUpdateResponse1 = objectService.update(valueRequest1, username)

        val valueRequest2 = ValueCreateRequest(
            value = ObjectValueData.StringValue("222"),
            description = null,
            objectPropertyId = propertyCreateResponse1.id,
            measureName = null,
            aspectPropertyId = complexAspect.properties[0].id,
            parentValueId = valueUpdateResponse1.id
        )
        val valueCreateResponse = objectService.create(valueRequest2, username)

        val propertyName2 = "prop2_$objectName2"
        val propertyRequest2 = PropertyCreateRequest(
            name = propertyName2, description = null,
            objectId = objectCreateResponse2.id, aspectId = referenceAspect.idStrict()
        )
        val propertyCreateResponse2 = objectService.create(propertyRequest2, username)

        val valueRequest3 = ValueUpdateRequest(
            propertyCreateResponse2.rootValue.id,
            ObjectValueData.Link(LinkValueData.ObjectValue(valueCreateResponse.id)),
            null,
            null,
            propertyCreateResponse2.rootValue.version
        )
        val valueUpdateResponse2 = objectService.update(valueRequest3, username)

        try {
            objectService.deleteObject(objectCreateResponse1.id, username)
            fail("no exception thrown")
        } catch (e: ObjectIsLinkedException) {

        }

        val updatedObject = objectService.findById(objectCreateResponse1.id)
        transaction(db) {
            assertEquals(1, updatedObject.properties.size)
        }
    }

    @Test
    fun `Delete object with internally linked value`() {
        val objectName = randomName()
        val objectDescription = "object description"
        val objectRequest =
            ObjectCreateRequest(objectName, objectDescription, subject.id)
        val objectCreateResponse = objectService.create(objectRequest, "user")

        val propertyNameDec = "dec_prop"

        val propertyRequestDec = PropertyCreateRequest(
            name = propertyNameDec, description = null,
            objectId = objectCreateResponse.id, aspectId = complexAspect.idStrict()
        )
        val propertyCreateResponseDec = objectService.create(propertyRequestDec, username)

        val valueRequestDec = ValueUpdateRequest(
            valueId = propertyCreateResponseDec.rootValue.id,
            value = ObjectValueData.DecimalValue.single("12.12"),
            measureName = Millimetre.name,
            description = null,
            version = propertyCreateResponseDec.rootValue.version
        )
        val valueUpdateResponseDec = objectService.update(valueRequestDec, username)

        val propertyNameLink = "link_prop"

        val propertyRequestLink = PropertyCreateRequest(
            name = propertyNameLink, description = null,
            objectId = objectCreateResponse.id, aspectId = referenceAspect.idStrict()
        )
        val propertyCreateResponseLink = objectService.create(propertyRequestLink, username)

        val valueRequestLink = ValueUpdateRequest(
            valueId = propertyCreateResponseLink.rootValue.id,
            value = ObjectValueData.Link(LinkValueData.ObjectValue(propertyCreateResponseDec.rootValue.id)),
            measureName = null,
            description = null,
            version = propertyCreateResponseLink.rootValue.version
        )
        val valueUpdateResponseLink = objectService.update(valueRequestLink, username)

        objectService.deleteObject(objectCreateResponse.id, username)

        try {
            objectService.findById(objectCreateResponse.id)
            fail("Nothing thrown")
        } catch (e: ObjectNotFoundException) {
        }
    }

}
