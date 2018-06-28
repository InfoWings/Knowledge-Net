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
            name = "prop_createObjectWithPropertyTestName", description = null,
            cardinality = PropertyCardinality.INFINITY.name, objectId = createdObjectId, aspectId = aspect.idStrict()
        )

        val createdPropertyId = objectService.create(propertyRequest, username)

        val foundObject = objectService.findById(createdObjectId)
        val foundProperty = objectService.findPropertyById(createdPropertyId)

        assertEquals(propertyRequest.name, foundProperty.name, "name is incorrect")
        assertEquals(propertyRequest.cardinality, foundProperty.cardinality.name, "cardinality is incorrect")

        transaction(db) {
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
            description = null,
            cardinality = PropertyCardinality.INFINITY.name,
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

}