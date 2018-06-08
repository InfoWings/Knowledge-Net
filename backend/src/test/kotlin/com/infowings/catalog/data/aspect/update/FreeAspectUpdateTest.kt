package com.infowings.catalog.data.aspect.update

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.BaseType
import com.infowings.catalog.common.Litre
import com.infowings.catalog.data.aspect.AspectService
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FreeAspectUpdateTest {
    private val username = "admin"

    @Autowired
    lateinit var aspectService: AspectService

    lateinit var aspect: AspectData

    @Before
    fun init() {
        val ad = AspectData("", "aspect", null, null, BaseType.Text.name, emptyList())
        aspect = aspectService.save(ad, username).toAspectData().copy(measure = Litre.name)
    }

    @Test
    fun testChangeBaseType() {
        val ad = AspectData("", "aspect", null, null, BaseType.Text.name, emptyList())
        val aspect = aspectService.save(ad, username).toAspectData()
        val newAspect = aspectService.save(aspect.copy(baseType = BaseType.Decimal.name), username)

        Assert.assertEquals("aspect should have new base type", newAspect.baseType, BaseType.Decimal)
    }

    @Test
    fun testChangeAspectMeasureOtherGroup() {

        val newAspect = aspectService.save(aspect.copy(measure = Litre.name), username)

        Assert.assertTrue("aspect should have new measure", newAspect.measure == Litre)

        Assert.assertTrue("aspect should have correct base type", newAspect.baseType == BaseType.Decimal)
    }
}