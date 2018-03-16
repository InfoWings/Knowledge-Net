package com.infowings.catalog.data

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.*
import com.infowings.catalog.data.aspect.*
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
class AspectServiceSavingTest {

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

    @Test(expected = IllegalArgumentException::class)
    fun testAddAspectWithEmptyParams2() {
        val ad = AspectData("", "newAspect", null, null, null, emptyList())
        aspectService.save(ad)
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

        val ad2 = AspectData(aspect.id, "new Aspect", Metre.name, null, BaseType.Decimal.name, emptyList(), 1)
        val newAspect = aspectService.save(ad2)

        assertThat("aspect should have new name", newAspect.name, Is.`is`("new Aspect"))
        assertThat("aspect should have new measure", newAspect.measure?.name, Is.`is`(Metre.name))
    }

    @Test(expected = AspectAlreadyExist::class)
    fun testAddTwoAspectsSameName() {
        val ad = AspectData("", "aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        aspectService.save(ad)

        val ad2 = AspectData("", "aspect", Metre.name, null, BaseType.Decimal.name, emptyList(), 1)
        aspectService.save(ad2)

        assertThat("should return two aspects with name 'aspect'", aspectService.findByName("aspect").size, Is.`is`(2))
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
        val aspect = aspectService.save(ad).toAspectData().copy(measure = Metre.name)

        val newAspect = aspectService.save(aspect)

        assertTrue("aspect should have new measure", newAspect.measure == Metre)

        assertTrue("aspect should have correct base type", newAspect.baseType == BaseType.Decimal)
    }

    // todo: Change to change measure in case no values for aspect
    @Test
    fun testChangeAspectMeasureOtherGroupFreeAspect() {
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

    @Test
    fun testChangeAspectMeasureOtherGroupNotFreeAspect() {
        val ad = AspectData("", "aspect", null, null, BaseType.Decimal.name, emptyList())
        val aspect = aspectService.save(ad)

        val property = AspectProperty("", "name", aspect, AspectPropertyCardinality.ONE, 0).toAspectPropertyData()
        aspectService.save(aspect.toAspectData().copy(name = "new", id = null, properties = listOf(property)))

        val ad2 = aspect.copy(measure = Litre, version = 2)
        val saved = aspectService.save(ad2.toAspectData())
        assertTrue("measure should be Litre", saved.measure == Litre)
    }

    @Test
    fun testMeasureBaseTypeManipulating() {
        val ad = AspectData("", "aspect", Litre.name, null, null, emptyList())
        val aspect = aspectService.save(ad)

        assertTrue("base type should be decimal", aspect.baseType == BaseType.Decimal)
        assertTrue("measure should be litre", aspect.measure == Litre)

        val ad2 = AspectData(aspect.id, "aspect", null, null, BaseType.Boolean.name, emptyList(), aspect.version)
        val aspect2 = aspectService.save(ad2)

        assertTrue("base type should be boolean", aspect2.baseType == BaseType.Boolean)
        assertTrue("measure should be null", aspect2.measure == null)

        val ad3 = AspectData(aspect.id, "aspect", Metre.name, null, BaseType.Decimal.name, emptyList(), aspect2.version)
        val aspect3 = aspectService.save(ad3)

        assertTrue("base type should be decimal", aspect3.baseType == BaseType.Decimal)
        assertTrue("measure should be metre", aspect3.measure == Metre)
    }
}