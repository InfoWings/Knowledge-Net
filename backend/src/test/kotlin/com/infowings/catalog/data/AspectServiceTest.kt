package com.infowings.catalog.data

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.*
import org.hamcrest.core.Is
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
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
    lateinit var aspectService: AspectService

    @Test
    fun testNotVirtualId() {
        val ad = AspectData("", "newAspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        aspectService.save(ad)

        assertThat("Ids are not virtual", aspectService.getAspects().all { !it.id.contains("-") }, Is.`is`(true))
    }

    @Test
    fun testAddAspect() {
        val ad = AspectData("", "newAspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val createAspect: Aspect = aspectService.save(ad)

        assertThat("aspect should be saved and restored", aspectService.findByName("newAspect").firstOrNull(), Is.`is`(createAspect))
    }

    @Test
    fun testAddAspectWithEmptyParams() {
        val ad = AspectData("", "newAspect", null, null, BaseType.Decimal.name, emptyList())
        val createAspect: Aspect = aspectService.save(ad)

        assertThat("aspect should be saved and restored event when some params are missing", aspectService.findByName("newAspect").firstOrNull(), Is.`is`(createAspect))
    }

    @Test
    fun testAddAspectWithEmptyParams2() {
        val ad = AspectData("", "newAspect", null, null, null, emptyList())
        val createAspect: Aspect = aspectService.save(ad)

        assertThat("aspect should be saved and restored event when some params are missing", aspectService.findByName("newAspect").firstOrNull(), Is.`is`(createAspect))
    }

    @Test
    fun testAddAspectWithEmptyParams3() {
        val ad = AspectData("", "newAspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val createAspect: Aspect = aspectService.save(ad)

        assertThat("aspect should be saved and restored event when some params are missing", aspectService.findByName("newAspect").firstOrNull(), Is.`is`(createAspect))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testFailAddAspect() {
        val ad = AspectData("", "newAspect", Kilometre.name, OpenDomain(BaseType.Boolean).toString(), BaseType.Boolean.name, emptyList())
        aspectService.save(ad)
    }

    @Test
    fun testChangeAspectParams() {
        val ad = AspectData("", "aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val aspect = aspectService.save(ad)

        val ad2 = AspectData(aspect.id, "new Aspect", Meter.name, null, BaseType.Decimal.name, emptyList(), 1)
        val newAspect = aspectService.save(ad2)

        assertThat("aspect should have new name", newAspect.name, Is.`is`("new Aspect"))
        assertThat("aspect should have new measure", newAspect.measure?.name, Is.`is`(Meter.name))
    }

    @Test
    fun testAddTwoAspectsSameName() {
        val ad = AspectData("", "aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val aspect = aspectService.save(ad)

        val ad2 = AspectData("", "aspect", Meter.name, null, BaseType.Decimal.name, emptyList(), 1)
        aspectService.save(ad2)

        assertThat("should return two aspects with name 'aspect'", aspectService.findByName("aspect").size, Is.`is`(2))
    }

    @Test(expected = AspectAlreadyExist::class)
    fun testAddAspectsSameNameSameMeasure() {
        val ad1 = AspectData("", "aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        aspectService.save(ad1)

        val ad2 = AspectData("", "aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        aspectService.save(ad2)
    }

    @Test
    fun testAddAspectsDiffNameSameMeasure() {
        val ad1 = AspectData("", "aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        aspectService.save(ad1)

        val ad2 = AspectData("", "aspect2", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        aspectService.save(ad2)

        assertThat("should return two aspects with same measure",
                aspectService.getAspects().filter { it.measure == Kilometre }.size,
                Is.`is`(2))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testUnCorrectMeasureBaseTypeRelations() {
        val ad = AspectData("", "aspect", Kilometre.name, null, BaseType.Boolean.name, emptyList())
        aspectService.save(ad)
    }

    @Test
    fun testChangeAspectMeasureSameGroup() {
        val ad = AspectData("", "aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val aspect = aspectService.save(ad).toAspectData().copy(measure = Meter.name)

        val newAspect = aspectService.save(aspect)

        assertTrue("aspect should have new measure", newAspect.measure == Meter)

        assertTrue("aspect should have correct base type", newAspect.baseType == BaseType.Decimal)
    }

    // todo: Change to change measure in case no values for aspect
    @Test
    fun testChangeAspectMeasureDiffGroup() {
        val ad = AspectData("", "aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val aspect = aspectService.save(ad).toAspectData().copy(measure = Litre.name)

        val newAspect = aspectService.save(aspect)

        assertTrue("aspect should have new measure", newAspect.measure == Litre)

        assertTrue("aspect should have correct base type", newAspect.baseType == BaseType.Decimal)
    }

    // todo: Change to change measure in case no values for aspect
    @Test
    fun testChangeAspectMeasureOtherGroupOtherBaseType() {
        val ad = AspectData("", "aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val aspect = aspectService.save(ad)

        // We have not measures with different from Decimal BaseType. So, little hack
        val field = Gram::class.java.getDeclaredField("baseType")
        field.isAccessible = true
        field.set(Gram, BaseType.Boolean)

        val newAspect = aspectService.save(aspect.toAspectData().copy(measure = Gram.name, baseType = Gram.baseType.name))

        assertTrue("aspect should have new measure", newAspect.measure == Gram)

        assertTrue("aspect should have correct base type", newAspect.measure?.baseType == BaseType.Boolean)

        field.set(Gram, BaseType.Decimal)

    }

    @Test(expected = AspectModificationException::class)
    fun testChangeBaseTypeToNull() {
        val ad = AspectData("", "aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val aspect = aspectService.save(ad)
        aspectService.save(aspect.toAspectData().copy(baseType = null))
    }

    @Test
    fun testMeasureBaseTypeManipulating() {
        val ad = AspectData("", "aspect", Litre.name, null, null, emptyList())
        val aspect = aspectService.save(ad)

        assertTrue("base type should be decimal", aspect.baseType == BaseType.Decimal)
        assertTrue("measure should be litre", aspect.measure == Litre)

        val ad2 = AspectData(aspect.id, "aspect", null, null, null, emptyList(), aspect.version)
        val aspect2 = aspectService.save(ad2)

        assertTrue("base type should be null", aspect2.baseType == BaseType.restoreBaseType(null))
        assertTrue("measure should be null", aspect2.measure == null)

        val ad3 = AspectData(aspect.id, "aspect", null, null, BaseType.Boolean.name, emptyList(), aspect2.version)
        val aspect3 = aspectService.save(ad3)

        assertTrue("base type should be decimal", aspect3.baseType == BaseType.Boolean)
        assertTrue("measure should be null", aspect3.measure == null)
    }
}