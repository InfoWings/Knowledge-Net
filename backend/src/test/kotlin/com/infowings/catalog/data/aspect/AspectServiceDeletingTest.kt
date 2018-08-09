package com.infowings.catalog.data.aspect

import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.ObjectCreateRequest
import com.infowings.catalog.common.objekt.PropertyCreateRequest
import com.infowings.catalog.common.objekt.ValueCreateRequest
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.objekt.ObjectService
import com.infowings.catalog.data.reference.book.ReferenceBookDao
import com.infowings.catalog.data.reference.book.ReferenceBookService
import com.infowings.catalog.randomName
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.session
import com.orientechnologies.orient.core.record.OVertex
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.core.Is
import org.junit.Assert.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest
class AspectServiceDeletingTest {
    private val username = "admin"

    @Autowired
    lateinit var aspectService: AspectService

    @Autowired
    lateinit var database: OrientDatabase

    @Autowired
    lateinit var referenceBookService: ReferenceBookService

    @Autowired
    lateinit var referenceBookDao: ReferenceBookDao

    @Autowired
    lateinit var subjectService: SubjectService

    @Autowired
    lateinit var objectService: ObjectService

    private lateinit var initialAspect: AspectData
    private lateinit var initialAspectId: String

    @BeforeEach
    fun saveAspectAndRemoveIt() {
        val ad = AspectData(null, "saveAspectAndRemoveIt", Metre.name, null, null)
        initialAspect = aspectService.save(ad, username)
        initialAspectId = initialAspect.id ?: throw IllegalStateException("initial aspect hash no id")
        session(database) {
            val aspectVertex = database.getVertexById(initialAspectId)!!.toAspectVertex()
            aspectVertex.deleted = true
            return@session aspectVertex.save<OVertex>()
        }
    }

    @Test
    fun testAddSameNameAfterRemoving() {
        val ad = AspectData(null, "testAddSameNameAfterRemoving", Metre.name, null, null)
        aspectService.remove(aspectService.findById(initialAspectId), username, true)
        val aspect = aspectService.save(ad, username)
        assertThat("Returned aspect should have different id", aspect.id, Is.`is`(Matchers.not(initialAspect.id)))
    }

    @Test
    fun testEditingAfterRemoving() {
        assertThrows<AspectModificationException> {
            aspectService.save(initialAspect, username)
        }
    }

    @Test
    fun testCreatePropertyLinksToRemoved() {
        val p1 = AspectPropertyData("", "", initialAspectId, PropertyCardinality.ONE.name, null)
        val ad = AspectData("", "testCreatePropertyLinksToRemoved", Metre.name, null, null, listOf(p1))
        assertThrows<AspectDoesNotExist> {
            aspectService.save(ad, username)
        }
    }

    private fun initialAspectData(name: String, properties: List<AspectPropertyData> = emptyList()) =
        AspectData(
            id = "", name = name, measure = Kilometre.name,
            domain = null, baseType = null, properties = properties, version = 0
        )

    private fun initialRefAspectData(name: String, properties: List<AspectPropertyData> = emptyList()) =
        AspectData(
            id = "", name = name, measure = null,
            domain = null, baseType = BaseType.Reference.name, properties = properties, version = 0
        )

    private fun initialAspectDataForRefBook(name: String, properties: List<AspectPropertyData> = emptyList()) =
        AspectData(
            id = "", name = name, measure = null,
            domain = null, baseType = BaseType.Text.name, properties = properties, version = 0
        )

    @Test
    fun testDeleteStandaloneAspect() {
        val aspectData = initialAspectData("testDeleteStandaloneAspect")
        val aspect = aspectService.save(aspectData, username)

        aspectService.remove(aspect, username)
        assertThrows<AspectDoesNotExist> {
            aspectService.remove(aspect, username)
        }
    }

    @Test
    fun testDeleteAspectWithProperty() {
        val aspectData = initialAspectData("testDeleteAspectWithProperty-ASPECT_DWP")
        val aspect = aspectService.save(aspectData, username)

        val aspectId = aspect.idStrict()
        val aspectProperty = AspectPropertyData("", "prop1", aspectId, PropertyCardinality.INFINITY.name, null)
        val aspectData2 = initialAspectData("testDeleteAspectWithProperty-ANOTHER_ASPECT_DWP", listOf(aspectProperty))
        aspectService.save(aspectData2, username)

        assertThrows<AspectHasLinkedEntitiesException> {
            aspectService.remove(aspectService.findById(aspectId), username)
        }
    }

