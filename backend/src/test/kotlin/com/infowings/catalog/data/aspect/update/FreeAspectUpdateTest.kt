package com.infowings.catalog.data.aspect.update

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.*
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
        val newAspect = aspectService.save(aspect.copy(baseType = BaseType.Decimal.name), username)

        Assert.assertEquals("aspect should have new base type", newAspect.baseType, BaseType.Decimal)
    }

    @Test
    fun testChangeAspectMeasureOtherGroup() {

        val newAspect = aspectService.save(aspect.copy(measure = Litre.name, baseType = null), username)

        Assert.assertTrue("aspect should have new measure", newAspect.measure == Litre)

        Assert.assertTrue("aspect should have correct base type", newAspect.baseType == BaseType.Decimal)
    }

    @Test
    fun testEditProperty() {
        val pd = AspectPropertyData(name = "prop", aspectId = aspect.id!!, cardinality = PropertyCardinality.ONE.name, description = null, id = "")
        val ad2 = AspectData("", "complex", Metre.name, null, BaseType.Decimal.name, listOf(pd))
        val complex = aspectService.save(ad2, username).toAspectData()

        val otherAspect = aspectService.save(AspectData(name = "other", measure = Metre.name), username)
        val newProperty = complex.properties[0].copy(aspectId = otherAspect.id)
        val edited = aspectService.save(complex.copy(properties = listOf(newProperty)), username)

        Assert.assertEquals("aspect property should change aspectId", edited.properties[0].aspect.id, otherAspect.id)
    }
}