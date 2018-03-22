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

        aspectService.remove(aspect)
        thrown.expect(AspectDoesNotExist::class.java)
        aspectService.remove(aspect)
    }

    @Test
    fun testDeleteAspectWithProperty() {
        val aspectData = initialAspectData("ASPECT_DWP")
        val aspect = aspectService.save(aspectData)


        val aspectProperty = AspectPropertyData("", "prop1", aspect.id, AspectPropertyCardinality.INFINITY.name)
        val aspectData2 = initialAspectData("ANOTHER_ASPECT_DWP" , listOf(aspectProperty))
        aspectService.save(aspectData2)

        thrown.expect(AspectHasLinkedEntitiesException::class.java)
        aspectService.remove(aspectService.findById(aspect.id))

    }

    @Test
    fun testDeleteAspectWithCM() {
        val aspectData = initialAspectData("ASPECT_CM")
        val aspect = aspectService.save(aspectData)

        thrown.expect(AspectConcurrentModificationException::class.java)
        aspectService.remove(aspect.copy(version = 5))
    }
}