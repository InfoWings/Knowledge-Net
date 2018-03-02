package com.infowings.catalog.data

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.*
import org.hamcrest.core.Is
import org.hamcrest.core.IsNull
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

    @Test(expected = AspectAlreadyExist::class)
    fun testChangeAspectNameInCaseExistsAspectWithSameName() {
        val ad1 = AspectData("", "aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val aspect1 = aspectService.createAspect(ad1)

        val ad2 = AspectData("", "aspect2", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val aspect2 = aspectService.createAspect(ad2)

        aspectService.changeName(aspect1.id, aspect2.name)
    }

    // todo: Change to change base type in case no measure and no values for aspect
    @Test
    fun testChangeBaseTypeInCaseNoMeasure() {
        val ad = AspectData("", "aspect", null, null, BaseType.Decimal.name, emptyList())
        val aspect = aspectService.createAspect(ad)

        val newAspect = aspectService.changeBaseType(aspect.id, BaseType.Boolean)

        assertThat("returned from changeBaseType aspect should be the same as found by findById", aspectService.findById(newAspect.id), Is.`is`(newAspect))

        assertTrue("aspect should have new base type", newAspect.baseType!! == BaseType.Boolean)
    }

    @Test(expected = AspectModificationException::class)
    fun testChangeBaseTypeInCaseExistMeasure() {
        val ad = AspectData("", "aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val aspect = aspectService.createAspect(ad)

        aspectService.changeBaseType(aspect.id, BaseType.Boolean)
    }

    // todo: Change to change measure in case no values for aspect
    @Test
    fun testChangeAspectMeasureOtherGroupSameBaseType() {
        val ad = AspectData("", "aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val aspect = aspectService.createAspect(ad)

        val newAspect = aspectService.changeMeasure(aspect.id, Gram)

        assertThat("returned from changeMeasure aspect should be the same as found by findById", aspectService.findById(newAspect.id), Is.`is`(newAspect))

        assertTrue("aspect should have new measure", newAspect.measure == Gram)

        assertTrue("aspect should have correct base type", newAspect.baseType == BaseType.Decimal)
    }

    // todo: Change to change measure in case no values for aspect
    @Test
    fun testChangeAspectMeasureOtherGroupOtherBaseType() {
        val ad = AspectData("", "aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val aspect = aspectService.createAspect(ad)

        // We have not measures with different from Decimal BaseType. So, little hack
        val field = Gram::class.java.getDeclaredField("baseType")
        field.isAccessible = true
        field.set(Gram, BaseType.Boolean)

        val newAspect = aspectService.changeMeasure(aspect.id, Gram)

        assertThat("returned from changeMeasure aspect should be the same as found by findById", aspectService.findById(newAspect.id), Is.`is`(newAspect))

        assertTrue("aspect should have new measure", newAspect.measure == Gram)

        assertTrue("aspect should have correct base type", newAspect.measure?.baseType == BaseType.Boolean)
    }

    @Test
    fun testChangeAspectMeasureSameGroup() {
        val ad = AspectData("", "aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val aspect = aspectService.createAspect(ad)

        val newAspect = aspectService.changeMeasure(aspect.id, Metre)

        assertTrue("aspect should have new measure", newAspect.measure == Metre)

        assertTrue("aspect should have correct base type", newAspect.baseType == BaseType.Decimal)
    }

    @Test
    fun testChangeAspectMeasureToNull() {
        val ad = AspectData("", "aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val aspect = aspectService.createAspect(ad)

        val newAspect = aspectService.changeMeasure(aspect.id, null)

        assertThat("aspect should have null as a measure", newAspect.measure, Is.`is`(IsNull()))

        assertThat("aspect should have correct base type", newAspect.baseType, Is.`is`(BaseType.restoreBaseType(null)))
    }

    @Test
    fun testChangeBaseTypeToNull() {
        val ad = AspectData("", "aspect", null, null, BaseType.Decimal.name, emptyList())
        val aspect = aspectService.createAspect(ad)

        val newAspect = aspectService.changeBaseType(aspect.id, null)

        assertThat("aspect should have correct base type", newAspect.baseType, Is.`is`(BaseType.restoreBaseType(null)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testAddIncorrectBaseTypeAnMeasurePair() {
        val ad = AspectData("", "aspect", Gram.name, null, BaseType.Boolean.name, emptyList())
        aspectService.createAspect(ad)
    }

    @Test
    fun testMeasureBaseTypeManipulating() {
        val ad = AspectData("", "aspect", Gram.name, null, null, emptyList())
        val aspect = aspectService.createAspect(ad)

        assertTrue("base type should be decimal", aspect.baseType == BaseType.Decimal)
        assertTrue("measure should be gram", aspect.measure == Gram)

        val ad2 = AspectData(aspect.id, "aspect", null, null, null, emptyList())
        val aspect2 = aspectService.updateAspect(ad2)

        assertTrue("base type should be null", aspect2.baseType == BaseType.restoreBaseType(null))
        assertTrue("measure should be null", aspect2.measure == null)

        val ad3 = AspectData(aspect.id, "aspect", null, null, BaseType.Boolean.name, emptyList())
        val aspect3 = aspectService.updateAspect(ad3)

        assertTrue("base type should be decimal", aspect3.baseType == BaseType.Boolean)
        assertTrue("measure should be null", aspect3.measure == null)

        val aspect4 = aspectService.changeMeasure(aspect3.id, Kilometre)

        assertTrue("base type should be null", aspect4.baseType == BaseType.Decimal)
        assertTrue("measure should be Kilometre", aspect4.measure == Kilometre)
    }
}