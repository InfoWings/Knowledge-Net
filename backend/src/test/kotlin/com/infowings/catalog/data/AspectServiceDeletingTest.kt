package com.infowings.catalog.data

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.Metre
import com.infowings.catalog.data.aspect.Aspect
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.aspect.toAspectVertex
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.session
import com.orientechnologies.orient.core.record.OVertex
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.core.Is
import org.junit.Before
import org.junit.Test
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
}