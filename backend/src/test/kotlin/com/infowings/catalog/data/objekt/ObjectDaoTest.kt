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

    private lateinit var validator: ObjectValidator

    private lateinit var subject: Subject

    private lateinit var aspect: Aspect

    private val username = "admin"

    @Before
    fun initTestData() {
        validator = ObjectValidator(objectService, subjectService, measureService, aspectService)
        subject = subjectService.createSubject(SubjectData(name = "subjectName", description = "descr"), username)
        aspect = aspectService.save(AspectData(name = "aspectName", description = "aspectDescr", baseType = BaseType.Text.name), username)
    }

    @Test
    fun saveTest() {
        val data = ObjectData(null, "saveTestName", "object descr", subject.id, emptyList())
        val objekt = validator.checkedForCreation(data)
        val saved = createObject(objekt)
        assertEquals(data.name, saved.name, "names mist be equal")
        assertEquals(data.description, saved.description, "descriptions mist be equal")
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
        assertEquals(objectProperty.name, saved.name, "names mist be equal")
    }

    @Test
    fun savePropertySimpleValueTest() {
        val objectData = ObjectData(null, "savePropertySimpleValueTest", "some descr", subject.id, emptyList())
        val obj = createObject(objectData)
        val propertyData = ObjectPropertyData(
            null,
            "savePropertySimpleValueTest",
            PropertyCardinality.ONE,
            obj.id, aspect.id, emptyList())


        val objectProperty = validator.checkedForCreation(propertyData)
        val saved = createObjectProperty(objectProperty)
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