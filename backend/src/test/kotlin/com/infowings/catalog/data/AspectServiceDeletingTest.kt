package com.infowings.catalog.data

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import com.infowings.catalog.common.Kilometre
import com.infowings.catalog.common.Metre
import com.infowings.catalog.data.aspect.*
import com.infowings.catalog.data.reference.book.ReferenceBookDao
import com.infowings.catalog.data.reference.book.ReferenceBookService
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.session
import com.orientechnologies.orient.core.record.OVertex
import org.apache.coyote.http11.Constants.a
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.core.Is
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AspectServiceDeletingTest {
    private val userName = "admin"

    @Autowired
    lateinit var aspectService: AspectService

    @Autowired
    lateinit var database: OrientDatabase

    @Autowired
    lateinit var referenceBookService: ReferenceBookService

    @Autowired
    lateinit var referenceBookDao: ReferenceBookDao

    private lateinit var initialAspect: Aspect

    @Before
    fun saveAspectAndRemoveIt() {
        val ad = AspectData(null, "aspect1", Metre.name, null, null)
        initialAspect = aspectService.save(ad, userName)
        session(database) {
            val aspectVertex = database.getVertexById(initialAspect.id)!!.toAspectVertex()
            aspectVertex.deleted = true
            return@session aspectVertex.save<OVertex>()
        }
    }

    @Test
    fun testAddSameNameAfterRemoving() {
        val ad = AspectData(null, "aspect1", Metre.name, null, null)
        aspectService.remove(aspectService.findById(initialAspect.id).toAspectData(), userName, true)
        val aspect = aspectService.save(ad, userName)
        assertThat("Returned aspect should have different id", aspect.id, Is.`is`(Matchers.not(initialAspect.id)))
    }

    @Test(expected = AspectModificationException::class)
    fun testEditingAfterRemoving() {
        aspectService.save(initialAspect.toAspectData(), userName)
    }

    @Test(expected = AspectDoesNotExist::class)
    fun testCreatePropertyLinksToRemoved() {
        val p1 = AspectPropertyData("", "", initialAspect.id, AspectPropertyCardinality.ONE.name)
        val ad = AspectData("", "aspect1", Metre.name, null, null, listOf(p1))
        aspectService.save(ad, userName)
    }

    private fun initialAspectData(name: String, properties: List<AspectPropertyData> = emptyList()) =
        AspectData(
            id = "", name = name, measure = Kilometre.name,
            domain = null, baseType = null, properties = properties, version = 0
        )

    @get:Rule
    val thrown = ExpectedException.none()

    @Test
    fun testDeleteStandaloneAspect() {
        val aspectData = initialAspectData("SOME_ASPECT")
        val aspect = aspectService.save(aspectData, userName)

        aspectService.remove(aspect.toAspectData(), userName)
        thrown.expect(AspectDoesNotExist::class.java)
        aspectService.remove(aspect.toAspectData(), userName)
    }

    @Test
    fun testDeleteAspectWithProperty() {
        val aspectData = initialAspectData("ASPECT_DWP")
        val aspect = aspectService.save(aspectData, userName)


        val aspectProperty = AspectPropertyData("", "prop1", aspect.id, AspectPropertyCardinality.INFINITY.name)
        val aspectData2 = initialAspectData("ANOTHER_ASPECT_DWP", listOf(aspectProperty))
        aspectService.save(aspectData2, userName)

        thrown.expect(AspectHasLinkedEntitiesException::class.java)
        aspectService.remove(aspectService.findById(aspect.id).toAspectData(), "")
    }

    @Test
    fun testDeleteAspectWithCM() {
        val aspectData = initialAspectData("ASPECT_CM")
        val aspect = aspectService.save(aspectData, userName)

        thrown.expect(AspectConcurrentModificationException::class.java)
        aspectService.remove(aspect.copy(version = 5).toAspectData(), "")
    }

    @Test
    fun testDeleteSimpleAspect() {
        val aspect = initialAspectData("A1")
        val saved = aspectService.save(aspect, userName)
        aspectService.remove(saved.toAspectData(), userName)

        assertThat(
            "There are no aspect instance in db",
            database.getVertexById(saved.id),
            Is.`is`(Matchers.nullValue())
        )
    }

    @Test
    fun testDeleteLinkedAspect() {
        var a1 = aspectService.save(initialAspectData("a1"), userName)
        val p1 = AspectPropertyData("", "", a1.id, AspectPropertyCardinality.ONE.name)
        val ad = AspectData("", "aspectLinked", Metre.name, null, null, listOf(p1))
        aspectService.save(ad, userName)

        a1 = aspectService.findById(a1.id)

        thrown.expect(AspectHasLinkedEntitiesException::class.java)
        aspectService.remove(a1.toAspectData(), "")

        val found = database.getVertexById(a1.id)
        assertNotNull("Aspect exists in db", found)
        assertNull("Aspect not deleted", found!!.getProperty<String>("deleted"))

        a1 = aspectService.findById(a1.id)
        aspectService.remove(a1.toAspectData(), "", true)
        val found2 = database.getVertexById(a1.id)?.toAspectVertex()
        assertNotNull("Aspect exists in db", found2)
        assertTrue("Aspect deleted", found2!!.deleted)
    }

    @Test
    fun testDeleteAspectWithRefBook() {
        var aspect = aspectService.save(initialAspectData("aspect"), userName)
        referenceBookService.createReferenceBook("book", aspect.id, userName)

        aspect = aspectService.findById(aspect.id)
        aspectService.remove(aspect.toAspectData(), userName)

        val foundAspectVertex = database.getVertexById(aspect.id)?.toAspectVertex()
        assertNull("Aspect not exists in db", foundAspectVertex)

        val foundBookVertex = referenceBookDao.getReferenceBookVertex(aspect.id)
        assertNull("RefBook not exists in db", foundBookVertex)
    }

    @Test
    fun testDeleteAspectProperty() {
        val simpleAspect1 = aspectService.save(initialAspectData("simpleAspect1"), userName)
        val simpleAspect2 = aspectService.save(initialAspectData("simpleAspect2"), userName)
        val property1 = AspectPropertyData("", "", simpleAspect1.id, AspectPropertyCardinality.ONE.name)
        val property2 = AspectPropertyData("", "", simpleAspect2.id, AspectPropertyCardinality.ONE.name)
        val initial = aspectService.save(initialAspectData("aspectData", listOf(property1, property2)), userName)
        val initialAspectData = initial.toAspectData()

        val propertyRemoved = aspectService.save(
            initialAspectData.copy(
                properties = listOf(
                    initialAspectData.properties[0],
                    initialAspectData.properties[1].copy(deleted = true)
                )
            ),
            userName
        )

        assertThat("Updated aspect does not have deleted property", propertyRemoved.properties.size == 1)
    }
}