package com.infowings.catalog.data

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.storage.OrientDatabase
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

    @Test
    fun testAddAspect() {
        val aspectService = AspectService(database)

        //aspectService.createAspect("newAspect", LengthMeasure.name(), null)
    }
}