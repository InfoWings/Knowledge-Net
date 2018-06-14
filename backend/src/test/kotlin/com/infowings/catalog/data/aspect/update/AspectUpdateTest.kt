package com.infowings.catalog.data.aspect.update

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.*
import com.infowings.catalog.data.aspect.AspectEmptyChangeException
import com.infowings.catalog.data.aspect.AspectService
import org.hamcrest.core.Is
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AspectUpdateTest {
    private val username = "admin"

    @Autowired
    lateinit var aspectService: AspectService

    lateinit var aspect: AspectData

    @Test
    fun testChangeAspectNameMeasure() {
        val ad = AspectData("", "aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val aspect = aspectService.save(ad, username)

        val ad2 = AspectData(aspect.id, "new Aspect", Metre.name, null, BaseType.Decimal.name, emptyList(), 1)
        val newAspect = aspectService.save(ad2, username)

        Assert.assertThat("aspect should have new name", newAspect.name, Is.`is`("new Aspect"))
        Assert.assertThat("aspect should have new measure", newAspect.measure?.name, Is.`is`(Metre.name))
    }

    @Test
    fun testChangeAspectMeasureSameGroup() {
        val ad = AspectData("", "aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val aspect = aspectService.save(ad, username).toAspectData().copy(measure = Metre.name)

        val newAspect = aspectService.save(aspect, username)

        Assert.assertTrue("aspect should have new measure", newAspect.measure == Metre)

        Assert.assertTrue("aspect should have correct base type", newAspect.baseType == BaseType.Decimal)
    }

    @Test
    fun testMeasureBaseTypeManipulating() {
        val ad = AspectData("", "aspect", Litre.name, null, null, emptyList())
        val aspect = aspectService.save(ad, username)

        Assert.assertTrue("base type should be decimal", aspect.baseType == BaseType.Decimal)
        Assert.assertTrue("measure should be litre", aspect.measure == Litre)

        val ad2 = AspectData(aspect.id, "aspect", null, null, BaseType.Boolean.name, emptyList(), aspect.version)
        val aspect2 = aspectService.save(ad2, username)

        Assert.assertTrue("base type should be boolean", aspect2.baseType == BaseType.Boolean)
        Assert.assertTrue("measure should be null", aspect2.measure == null)

        val ad3 = AspectData(
            aspect.id,
            "aspect",
            Metre.name,
            null,
            BaseType.Decimal.name,
            emptyList(),
            aspect2.version
        )
        val aspect3 = aspectService.save(ad3, username)

        Assert.assertTrue("base type should be decimal", aspect3.baseType == BaseType.Decimal)
        Assert.assertTrue("measure should be metre", aspect3.measure == Metre)
    }

    @Test
    fun testChangeAspectMeasureOtherGroupOtherBaseType() {
        val ad = AspectData("", "aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val aspect = aspectService.save(ad, username)

        // We have not measures with different from Decimal BaseType. So, little hack
        val field = Gram::class.java.getDeclaredField("baseType")
        field.isAccessible = true
        field.set(Gram, BaseType.Boolean)

        val newAspect =
            aspectService.save(aspect.toAspectData().copy(measure = Gram.name, baseType = Gram.baseType.name), username)

        Assert.assertTrue("aspect should have new measure", newAspect.measure == Gram)

        Assert.assertTrue("aspect should have correct base type", newAspect.measure?.baseType == BaseType.Boolean)

        field.set(Gram, BaseType.Decimal)
    }

    @Test
    fun testUpdateSameData() {
        val ad = AspectData("", "aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        aspectService.save(ad, username)

        val ad2 = aspectService.getAspects().first().toAspectData()
        try {
            aspectService.save(ad2, username)
        } catch (e: AspectEmptyChangeException) {
        }
        val newAspect = aspectService.findById(ad2.id!!)
        Assert.assertEquals("Same data shouldn't be rewritten", ad2.version, newAspect.version)
    }
}