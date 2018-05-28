package com.infowings.catalog.data.objekt

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.*
import com.infowings.catalog.data.MeasureService
import com.infowings.catalog.data.Subject
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.Aspect
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

    private lateinit var aspect: Aspect

    private lateinit var complexAspect: Aspect

    private val username = "admin"

    @Before
    fun initTestData() {
        subject = subjectService.createSubject(SubjectData(name = "subjectName", description = "descr"), username)
        aspect = aspectService.save(
            AspectData(name = "aspectName", description = "aspectDescr", baseType = BaseType.Text.name), username
        )
        val property = AspectPropertyData("", "p", aspect.id, PropertyCardinality.INFINITY.name, null)
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
        val data = ObjectData(null, "createObjectTestName", "object descr", subject.id, emptyList())
        val saved = objectService.create(data, "user")

        assertTrue(saved.id != null)
        assertEquals(data.name, saved.name, "names must be equal")
        assertEquals(data.description, saved.description, "descriptions must be equal")
        assertEquals(data.subjectId, saved.subject.id, "subjects must be equal")
    }

    @Test
    fun createObjectWithPropertyTest() {
        val data = ObjectData(null, "createObjectWithPropertyTestName", "object descr", subject.id, emptyList())
        val savedObject = objectService.create(data, "user")

        val savedObjectId = savedObject.id?.toString() ?: fail("saved object has null id")

        val objectPropertyData = ObjectPropertyData(
            null, name = "prop_createObjectWithPropertyTestName",
            cardinality = PropertyCardinality.INFINITY, objectId = savedObjectId, aspectId = aspect.id,
            valueIds = emptyList()
        )

        val savedProperty = objectService.create(objectPropertyData, username)
        val foundObject = objectService.findById(savedObjectId)
        val updatedObject = savedProperty.objekt

        assertTrue(savedProperty.id != null)
        assertEquals(objectPropertyData.name, savedProperty.name, "name is incorrect")
        assertEquals(objectPropertyData.cardinality, savedProperty.cardinality, "cardinality is incorrect")
        assertEquals(objectPropertyData.aspectId, savedProperty.aspect.id, "aspect id is incorrect is incorrect")
        assertEquals(objectPropertyData.objectId, savedProperty.objekt.id, "object id is incorrect is incorrect")
        transaction(db) {
            assertEquals(1, foundObject.properties.size, "found parent object must contain 1 property")
            assertEquals(1, updatedObject.properties.size, "updated parent object must contain 1 property")
            assertEquals(
                savedProperty.id, foundObject.properties.first().identity,
                "found parent object must contain correct property"
            )
            assertEquals(
                savedProperty.id, updatedObject.properties.first().identity,
                "found parent object must contain correct property"
            )
        }
    }

    @Test
    fun createObjectWithValueTest() {
        val data = ObjectData(null, "createObjectWithValueTestName", "object descr", subject.id, emptyList())
        val savedObject = objectService.create(data, "user")

        val savedObjectId = savedObject.id?.toString() ?: fail("saved object has null id")

        val objectPropertyData = ObjectPropertyData(
            null, name = "prop_createObjectWithValueTestName",
            cardinality = PropertyCardinality.INFINITY, objectId = savedObjectId, aspectId = aspect.id,
            valueIds = emptyList()
        )

        val savedProperty = objectService.create(objectPropertyData, username)

        val savedObjectPropertyId = savedProperty.id?.toString() ?: fail("saved object property has null id")

        val typeName = "size"
        val scalarInt = 123

        val valueData = ObjectPropertyValueData(
            null,
            ObjectValueData.IntegerValue(scalarInt, null),
            savedObjectPropertyId, complexAspect.properties[0].id,
            null,
            null
        )

        val savedValue: ObjectPropertyValue = objectService.create(valueData, username)
        val objectValue = savedValue.value

        when (objectValue) {
            is ObjectValue.IntegerValue -> {
                assertEquals(scalarInt, objectValue.value, "scalar value must be with correct type name")
                assertEquals(
                    valueData.objectPropertyId, savedValue.objectProperty.id,
                    "object property must point to parent property"
                )
                assertEquals(
                    valueData.aspectPropertyId, savedValue.aspectProperty.id,
                    "object property must point to proper root characteristic"
                )
                assertTrue(valueData.parentValueId == null)

            }
            else ->
                fail("value must be integer")
        }
    }
}