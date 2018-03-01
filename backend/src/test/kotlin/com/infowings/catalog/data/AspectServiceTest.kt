package com.infowings.catalog.data

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.*
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
    lateinit var aspectService: AspectService

    @Test
    fun testAddAspect() {
        val ad = AspectData("", "newAspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val createAspect: Aspect = aspectService.createAspect(ad)

        assertThat("aspect should be saved and restored", aspectService.findByName("newAspect"), Is.`is`(createAspect))
    }

    @Test
    fun testAddAspectWithEmptyParams() {
        val ad = AspectData("", "newAspect", null, null, BaseType.Decimal.name, emptyList())
        val createAspect: Aspect = aspectService.createAspect(ad)

        assertThat("aspect should be saved and restored event when some params are missing", aspectService.findByName("newAspect"), Is.`is`(createAspect))
    }

    @Test
    fun testAddAspectWithEmptyParams2() {
        val ad = AspectData("", "newAspect", null, null, null, emptyList())
        val createAspect: Aspect = aspectService.createAspect(ad)

        assertThat("aspect should be saved and restored event when some params are missing", aspectService.findByName("newAspect"), Is.`is`(createAspect))
    }

    @Test
    fun testAddAspectWithEmptyParams3() {
        val ad = AspectData("", "newAspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val createAspect: Aspect = aspectService.createAspect(ad)

        assertThat("aspect should be saved and restored event when some params are missing", aspectService.findByName("newAspect"), Is.`is`(createAspect))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testFailAddAspect() {
        val ad = AspectData("", "newAspect", Kilometre.name, OpenDomain(BaseType.Boolean).toString(), BaseType.Boolean.name, emptyList())
        aspectService.createAspect(ad)
    }

    @Test
    fun testChangeAspectName() {
        val ad = AspectData("", "aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val aspect = aspectService.createAspect(ad)

        val newAspect = aspectService.changeName(aspect.id, "new Aspect")

        assertThat("returned from changeName aspect should be the same as found by findById", aspectService.findById(newAspect.id), Is.`is`(newAspect))

        assertThat("aspect should have new name", newAspect.name, Is.`is`("new Aspect"))
    }

    @Test(expected = AspectAlreadyExist::class)
    fun testAddAspectInCaseExistsAspectWithSameName() {
        val ad1 = AspectData("", "aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        aspectService.createAspect(ad1)

        val ad2 = AspectData("", "aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        aspectService.createAspect(ad2)
    }

    @Test(expected = AspectModificationException::class)
    fun testChangeAspectNameInCaseExistsAspectWithSameName() {
        val ad1 = AspectData("", "aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val aspect1 = aspectService.createAspect(ad1)

        val ad2 = AspectData("", "aspect2", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val aspect2 = aspectService.createAspect(ad2)

        aspectService.changeName(aspect1.id, aspect2.name)
    }

    @Test
            // todo: Change to change base type in case no measure and no values for aspect
    fun testChangeBaseTypeInCaseNoMeasure() {
        val ad = AspectData("", "aspect", null, null, BaseType.Decimal.name, emptyList())
        val aspect = aspectService.createAspect(ad)

        val newAspect = aspectService.changeBaseType(aspect.id, BaseType.Boolean)

        assertThat("returned from changeBaseType aspect should be the same as found by findById", aspectService.findById(newAspect.id), Is.`is`(newAspect))

        assertThat("aspect should have new base type", newAspect.baseType!!.name, Is.`is`(BaseType.Boolean.name))
    }

    @Test(expected = AspectModificationException::class)
    fun testChangeBaseTypeInCaseExistMeasure() {
        val ad = AspectData("", "aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val aspect = aspectService.createAspect(ad)

        aspectService.changeBaseType(aspect.id, BaseType.Boolean)
    }

    @Test
            // todo: Change to change measure in case no values for aspect
    fun testChangeAspectMeasureOtherGroupSameBaseType() {
        val ad = AspectData("", "aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val aspect = aspectService.createAspect(ad)

        val newAspect = aspectService.changeMeasure(aspect.id, Gram)

        assertThat("returned from changeMeasure aspect should be the same as found by findById", aspectService.findById(newAspect.id), Is.`is`(newAspect))

        assertThat("aspect should have new measure", newAspect.measure!!.name, Is.`is`(Gram.name))

        assertThat("aspect should have correct base type", newAspect.baseType!!.name, Is.`is`(BaseType.Decimal.name))
    }

    @Test
            // todo: Change to change measure in case no values for aspect
    fun testChangeAspectMeasureOtherGroupOtherBaseType() {
        val ad = AspectData("", "aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val aspect = aspectService.createAspect(ad)

        // We have not measures with different from Decimal BaseType. So, little hack
        val field = Gram::class.java.getDeclaredField("baseType")
        field.isAccessible = true
        field.set(Gram, BaseType.Boolean)

        val newAspect = aspectService.changeMeasure(aspect.id, Gram)

        assertThat("returned from changeMeasure aspect should be the same as found by findById", aspectService.findById(newAspect.id), Is.`is`(newAspect))

        assertThat("aspect should have new measure", newAspect.measure!!.name, Is.`is`(Gram.name))

        assertThat("aspect should have correct base type", newAspect.measure!!.baseType.name, Is.`is`(BaseType.Boolean.name))
    }

    @Test
    fun testChangeAspectMeasureSameGroup() {
        val ad = AspectData("", "aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val aspect = aspectService.createAspect(ad)

        val newAspect = aspectService.changeMeasure(aspect.id, Metre)

        assertThat("aspect should have new measure", newAspect.measure!!.name, Is.`is`(Metre.name))

        assertThat("aspect should have correct base type", newAspect.baseType!!.name, Is.`is`(BaseType.Decimal.name))
    }
}