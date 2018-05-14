package com.infowings.catalog.data

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.*
import com.infowings.catalog.common.BaseType.Boolean
import com.infowings.catalog.common.BaseType.Decimal
import com.infowings.catalog.data.aspect.*
import com.infowings.catalog.data.aspect.AspectPropertyCardinality.INFINITY
import org.hamcrest.core.Is
import org.junit.Assert
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
    private val username = "admin"

    @Autowired
    lateinit var aspectService: AspectService

    @Autowired
    lateinit var subjectService: SubjectService

    @Test
    fun testNotVirtualId() {
        val ad = AspectData("", "newAspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        aspectService.save(ad, username)

        assertThat("Ids are not virtual", aspectService.getAspects().all { !it.id.contains("-") }, Is.`is`(true))
    }

    @Test
    fun testAddAspect() {
        val ad = AspectData("", "newAspect", Kilometre.name, null, Decimal.name, emptyList())
        val createAspect: Aspect = aspectService.save(ad, username)

        assertThat(
            "aspect should be saved and restored",
            aspectService.findByName("newAspect").firstOrNull(),
            Is.`is`(createAspect)
        )
    }

    @Test
    fun testAddAspectTrim() {
        val aspectBase =
            aspectService.save(AspectData("", "AspectBase", Kilometre.name, null, Decimal.name, emptyList()), username)
        val aspectProp = AspectPropertyData("", "  propTrim  ", aspectBase.id, AspectPropertyCardinality.INFINITY.name)
        val ad = AspectData("", "  newAspectTrim   ", Kilometre.name, null, Decimal.name, listOf(aspectProp))
        val createAspect: Aspect = aspectService.save(ad, username)

        val aspect = aspectService.findByName("newAspectTrim").firstOrNull()
        assertThat("aspect should be saved and restored with trim name", aspect, Is.`is`(createAspect))
        assertThat(
            "aspect should be saved and restored with trim property name",
            aspect?.properties?.first()?.name,
            Is.`is`("propTrim")
        )
    }

    @Test
    fun testAddAspectWithEmptyParams() {
        val ad = AspectData("", "newAspect", null, null, Decimal.name, emptyList())
        val createAspect: Aspect = aspectService.save(ad, username)

        assertThat(
            "aspect should be saved and restored event when some params are missing",
            aspectService.findByName("newAspect").firstOrNull(),
            Is.`is`(createAspect)
        )
    }

    @Test(expected = AspectInconsistentStateException::class)
    fun testAddAspectWithEmptyParams2() {
        val ad = AspectData("", "newAspect", null, null, null, emptyList())
        aspectService.save(ad, username)
    }

    @Test
    fun testAddAspectWithEmptyParams3() {
        val ad = AspectData("", "newAspect", Kilometre.name, null, Decimal.name, emptyList())
        val createAspect: Aspect = aspectService.save(ad, username)

        assertThat(
            "aspect should be saved and restored event when some params are missing",
            aspectService.findByName("newAspect").firstOrNull(),
            Is.`is`(createAspect)
        )
    }

    @Test(expected = AspectInconsistentStateException::class)
    fun testFailAddAspect() {
        val ad = AspectData(
            "",
            "newAspect",
            Kilometre.name,
            OpenDomain(Boolean).toString(),
            BaseType.Boolean.name,
            emptyList()
        )
        aspectService.save(ad, username)
    }

    @Test
    fun testChangeAspectParams() {
        val ad = AspectData("", "aspect", Kilometre.name, null, Decimal.name, emptyList())
        val aspect = aspectService.save(ad, username)

        val ad2 = AspectData(aspect.id, "new Aspect", Metre.name, null, Decimal.name, emptyList(), 1)
        val newAspect = aspectService.save(ad2, username)

        assertThat("aspect should have new name", newAspect.name, Is.`is`("new Aspect"))
        assertThat("aspect should have new measure", newAspect.measure?.name, Is.`is`(Metre.name))
    }

    @Test(expected = AspectAlreadyExist::class)
    fun testAddTwoAspectsSameName() {
        val ad = AspectData("", "aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        aspectService.save(ad, username)

        val ad2 = AspectData("", "aspect", Metre.name, null, Decimal.name, emptyList(), 1)
        aspectService.save(ad2, username)

        assertThat("should return two aspects with name 'aspect'", aspectService.findByName("aspect").size, Is.`is`(2))
    }

    @Test(expected = AspectAlreadyExist::class)
    fun testAddTwoAspectsSameNameIgnoreCase() {
        val ad = AspectData("", "aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        aspectService.save(ad, username)

        val ad2 = AspectData("", "Aspect", Metre.name, null, Decimal.name, emptyList(), 1)
        aspectService.save(ad2, username)
    }

    @Test(expected = AspectAlreadyExist::class)
    fun testAddAspectsSameNameSameMeasure() {
        val ad1 = AspectData("", "aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        aspectService.save(ad1, username)

        val ad2 = AspectData("", "aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        aspectService.save(ad2, username)
    }

    @Test
    fun testAddAspectsDiffNameSameMeasure() {
        val ad1 = AspectData("", "aspect", Kilometre.name, null, Decimal.name, emptyList())
        aspectService.save(ad1, username)

        val ad2 = AspectData("", "aspect2", Kilometre.name, null, Decimal.name, emptyList())
        aspectService.save(ad2, username)

        assertThat(
            "should return two aspects with same measure",
            aspectService.getAspects().filter { it.measure == Kilometre }.size,
            Is.`is`(2)
        )
    }

    @Test(expected = AspectInconsistentStateException::class)
    fun testUnCorrectMeasureBaseTypeRelations() {
        val ad = AspectData("", "aspect", Kilometre.name, null, BaseType.Boolean.name, emptyList())
        aspectService.save(ad, username)
    }

    @Test
    fun testChangeAspectMeasureSameGroup() {
        val ad = AspectData("", "aspect", Kilometre.name, null, Decimal.name, emptyList())
        val aspect = aspectService.save(ad, username).toAspectData().copy(measure = Metre.name)

        val newAspect = aspectService.save(aspect, username)

        assertTrue("aspect should have new measure", newAspect.measure == Metre)

        assertTrue("aspect should have correct base type", newAspect.baseType == Decimal)
    }

    // todo: Change to change measure in case no values for aspect
    @Test
    fun testChangeAspectMeasureOtherGroupFreeAspect() {
        val ad = AspectData("", "aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val aspect = aspectService.save(ad, username).toAspectData().copy(measure = Litre.name)

        val newAspect = aspectService.save(aspect, username)

        assertTrue("aspect should have new measure", newAspect.measure == Litre)

        assertTrue("aspect should have correct base type", newAspect.baseType == Decimal)
    }

    // todo: Change to change measure in case no values for aspect
    @Test
    fun testChangeAspectMeasureOtherGroupOtherBaseType() {
        val ad = AspectData("", "aspect", Kilometre.name, null, Decimal.name, emptyList())
        val aspect = aspectService.save(ad, username)

        // We have not measures with different from Decimal BaseType. So, little hack
        val field = Gram::class.java.getDeclaredField("baseType")
        field.isAccessible = true
        field.set(Gram, Boolean)

        val newAspect =
            aspectService.save(aspect.toAspectData().copy(measure = Gram.name, baseType = Gram.baseType.name), username)

        assertTrue("aspect should have new measure", newAspect.measure == Gram)

        assertTrue("aspect should have correct base type", newAspect.measure?.baseType == Boolean)

        field.set(Gram, Decimal)
    }

    @Test
    fun testChangeAspectMeasureOtherGroupNotFreeAspect() {
        val ad = AspectData("", "aspect", null, null, BaseType.Decimal.name, emptyList())
        val aspect = aspectService.save(ad, username)

        val property = AspectProperty("", "name", aspect, AspectPropertyCardinality.ONE, 0).toAspectPropertyData()
        aspectService.save(aspect.toAspectData().copy(name = "new", id = null, properties = listOf(property)), username)

        val ad2 = aspect.copy(measure = Litre, version = 2)
        val saved = aspectService.save(ad2.toAspectData(), username)
        assertTrue("measure should be Litre", saved.measure == Litre)
    }

    @Test
    fun testMeasureBaseTypeManipulating() {
        val ad = AspectData("", "aspect", Litre.name, null, null, emptyList())
        val aspect = aspectService.save(ad, username)

        assertTrue("base type should be decimal", aspect.baseType == Decimal)
        assertTrue("measure should be litre", aspect.measure == Litre)

        val ad2 = AspectData(aspect.id, "aspect", null, null, BaseType.Boolean.name, emptyList(), aspect.version)
        val aspect2 = aspectService.save(ad2, username)

        assertTrue("base type should be boolean", aspect2.baseType == BaseType.Boolean)
        assertTrue("measure should be null", aspect2.measure == null)

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

        assertTrue("base type should be decimal", aspect3.baseType == BaseType.Decimal)
        assertTrue("measure should be metre", aspect3.measure == Metre)
    }

    @Test
    fun testAspectWithoutCyclicDependency() {
        val aspect = prepareAspect()
        assertThat(
            "aspect should be saved and restored if no cyclic dependencies",
            aspectService.findByName("aspect").firstOrNull(),
            Is.`is`(aspect)
        )
    }


    @Test(expected = AspectCyclicDependencyException::class)
    fun testAspectCyclicDependency() {
        val aspect = prepareAspect()
        val editedPropertyData1 = AspectPropertyData("", "prop1", aspect.id, AspectPropertyCardinality.INFINITY.name)
        val aspect1 = aspect.properties.first().aspect
        val editedAspectData1 = AspectData(
            aspect1.id,
            "aspect1",
            Metre.name,
            null,
            Decimal.name,
            aspect1.properties.toAspectPropertyData().plus(editedPropertyData1),
            aspect1.version
        )

        aspectService.save(editedAspectData1, username)
    }

    @Test
    fun testDoubleSaveNoSubject() {
        val aspectData = aspectDataWithSubject("AspectDoubleSaveNoSubject")
        aspectService.save(aspectData, username)
        try {
            aspectService.save(aspectData, username)
            Assert.fail("Nothing thrown")
        } catch (e: AspectAlreadyExist) {

        } catch (e: Throwable) {
            Assert.fail("Thrown unexpected " + e)
        }
    }

    @Test
    fun testDoubleSaveSameSubject() {
        val aspectData = aspectDataWithSubject("AspectDoubleSaveSameSubject", "SubjectDoubleSaveSameSubject")
        aspectService.save(aspectData, username)
        try {
            aspectService.save(aspectData, username)
            Assert.fail("Nothing thrown")
        } catch (e: AspectAlreadyExist) {
        } catch (e: Throwable) {
            Assert.fail("Thrown unexpected " + e)
        }
    }

    @Test
    fun testDoubleSaveWithAndWithoutSubject() {
        val aspectData1 = aspectDataWithSubject("AspectDoubleSaveWithAndWithoutSubject")
        val aspectData2 = aspectDataWithSubject(
            "AspectDoubleSaveWithAndWithoutSubject",
            "SubjectDoubleSaveWithAndWithoutSubject"
        )

        val aspect1 = aspectService.save(aspectData1, username)
        val aspect2 = aspectService.save(aspectData2, username)

        Assert.assertEquals("first subject is incorrect", null, aspect1.subject)
        Assert.assertEquals(
            "second subject is incorrect", aspectData2.subject?.copy(version = 2),
            aspect2.subject?.toSubjectData()
        )
    }

    @Test
    fun testDoubleSaveWithoutAndWithSubject() {
        val aspectData1 = aspectDataWithSubject(
            "AspectDoubleSaveWithoutAndWithSubject",
            "SubjectDoubleSaveWithoutAndWithSubject"
        )
        val aspectData2 = aspectDataWithSubject("AspectDoubleSaveWithoutAndWithSubject")

        val aspect1 = aspectService.save(aspectData1, username)
        val aspect2 = aspectService.save(aspectData2, username)

        Assert.assertEquals(
            "first subject is incorrect",
            aspectData1.subject?.copy(version = 2), aspect1.subject?.toSubjectData()
        )
        Assert.assertEquals("second subject is incorrect", null, aspect2.subject)
    }

    @Test
    fun testDoubleSaveDifferentSubjects() {
        val aspectData1 = aspectDataWithSubject(
            "AspectDoubleSaveDifferentSubjects",
            "SubjectDoubleSaveDifferentSubjects"
        )
        val aspectData2 = aspectDataWithSubject(
            "AspectDoubleSaveDifferentSubjects",
            "OtherSubjectDoubleSaveDifferentSubjects"
        )

        val aspect1 = aspectService.save(aspectData1, username)
        val aspect2 = aspectService.save(aspectData2, username)

        Assert.assertEquals(
            "first subject is incorrect",
            aspectData1.subject?.copy(version = 2), aspect1.subject?.toSubjectData()
        )
        Assert.assertEquals(
            "second subject is incorrect",
            aspectData2.subject?.copy(version = 2), aspect2.subject?.toSubjectData()
        )
    }

    @Test
    fun testTripleSaveNoSomeOther() {
        val aspectData1 = aspectDataWithSubject("AspectTripleSaveNoSomeOther")
        val aspectData2 = aspectDataWithSubject(
            "AspectTripleSaveNoSomeOther",
            "SomeSubjectTripleSaveNoSomeOther"
        )
        val aspectData3 = aspectDataWithSubject(
            "AspectTripleSaveNoSomeOther",
            "OtherSubjectTripleSaveNoSomeOther"
        )

        val aspect1 = aspectService.save(aspectData1, username)
        val aspect2 = aspectService.save(aspectData2, username)
        val aspect3 = aspectService.save(aspectData3, username)

        Assert.assertEquals("first subject is incorrect", null, aspect1.subject)
        Assert.assertEquals(
            "second subject is incorrect",
            aspectData2.subject?.copy(version = 2), aspect2.subject?.toSubjectData()
        )
        Assert.assertEquals(
            "third subject is incorrect",
            aspectData3.subject?.copy(version = 2), aspect3.subject?.toSubjectData()
        )
    }

    @Test
    fun testTripleSaveSomeNoOther() {
        val aspectData1 = aspectDataWithSubject(
            "AspectTripleSaveSomeNoOther",
            "SomeSubjectTripleSaveSomeNoOther"
        )
        val aspectData2 = aspectDataWithSubject("AspectTripleSaveSomeNoOther")
        val aspectData3 = aspectDataWithSubject(
            "AspectTripleSaveSomeNoOther",
            "OtherSubjectTripleSaveSomeNoOther"
        )

        val aspect1 = aspectService.save(aspectData1, username)
        val aspect2 = aspectService.save(aspectData2, username)
        val aspect3 = aspectService.save(aspectData3, username)

        Assert.assertEquals(
            "first subject is incorrect", aspectData1.subject?.copy(version = 2),
            aspect1.subject?.toSubjectData()
        )
        Assert.assertEquals("second subject is incorrect", null, aspect2.subject)
        Assert.assertEquals(
            "third subject is incorrect", aspectData3.subject?.copy(version = 2),
            aspect3.subject?.toSubjectData()
        )
    }

    @Test
    fun testTripleSaveSomeOtherNo() {
        val aspectData1 = aspectDataWithSubject(
            "AspectTripleSaveSomeOtherNo",
            "SomeSubjectTripleSaveSomeOtherNo"
        )
        val aspectData2 = aspectDataWithSubject(
            "AspectTripleSaveSomeOtherNo",
            "OtherSubjectTripleSaveSomeOtherNo"
        )
        val aspectData3 = aspectDataWithSubject("AspectTripleSaveSomeOtherNo")

        val aspect1 = aspectService.save(aspectData1, username)
        val aspect2 = aspectService.save(aspectData2, username)
        val aspect3 = aspectService.save(aspectData3, username)

        Assert.assertEquals(
            "first subject is incorrect",
            aspectData1.subject?.copy(version = 2), aspect1.subject?.toSubjectData()
        )
        Assert.assertEquals(
            "second subject is incorrect",
            aspectData2.subject?.copy(version = 2), aspect2.subject?.toSubjectData()
        )
        Assert.assertEquals("third subject is incorrect", null, aspect3.subject)
    }

    @Test
    fun testUpdateSameData() {
        prepareAspect()
        val ad = aspectService.getAspects().first().toAspectData()
        try {
            aspectService.save(ad, username)
        } catch (e: AspectEmptyChangeException) {
        }
        val newAspect = aspectService.findById(ad.id!!)
        Assert.assertEquals("Same data shouldn't be rewritten", ad.version, newAspect.version)
    }

    private fun prepareAspect(): Aspect {
        /*
         *  aspect
         *    aspectProperty
         *       aspect1
         *          aspectProperty1
         *              aspect2
         */

        val aspectData2 = AspectData(null, "aspect2", Kilogram.name, null, Decimal.name, emptyList())
        val aspect2: Aspect = aspectService.save(aspectData2, username)

        val aspectPropertyData1 = AspectPropertyData("", "prop1", aspect2.id, INFINITY.name)
        val aspectData1 = AspectData(null, "aspect1", Metre.name, null, Decimal.name, listOf(aspectPropertyData1))
        val aspect1: Aspect = aspectService.save(aspectData1, username)

        val aspectPropertyData = AspectPropertyData("", "prop", aspect1.id, INFINITY.name)
        val aspectData = AspectData(null, "aspect", Metre.name, null, Decimal.name, listOf(aspectPropertyData))
        return aspectService.save(aspectData, username)
    }

    private fun aspectDataWithSubject(aspectName: String, subjectName: String? = null): AspectData {
        val subjectData: SubjectData? = subjectName?.let {
            subjectService.createSubject(SubjectData(name = it, description = "some description"), username).toSubjectData()
        }
        return AspectData(
            null, aspectName, Kilogram.name, null, Decimal.name, emptyList(),
            subject = subjectData
        )
    }
}