    @Test
    fun testDeleteAspectWithObjectValue() {
        val aspectData = initialRefAspectData("testDeleteAspectWithObjectValue-ASPECT-1")
        val aspect = aspectService.save(aspectData, username)

        val aspectId = aspect.idStrict()

        val aspect2 = aspectService.save(initialAspectData("testDeleteAspectWithObjectValue-ASPECT-2"), username)

        val subject = subjectService.createSubject(SubjectData(name = "subject", description = null), username)
        val objectId = objectService.create(ObjectCreateRequest("obj", null, subject.id, subject.version), username)
        val propId = objectService.create(PropertyCreateRequest(objectId, "prop", null, aspectId), username)
        val objValue = objectService.create(ValueCreateRequest(ObjectValueData.Link(LinkValueData.Aspect(aspect2.idStrict())), null, propId), username)

        assertThrows<AspectHasLinkedEntitiesException> {
            aspectService.remove(aspectService.findById(aspect2.idStrict()), username)
        }
    }

    @Test
    fun testDeleteAspectWithCM() {
        val aspectData = initialAspectData("testDeleteAspectWithCM-ASPECT_CM")
        val aspect = aspectService.save(aspectData, username)

        assertThrows<AspectConcurrentModificationException> {
            aspectService.remove(aspect.copy(version = 5), username)
        }
    }

    @Test
    fun testDeleteSimpleAspect() {
        val aspect = initialAspectData("testDeleteSimpleAspect")
        val saved = aspectService.save(aspect, username)
        aspectService.remove(saved, username)

        val savedId = saved.id ?: throw IllegalStateException("no id of saved aspect")

        assertThat(
            "There are no aspect instance in db",
            database.getVertexById(savedId),
            Is.`is`(Matchers.nullValue())
        )
    }

    @Test
    @Disabled
    fun testDeleteLinkedByAspect() {
        var aspect = aspectService.save(initialAspectData("testDeleteLinkedByAspect"), username)
        var aspectId = aspect.id ?: throw IllegalStateException("No id for aspect testDeleteLinkedByAspect")
        val p1 = AspectPropertyData("", "", aspectId, PropertyCardinality.ONE.name, null)
        val ad = AspectData("", "testDeleteLinkedByAspect-aspectLinked", Metre.name, null, null, listOf(p1))
        aspectService.save(ad, username)

        aspect = aspectService.findById(aspectId)
        aspectId = aspect.id ?: throw IllegalStateException("No id for aspect testDeleteLinkedByAspect")

        assertThrows<AspectHasLinkedEntitiesException> {
            aspectService.remove(aspect, username)
        }

        val found: OVertex = database.getVertexById(aspectId) ?: throw IllegalStateException("Aspect should exist")
        assertNull("Aspect not deleted", found!!.getProperty<String>("deleted"))

        aspect = aspectService.findById(aspectId)
        aspectId = aspect.id ?: throw IllegalStateException("No id for aspect testDeleteLinkedByAspect")

        aspectService.remove(aspect, username, true)
        val found2 = database.getVertexById(aspectId)?.toAspectVertex()
        assertNotNull("Aspect exists in db", found2)
        assertTrue("Aspect deleted", found2!!.deleted)
    }

    @Test
    fun `Aspect must not be deleted if has linked value`() {
        val aspectData = AspectData("", randomName(), Second.name, null, BaseType.Decimal.name, emptyList())
        val leafAspect = aspectService.save(aspectData, username)
        val ap2 =
            AspectPropertyData(
                name = "testDeleteHasValue1",
                cardinality = PropertyCardinality.ONE.name,
                aspectId = leafAspect.idStrict(),
                id = "",
                description = ""
            )
        val ap3 =
            AspectPropertyData(
                name = "testDeleteHasValue2",
                cardinality = PropertyCardinality.ONE.name,
                aspectId = leafAspect.idStrict(),
                id = "",
                description = ""
            )
        val ad3 = AspectData("", randomName(), Kilometre.name, null, BaseType.Decimal.name, listOf(ap2, ap3))
        val aspectWithObjectProperty = aspectService.save(ad3, username)

        val subject = subjectService.createSubject(SubjectData(name = "testDeleteHasValueSubject", description = null), username)
        val obj = objectService.create(ObjectCreateRequest("obj", null, subject.id, subject.version), username)
        objectService.create(PropertyCreateRequest(obj, "prop", null, aspectWithObjectProperty.id!!), username)


        assertThrows<AspectHasLinkedEntitiesException> {
            aspectService.remove(aspectWithObjectProperty.copy(version = aspectWithObjectProperty.version + 1), username, force = false)
        }

        assertThrows<AspectConcurrentModificationException> {
            aspectService.remove(aspectWithObjectProperty, username, true)
        }

        val deletedAspect = aspectService.findById(aspectWithObjectProperty.idStrict())
        assertFalse("Found aspect must not be deleted", deletedAspect.deleted)
    }

