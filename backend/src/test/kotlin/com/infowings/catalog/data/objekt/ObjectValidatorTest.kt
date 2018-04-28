package com.infowings.catalog.data.objekt


import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.BaseType
import com.infowings.catalog.common.ObjectData
import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.data.MeasureService
import com.infowings.catalog.data.Subject
import com.infowings.catalog.data.SubjectNotFoundException
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.Aspect
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.record.impl.OVertexDocument
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

    private val username = "admin"

    @Before
    fun initTestData() {
        validator = ObjectValidator(objectService, subjectService, measureService, aspectService)
        subject = subjectService.createSubject(SubjectData(name = "subjectName", description = "descr"), username)
        aspect = aspectService.save(AspectData(name = "aspectName", description = "aspectDescr", baseType = BaseType.Text.name), username)
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
    fun objectValidatorAbsentSubjectTest() {
        val data = ObjectData(null, "objectValidatorTestName", "object descr", createNonExistentSubjectKey(), emptyList())

        try {
            validator.checkedForCreation(data)
            Assert.fail("Nothing thrown")
        } catch (e: SubjectNotFoundException) {
        } catch (e: Exception) {
            Assert.fail("Unexpected exception: $e")
        }
    }


    private fun createObject(objekt: Objekt): ObjectVertex = transaction(db) {
        val newVertex = dao.newObjectVertex()
        return@transaction dao.saveObject(newVertex, objekt)
    }

    private fun createObject(objectData: ObjectData): ObjectVertex {
        val objekt = validator.checkedForCreation(objectData)
        return createObject(objekt)
    }

    private fun createNonExistentSubjectKey(): String {
        val vertex = transaction(db) {
            val newVertex = db.createNewVertex(SUBJECT_CLASS)
            newVertex.setProperty("name", "non-existent")
            return@transaction newVertex.save<OVertexDocument>()
        }

        val id = vertex.id

        session(db) {
            db.delete(vertex)
        }

        return id
    }
}