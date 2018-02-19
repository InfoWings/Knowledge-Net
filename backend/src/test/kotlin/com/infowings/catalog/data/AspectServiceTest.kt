package com.infowings.catalog.data

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.common.catalog.data.AspectData
import org.hamcrest.core.Is
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AspectServiceTest {
    @Autowired
    lateinit var database: OrientDatabase
    @Autowired
    lateinit var measureService: MeasureService

    @Test
    fun testAddAspect() {
        val aspectService = AspectService(database, measureService)
        val ad = AspectData("", "newAspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val createAspect: Aspect = aspectService.createAspect(ad)

        assertThat("aspect should be saved and restored", aspectService.findByName("newAspect"), Is.`is`(createAspect))
    }

    @Test
    fun testAddAspectWithEmptyParams() {
        val aspectService = AspectService(database, measureService)
        val ad = AspectData("", "newAspect", null, null, BaseType.Decimal.name, emptyList())

        val createAspect: Aspect = aspectService.createAspect(ad)

        assertThat("aspect should be saved and restored event when some params are missing", aspectService.findByName("newAspect"), Is.`is`(createAspect))
    }

    @Test
    fun testAddAspectWithEmptyParams2() {
        val aspectService = AspectService(database, measureService)
        val ad = AspectData("", "newAspect", null, null, null, emptyList())

        val createAspect: Aspect = aspectService.createAspect(ad)

        assertThat("aspect should be saved and restored event when some params are missing", aspectService.findByName("newAspect"), Is.`is`(createAspect))
    }

    @Test
    fun testAddAspectWithEmptyParams3() {
        val aspectService = AspectService(database, measureService)
        val ad = AspectData("", "newAspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())

        val createAspect: Aspect = aspectService.createAspect(ad)

        assertThat("aspect should be saved and restored event when some params are missing", aspectService.findByName("newAspect"), Is.`is`(createAspect))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testFailAddAspect() {
        val aspectService = AspectService(database, measureService)
        val ad = AspectData("", "newAspect", Kilometre.name, OpenDomain(BaseType.Boolean).toString(), BaseType.Boolean.name, emptyList())

        aspectService.createAspect(ad)
    }
}