    @Test
    fun testDeleteAspectWithRefBook() {
        var aspect = aspectService.save(initialAspectDataForRefBook("testDeleteAspectWithRefBook"), username)
        var aspectId = aspect.id ?: throw IllegalStateException("No id for aspect id")

        referenceBookService.createReferenceBook("book", aspectId, username)

        aspect = aspectService.findById(aspectId)
        aspectId = aspect.id ?: throw IllegalStateException("No id for aspect id")

        aspectService.remove(aspect, username)

        val foundAspectVertex = database.getVertexById(aspectId)?.toAspectVertex()
        assertNull("Aspect not exists in db", foundAspectVertex)

        val foundBookVertex = referenceBookDao.getRootVertex(aspectId)
        assertNull("RefBook not exists in db", foundBookVertex)
    }

    @Test
    fun testDeleteAspectProperty() {
        val simpleAspect1 = aspectService.save(initialAspectData("simpleAspect1"), username)
        val simpleAspect2 = aspectService.save(initialAspectData("simpleAspect2"), username)
        val id1 = simpleAspect1.id ?: throw IllegalArgumentException("no id for aspect 1")
        val id2 = simpleAspect2.id ?: throw IllegalArgumentException("no id for aspect 2")
        val property1 = AspectPropertyData("", "", id1, PropertyCardinality.ONE.name, null)
        val property2 = AspectPropertyData("", "", id2, PropertyCardinality.ONE.name, null)
        val initial = aspectService.save(initialAspectData("aspectData", listOf(property1, property2)), username)

        val propertyRemoved = aspectService.save(
            initial.copy(
                properties = listOf(
                    initial.properties[0],
                    initial.properties[1].copy(deleted = true)
                )
            ),
            username
        )

        assertThat("Updated aspect does not have deleted property", propertyRemoved.properties.size == 1)
    }

    @Test
    fun testDeleteAspectPropertyWithObjectValue() {
        val simpleAspect1 = aspectService.save(initialAspectData("testDeleteAspectPropertyWithObjectValue1"), username)
        val simpleAspect2 = aspectService.save(initialAspectData("testDeleteAspectPropertyWithObjectValue2"), username)
        val id1 = simpleAspect1.id ?: throw IllegalArgumentException("no id for aspect 1")
        val id2 = simpleAspect2.id ?: throw IllegalArgumentException("no id for aspect 2")
        val property1 = AspectPropertyData("", "prop-0", id1, PropertyCardinality.ONE.name, null)
        val property2 = AspectPropertyData("", "prop-1", id2, PropertyCardinality.ONE.name, null)
        val initial = aspectService.save(initialAspectData("testDeleteAspectPropertyWithObjectValue", listOf(property1, property2)), username)

        val aspectData = initialRefAspectData("testDeleteAspectPropertyWithObjectValue-ASPECT-1")
        val aspect = aspectService.save(aspectData, username)
        val aspectId = aspect.idStrict()

        val subject = subjectService.createSubject(SubjectData(name = "testDeleteAspectPropertyWithObjectValue-subject", description = null), username)
        val objectId = objectService.create(ObjectCreateRequest("obj", null, subject.id, subject.version), username)
        val propId = objectService.create(PropertyCreateRequest(objectId, "prop", null, aspectId), username)
        val objValue =
            objectService.create(ValueCreateRequest(ObjectValueData.Link(LinkValueData.AspectProperty(initial.properties[0].id)), null, propId), username)

        val current = aspectService.findById(initial.idStrict())

        val afterRemoval = aspectService.save(current.copy(properties = current.properties.map { it.copy(deleted = true) }), username)

        assertEquals(1, afterRemoval.properties.size)
        assertEquals(true, afterRemoval.properties[0].deleted)
        assertEquals("prop-0", afterRemoval.properties[0].name)
    }
}