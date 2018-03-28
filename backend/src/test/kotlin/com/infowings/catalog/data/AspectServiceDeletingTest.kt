package com.infowings.catalog.data

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import com.infowings.catalog.common.Kilometre
import com.infowings.catalog.common.Metre
import com.infowings.catalog.data.aspect.*
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.session
import com.orientechnologies.orient.core.record.OVertex
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.core.Is
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

    @Autowired
    lateinit var aspectService: AspectService

    @Autowired
    lateinit var database: OrientDatabase

    private lateinit var initialAspect: Aspect

    @Before
    fun saveAspectAndRemoveIt() {
        val ad = AspectData("", "aspect1", Metre.name, null, null)
        initialAspect = aspectService.save(ad)
        session(database) {
            val aspectVertex = database.getVertexById(initialAspect.id)!!.toAspectVertex()
            aspectVertex.deleted = true
            return@session aspectVertex.save<OVertex>()
        }
    }

    @Test
    fun testAddSameNameAfterRemoving() {
        val ad = AspectData("", "aspect1", Metre.name, null, null)
        val aspect = aspectService.save(ad)
        assertThat("Returned aspect should have different id", aspect.id, Is.`is`(Matchers.not(initialAspect.id)))
    }

    @Test(expected = AspectModificationException::class)
    fun testEditingAfterRemoving() {
        aspectService.save(initialAspect.toAspectData())
    }

    @Test(expected = AspectDoesNotExist::class)
    fun testCreatePropertyLinksToRemoved() {
        val p1 = AspectPropertyData("", "", initialAspect.id, AspectPropertyCardinality.ONE.name)
        val ad = AspectData("", "aspect1", Metre.name, null, null, listOf(p1))
        val aspect = aspectService.save(ad)
    }

    private fun initialAspectData(name: String, properties: List<AspectPropertyData> = emptyList()) =
            AspectData(id = "", version = 0, baseType = null,
                    domain = null, measure = Kilometre.name, name = name, properties = properties)

    @get:Rule
    val thrown = ExpectedException.none()

    @Test
    fun testDeleteStandaloneAspect() {
        val aspectData = initialAspectData("SOME_ASPECT")
        val aspect = aspectService.save(aspectData)

        aspectService.remove(aspect.toAspectData(), "")
        thrown.expect(AspectDoesNotExist::class.java)
        aspectService.remove(aspect.toAspectData(), "")
    }

    @Test
    fun testDeleteAspectWithProperty() {
        val aspectData = initialAspectData("ASPECT_DWP")
        val aspect = aspectService.save(aspectData)


        val aspectProperty = AspectPropertyData("", "prop1", aspect.id, AspectPropertyCardinality.INFINITY.name)
        val aspectData2 = initialAspectData("ANOTHER_ASPECT_DWP" , listOf(aspectProperty))
        aspectService.save(aspectData2)

        thrown.expect(AspectHasLinkedEntitiesException::class.java)
        aspectService.remove(aspectService.findById(aspect.id).toAspectData(), "")

    }

    @Test
    fun testDeleteAspectWithCM() {
        val aspectData = initialAspectData("ASPECT_CM")
        val aspect = aspectService.save(aspectData)

        thrown.expect(AspectConcurrentModificationException::class.java)
        aspectService.remove(aspect.copy(version = 5).toAspectData(), "")
    }

    @Test
    fun testDeleteSimpleAspect() {
        val aspect = initialAspectData("A1")
        val saved = aspectService.save(aspect)
        aspectService.remove(saved.toAspectData(), "")

        assertThat(
            "There are no aspect instance in db",
            database.getVertexById(saved.id),
            Is.`is`(Matchers.nullValue())
        )
    }

    @Test
    fun testDeleteLinkedAspect() {
        var a1 = aspectService.save(initialAspectData("a1"))
        val p1 = AspectPropertyData("", "", a1.id, AspectPropertyCardinality.ONE.name)
        val ad = AspectData("", "aspect1", Metre.name, null, null, listOf(p1))
        val saved = aspectService.save(ad)

        a1 = aspectService.findById(a1.id)

        thrown.expect(AspectHasLinkedEntitiesException::class.java)
        aspectService.remove(a1.toAspectData(), "")

        val found = database.getVertexById(a1.id)
        assertThat("Aspect exists in db", found, Is.`is`(Matchers.nullValue()))
        assertThat("Aspect not deleted", found!!.getProperty<String>("deleted"), Is.`is`(Matchers.nullValue()))

        a1 = aspectService.findById(a1.id)
        aspectService.remove(a1.toAspectData(), "", true)
        val found2 = database.getVertexById(a1.id)?.toAspectVertex()
        assertThat("Aspect exists in db", found2, Is.`is`(Matchers.not(null)))
        assertThat("Aspect not deleted", found2!!.deleted, Is.`is`(true))
    }
}