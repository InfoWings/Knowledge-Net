package com.infowings.catalog.data

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.transaction
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
    fun testAddAspectProperty() {
        val aspectService = AspectService(database, measureService)

        val createAspect: Aspect = aspectService.createAspect("newAspect", Kilometre.name, BaseType.Decimal.name)

        val aspectProperty = AspectProperty("", "property", createAspect, AspectPropertyPower.INFINITY)

        val saved: String = transaction(database) { session ->
            return@transaction aspectService.saveAspectProperty(aspectProperty, session)
        }.identity.toString()

        val loaded = transaction(database) { session -> aspectService.loadAspectProperty(saved, session) }

        assertThat("aspect property should be saved and restored", loaded.id, Is.`is`(saved))
    }


}


