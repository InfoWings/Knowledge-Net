package com.infowings.catalog.data.objekt

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.*
import com.infowings.catalog.data.MeasureService
import com.infowings.catalog.data.Subject
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.Aspect
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.reference.book.ReferenceBookService
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
    private lateinit var aspectService: AspectService
    @Autowired
    private lateinit var measureService: MeasureService
    @Autowired
    private lateinit var objectService: ObjectService
    @Autowired
    private lateinit var refBookService: ReferenceBookService

    private lateinit var validator: ObjectValidator

    private lateinit var subject: Subject

    private lateinit var aspect: Aspect

    private lateinit var complexAspect: Aspect

    private val username = "admin"

    @Before
    fun initTestData() {
        validator = ObjectValidator(objectService, subjectService, measureService, refBookService, aspectService)
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
    fun saveTest() {
        val data = ObjectData(null, "saveTestName", "object descr", subject.id, emptyList())
        val objekt = validator.checkedForCreation(data)
        val saved = createObject(objekt)

        assertEquals(data.name, saved.name, "names must be equal")
        assertEquals(data.description, saved.description, "descriptions must be equal")
        assertTrue("cluster id must be non-negative: ${saved.identity}", saved.identity.clusterId >= 0)
        assertTrue("cluster position must be non-negative: ${saved.identity}", saved.identity.clusterPosition >= 0)
    }

    @Test
    fun saveTwiceTest() {
        val data1 = ObjectData(null, "saveTwiceName-1", "object descr-1", subject.id, emptyList())
        val data2 = ObjectData(null, "saveTwiceName-2", "object descr-2", subject.id, emptyList())

        val objekt1 = validator.checkedForCreation(data1)
        val saved1 = createObject(objekt1)

        val objekt2 = validator.checkedForCreation(data2)
        val saved2 = createObject(objekt2)

        assert(saved1.id != saved2.id)
    }

    @Test
    fun savePropertyTest() {
        val objectData = ObjectData(null, "savePropertyTestObjectName", "some descr", subject.id, emptyList())
        val obj = createObject(objectData)
        val propertyData = ObjectPropertyData(
            null,
            "savePropertyTestObjectPropertyName",
            PropertyCardinality.ONE,
            obj.id, aspect.id, emptyList())


        val objectProperty = validator.checkedForCreation(propertyData)
        val saved = createObjectProperty(objectProperty)

        val updatedObject = objectProperty.objekt
        val foundObject = objectService.findById(obj.id)

        assertEquals(objectProperty.name, saved.name, "names must be equal")
        assertEquals(objectProperty.objekt.id, obj.id, "objekt must point to parent object")
        transaction(db) {
            assertEquals(1, updatedObject.properties.size, "updated object must contain one property")
            assertEquals(1, foundObject.properties.size, "found object must contain one property")

            assertEquals(updatedObject.properties.first().id, saved.id, "updated object's property must link to new one")
            assertEquals(foundObject.properties.first().id, saved.id, "found object's property must link to new one")
        }
    }

    @Test
    fun savePropertySimpleIntValueTest() {
        val objectData = ObjectData(null, "savePropertySimpleIntValueTest", "some descr", subject.id, emptyList())
        val savedObject = createObject(objectData)
        val propertyData = ObjectPropertyData(
            null,
            "savePropertySimpleIntValueTest",
            PropertyCardinality.ONE,
            savedObject.id, aspect.id, emptyList())


        val objectProperty: ObjectProperty = validator.checkedForCreation(propertyData)
        val savedProperty = createObjectProperty(objectProperty)

        val intType = "size"

        val propertyValueData = ObjectPropertyValueData(
            null,
            ObjectValueData.Scalar(ScalarValue.IntegerValue(123), null, null),
            savedProperty.id,
            complexAspect.id,
            null,
            null
        )
        val objectPropertyValue = validator.checkedForCreation(propertyValueData)
        val savedValue = createObjectPropertyValue(objectPropertyValue)

        assertEquals(ScalarTypeTag.INTEGER, savedValue.typeTag, "type tag must be integer")
        assertNotNull(savedValue.intValue, "int value must be non-null")
        assertTrue("str type must be non-null", savedValue.strValue == null)
        assertTrue("compound type must be non-null", savedValue.compoundValue == null)

        val updatedObjectProperty = transaction(db) {savedValue.objectProperty}
        val foundObjectProperty = objectService.findPropertyById(savedProperty.id)

        if (updatedObjectProperty != null) {
            transaction(db) {
                assertEquals(updatedObjectProperty.id, savedValue.objectProperty?.id, "object property id must point to parent")
                assertEquals(foundObjectProperty.id, savedValue.objectProperty?.id, "object property id must point to found property")

                assertEquals(1, updatedObjectProperty.values.size, "property must contain 1 value")
                assertEquals(1, foundObjectProperty.values.size, "property must contain 1 value")
            }
        } else {
            fail("object property is null")
        }
    }

    @Test
    fun savePropertySimpleStrValueTest() {
        val objectData = ObjectData(null, "savePropertySimpleStrValueTest", "some descr", subject.id, emptyList())
        val savedObject = createObject(objectData)
        val propertyData = ObjectPropertyData(
            null,
            "savePropertySimpleStrValueTest",
            PropertyCardinality.ONE,
            savedObject.id, aspect.id, emptyList())


        val objectProperty: ObjectProperty = validator.checkedForCreation(propertyData)
        val savedProperty = createObjectProperty(objectProperty)

        val typeName = "type-tag"

        val propertyValueData = ObjectPropertyValueData(
            null,
            ObjectValueData.Scalar(ScalarValue.StringValue("some value"), null, null),
            savedProperty.id,
            complexAspect.id,
            null,
            null
        )
        val objectPropertyValue = validator.checkedForCreation(propertyValueData)
        val savedValue = createObjectPropertyValue(objectPropertyValue)

        assertEquals(ScalarTypeTag.STRING, savedValue.typeTag, "type tag must be string")
        assertNotNull(savedValue.strValue, "str value must be non-null")
        assertEquals("some value", savedValue.strValue, "str value must be correct")
        assertTrue("int type must be non-null", savedValue.intValue == null)
        assertTrue("compound type must be non-null", savedValue.compoundValue == null)
    }

    private fun createObject(objekt: Objekt): ObjectVertex = transaction(db) {
        val newVertex = dao.newObjectVertex()
        return@transaction dao.saveObject(newVertex, objekt)
    }

    private fun createObject(objectData: ObjectData): ObjectVertex {
        val objekt = validator.checkedForCreation(objectData)
        return createObject(objekt)
    }

    private fun createObjectProperty(propertyData: ObjectProperty): ObjectPropertyVertex = transaction(db) {
        val newVertex = dao.newObjectPropertyVertex()
        return@transaction dao.saveObjectProperty(newVertex, propertyData)
    }

    private fun createObjectPropertyValue(valueData: ObjectPropertyValue): ObjectPropertyValueVertex = transaction(db) {
        val newVertex = dao.newObjectValueVertex()
        return@transaction dao.saveObjectValue(newVertex, valueData)
    }
}