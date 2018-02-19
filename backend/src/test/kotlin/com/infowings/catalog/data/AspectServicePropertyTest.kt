package com.infowings.catalog.data

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.common.catalog.data.AspectData
import com.infowings.common.catalog.data.AspectPropertyData
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
class AspectServicePropertyTest {
    @Autowired
    lateinit var database: OrientDatabase
    @Autowired
    lateinit var measureService: MeasureService

    @Test
    fun testAddAspectProperties() {
        val aspectService = AspectService(database, measureService)

        val ad = AspectData("", "base", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val createAspect: Aspect = aspectService.createAspect(ad)

        val property = AspectPropertyData("", "p", createAspect.id, AspectPropertyPower.INFINITY.name)

        val ad2 = AspectData("", "complex", Kilometre.name, null, BaseType.Decimal.name, listOf(property))
        val createAspect2: Aspect = aspectService.createAspect(ad2)

        val loaded = aspectService.findById(createAspect2.id)

        assertThat("aspect property should be saved and restored", loaded, Is.`is`(createAspect2))

        val all = aspectService.getAspects()
        assertThat("There should be 2 aspects in db", all.size, Is.`is`(2))
    }


}


