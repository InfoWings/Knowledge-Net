package com.infowings.catalog.data.aspect

import com.infowings.catalog.common.*
import com.infowings.catalog.common.BaseType.Boolean
import com.infowings.catalog.common.BaseType.Decimal
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.toSubjectData
import org.hamcrest.core.Is
import org.junit.Assert
import org.junit.Assert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.test.assertEquals


@ExtendWith(SpringExtension::class)
@SpringBootTest
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

        assertThat("Ids are not virtual", aspectService.getAspects().all { !it.idStrict().contains("-") }, Is.`is`(true))
    }

    @Test
    fun testAddAspect() {
        val name = "testAddAspect-newAspect"
        val ad = AspectData("", name, Kilometre.name, null, Decimal.name, emptyList())
        val createAspect: AspectData = aspectService.save(ad, username)

        assertThat(
            "aspect should be saved and restored",
            aspectService.findByName(name).firstOrNull(),
            Is.`is`(createAspect)
        )
    }

    @Test
    fun testAddAspectTrim() {
        val aspectBase =
            aspectService.save(AspectData("", "AspectBase", Kilometre.name, null, Decimal.name, emptyList()), username)
        val aspectProp = AspectPropertyData("", "  propTrim  ", aspectBase.idStrict(), PropertyCardinality.INFINITY.name, null)
        val ad = AspectData("", "  newAspectTrim   ", Kilometre.name, null, Decimal.name, listOf(aspectProp))
        val createAspect: AspectData = aspectService.save(ad, username)

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
        val name = "testAddAspectWithEmptyParams-newAspect"
        val ad = AspectData("", name, null, null, Decimal.name, emptyList())
        val createAspect: AspectData = aspectService.save(ad, username)

        assertThat(
            "aspect should be saved and restored event when some params are missing",
            aspectService.findByName(name).firstOrNull(),
            Is.`is`(createAspect)
        )
    }

    @Test
    fun testAddAspectWithEmptyParams2() {
        val ad = AspectData("", "testAddAspectWithEmptyParams2-newAspect", null, null, null, emptyList())
        assertThrows<AspectInconsistentStateException> {
            aspectService.save(ad, username)
        }
    }

    @Test
    fun testAddAspectWithEmptyParams3() {
        val name = "testAddAspectWithEmptyParams3-newAspect"
        val ad = AspectData("", name, Kilometre.name, null, Decimal.name, emptyList())
        val createAspect: AspectData = aspectService.save(ad, username)

        assertThat(
            "aspect should be saved and restored event when some params are missing",
            aspectService.findByName(name).firstOrNull(),
            Is.`is`(createAspect)
        )
    }

    @Test
    fun testFailAddAspect() {
        val ad = AspectData(
            "",
            "testFailAddAspect-newAspect",
            Kilometre.name,
            OpenDomain(Boolean).toString(),
            BaseType.Boolean.name,
            emptyList()
        )
        assertThrows<AspectInconsistentStateException> { aspectService.save(ad, username) }

    }

    @Test
    fun testAddTwoAspectsSameName() {
        val ad = AspectData("", "testAddTwoAspectsSameName-aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        aspectService.save(ad, username)

        val ad2 = AspectData("", "testAddTwoAspectsSameName-aspect", Metre.name, null, Decimal.name, emptyList(), 1)
        assertThrows<AspectAlreadyExist> {
            aspectService.save(ad2, username)
        }
        assertThat("should return two aspects with name 'aspect'", aspectService.findByName("aspect").size, Is.`is`(2))
    }

    @Test
    fun testAddTwoAspectsSameNameIgnoreCase() {
        val ad = AspectData("", "testAddTwoAspectsSameNameIgnoreCase-aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        aspectService.save(ad, username)

        val ad2 = AspectData("", "testAddTwoAspectsSameNameIgnoreCase-Aspect", Metre.name, null, Decimal.name, emptyList(), 1)
        assertThrows<AspectAlreadyExist> { aspectService.save(ad2, username) }
    }

    @Test
    fun testAddAspectsSameNameSameMeasure() {
        val ad1 = AspectData("", "testAddAspectsSameNameSameMeasure-aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        aspectService.save(ad1, username)

        val ad2 = AspectData("", "testAddAspectsSameNameSameMeasure-aspect", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        assertThrows<AspectAlreadyExist> { aspectService.save(ad2, username) }
    }

    @Test
    fun testAddAspectsDiffNameSameMeasure() {
        val ad1 = AspectData("", "testAddAspectsDiffNameSameMeasure-aspect", Kilometre.name, null, Decimal.name, emptyList())
        aspectService.save(ad1, username)

        val ad2 = AspectData("", "testAddAspectsDiffNameSameMeasure-aspect2", Kilometre.name, null, Decimal.name, emptyList())
        aspectService.save(ad2, username)

        assertThat(
            "should return two aspects with same measure",
            aspectService.getAspects().filter { it.measure == Kilometre.name }.filter { it.name.startsWith("testAddAspectsDiffNameSameMeasure") }.size,
            Is.`is`(2)
        )
    }

    @Test
    fun testUnCorrectMeasureBaseTypeRelations() {
        val ad = AspectData("", "testUnCorrectMeasureBaseTypeRelations-aspect", Kilometre.name, null, BaseType.Boolean.name, emptyList())
        assertThrows<AspectInconsistentStateException> { aspectService.save(ad, username) }
    }

    @Test
    fun testAspectWithoutCyclicDependency() {
        val aspect = prepareAspect("testAspectWithoutCyclicDependency")
        assertThat(
            "aspect should be saved and restored if no cyclic dependencies",
            aspectService.findByName("testAspectWithoutCyclicDependency-aspect").firstOrNull(),
            Is.`is`(aspect)
        )
    }


    @Test
    fun testAspectCyclicDependency() {
        val aspect = prepareAspect("testAspectCyclicDependency")
        val editedPropertyData1 = AspectPropertyData("", "prop1", aspect.idStrict(), PropertyCardinality.INFINITY.name, null)
        val aspect1 = aspectService.findById(aspect.properties.first().aspectId)
        val editedAspectData1 = AspectData(
            aspect1.id,
            "aspect1",
            Metre.name,
            null,
            Decimal.name,
            aspect1.properties.plus(editedPropertyData1),
            aspect1.version
        )
        assertThrows<AspectCyclicDependencyException> {
            aspectService.save(editedAspectData1, username)
        }
    }

    @Test
    fun testDoubleSaveNoSubject() {
        val aspectData = aspectDataWithSubject("testDoubleSaveNoSubject-AspectDoubleSaveNoSubject")
        aspectService.save(aspectData, username)
        try {
            aspectService.save(aspectData, username)
            Assert.fail("Nothing thrown")
        } catch (e: AspectAlreadyExist) {

        } catch (e: Throwable) {
            Assert.fail("Thrown unexpected $e")
        }
    }

    @Test
    fun testDoubleSaveSameSubject() {
        val aspectData = aspectDataWithSubject(
            aspectName = "testDoubleSaveSameSubject-AspectDoubleSaveSameSubject",
            subjectName = "testDoubleSaveSameSubject-SubjectDoubleSaveSameSubject"
        )
        aspectService.save(aspectData, username)
        assertThrows<AspectAlreadyExist> { aspectService.save(aspectData, username) }
    }

    @Test
    fun testDoubleSaveWithAndWithoutSubject() {
        val aspectData1 = aspectDataWithSubject("testDoubleSaveWithAndWithoutSubject-AspectDoubleSaveWithAndWithoutSubject")
        val aspectData2 = aspectDataWithSubject(
            aspectName = "testDoubleSaveWithAndWithoutSubject-AspectDoubleSaveWithAndWithoutSubject",
            subjectName = "testDoubleSaveWithAndWithoutSubject-SubjectDoubleSaveWithAndWithoutSubject"
        )

        val aspect1 = aspectService.save(aspectData1, username)
        val aspect2 = aspectService.save(aspectData2, username)

        Assert.assertEquals("first subject is incorrect", null, aspect1.subject)
        Assert.assertEquals(
            "second subject is incorrect",
            aspectData2.subject?.copy(version = 2),
            aspect2.subject
        )
    }

    @Test
    fun testDoubleSaveWithoutAndWithSubject() {
        val aspectData1 = aspectDataWithSubject(
            aspectName = "testDoubleSaveWithoutAndWithSubject-AspectDoubleSaveWithoutAndWithSubject",
            subjectName = "testDoubleSaveWithoutAndWithSubject-SubjectDoubleSaveWithoutAndWithSubject"
        )
        val aspectData2 = aspectDataWithSubject("AspectDoubleSaveWithoutAndWithSubject")

        val aspect1 = aspectService.save(aspectData1, username)
        val aspect2 = aspectService.save(aspectData2, username)

        Assert.assertEquals(
            "first subject is incorrect",
            aspectData1.subject?.copy(version = 2), aspect1.subject
        )
        Assert.assertEquals("second subject is incorrect", null, aspect2.subject)
    }

    @Test
    fun testDoubleSaveDifferentSubjects() {
        val aspectData1 = aspectDataWithSubject(
            aspectName = "testDoubleSaveDifferentSubjects-AspectDoubleSaveDifferentSubjects",
            subjectName = "testDoubleSaveDifferentSubjects-SubjectDoubleSaveDifferentSubjects"
        )
        val aspectData2 = aspectDataWithSubject(
            aspectName = "testDoubleSaveDifferentSubjects-AspectDoubleSaveDifferentSubjects",
            subjectName = "testDoubleSaveDifferentSubjects-OtherSubjectDoubleSaveDifferentSubjects"
        )

        val aspect1 = aspectService.save(aspectData1, username)
        val aspect2 = aspectService.save(aspectData2, username)

        Assert.assertEquals(
            "first subject is incorrect",
            aspectData1.subject?.copy(version = 2), aspect1.subject
        )
        Assert.assertEquals(
            "second subject is incorrect",
            aspectData2.subject?.copy(version = 2), aspect2.subject
        )
    }

    @Test
    fun testTripleSaveNoSomeOther() {
        val aspectData1 = aspectDataWithSubject("testTripleSaveNoSomeOther-AspectTripleSaveNoSomeOther")
        val aspectData2 = aspectDataWithSubject(
            aspectName = "testTripleSaveNoSomeOther-AspectTripleSaveNoSomeOther",
            subjectName = "testTripleSaveNoSomeOther-SomeSubjectTripleSaveNoSomeOther"
        )
        val aspectData3 = aspectDataWithSubject(
            aspectName = "testTripleSaveNoSomeOther-AspectTripleSaveNoSomeOther",
            subjectName = "testTripleSaveNoSomeOther-OtherSubjectTripleSaveNoSomeOther"
        )

        val aspect1 = aspectService.save(aspectData1, username)
        val aspect2 = aspectService.save(aspectData2, username)
        val aspect3 = aspectService.save(aspectData3, username)

        Assert.assertEquals("first subject is incorrect", null, aspect1.subject)
        Assert.assertEquals(
            "second subject is incorrect",
            aspectData2.subject?.copy(version = 2), aspect2.subject
        )
        Assert.assertEquals(
            "third subject is incorrect",
            aspectData3.subject?.copy(version = 2), aspect3.subject
        )
    }

    @Test
    fun testTripleSaveSomeNoOther() {
        val aspectData1 = aspectDataWithSubject(
            aspectName = "testTripleSaveSomeNoOther-AspectTripleSaveSomeNoOther",
            subjectName = "testTripleSaveSomeNoOther-SomeSubjectTripleSaveSomeNoOther"
        )
        val aspectData2 = aspectDataWithSubject("testTripleSaveSomeNoOther-AspectTripleSaveSomeNoOther")
        val aspectData3 = aspectDataWithSubject(
            aspectName = "testTripleSaveSomeNoOther-AspectTripleSaveSomeNoOther",
            subjectName = "testTripleSaveSomeNoOther-OtherSubjectTripleSaveSomeNoOther"
        )

        val aspect1 = aspectService.save(aspectData1, username)
        val aspect2 = aspectService.save(aspectData2, username)
        val aspect3 = aspectService.save(aspectData3, username)

        Assert.assertEquals(
            "first subject is incorrect", aspectData1.subject?.copy(version = 2),
            aspect1.subject
        )
        Assert.assertEquals("second subject is incorrect", null, aspect2.subject)
        Assert.assertEquals(
            "third subject is incorrect", aspectData3.subject?.copy(version = 2),
            aspect3.subject
        )
    }

    @Test
    fun testTripleSaveSomeOtherNo() {
        val aspectData1 = aspectDataWithSubject(
            aspectName = "testTripleSaveSomeOtherNo-AspectTripleSaveSomeOtherNo",
            subjectName = "testTripleSaveSomeOtherNo-SomeSubjectTripleSaveSomeOtherNo"
        )
        val aspectData2 = aspectDataWithSubject(
            aspectName = "testTripleSaveSomeOtherNo-AspectTripleSaveSomeOtherNo",
            subjectName = "testTripleSaveSomeOtherNo-OtherSubjectTripleSaveSomeOtherNo"
        )
        val aspectData3 = aspectDataWithSubject("AspectTripleSaveSomeOtherNo")

        val aspect1 = aspectService.save(aspectData1, username)
        val aspect2 = aspectService.save(aspectData2, username)
        val aspect3 = aspectService.save(aspectData3, username)

        Assert.assertEquals(
            "first subject is incorrect",
            aspectData1.subject?.copy(version = 2), aspect1.subject
        )
        Assert.assertEquals(
            "second subject is incorrect",
            aspectData2.subject?.copy(version = 2), aspect2.subject
        )
        Assert.assertEquals("third subject is incorrect", null, aspect3.subject)
    }

    @Test
    fun testCreateAspectSameNameWithSpaces() {
        val aspectData1 = aspectDataWithSubject("test")
        val aspectData2 = aspectDataWithSubject("test  ")
        aspectService.save(aspectData1, username)
        assertThrows<AspectAlreadyExist> { aspectService.save(aspectData2, username) }
    }

    @Test
    fun testUpdateAspectSameNameWithSpaces() {
        val aspectData1 = aspectDataWithSubject("test ")
        val aspectData2 = aspectDataWithSubject("test2")
        aspectService.save(aspectData1, username)
        val ans = aspectService.save(aspectData2, username)
        assertThrows<AspectAlreadyExist> { aspectService.save(ans.copy(name = "test   "), username) }
    }

    @Test
    fun testSaveWithAroundSpaces() {
        val leafAspect = aspectService.save(AspectData(name = "testSaveWithAroundSpaces-leaf", baseType = BaseType.Text.name), username)
        val aspectPropertyData =
            AspectPropertyData(id = "", name = "   p1   ", aspectId = leafAspect.id!!, cardinality = PropertyCardinality.ONE.name, description = "  d1   ")
        val complexAspect =
            AspectData(
                name = "     testSaveWithAroundSpaces-test ",
                description = " description    ",
                baseType = BaseType.Text.name,
                properties = listOf(aspectPropertyData)
            )
        val res = aspectService.save(complexAspect, username)
        assertEquals(res.name, "testSaveWithAroundSpaces-test", "Aspect should save with trimmed name")
        assertEquals(res.description, "description", "Aspect should save with trimmed description")
        assertEquals(res.properties[0].name, "p1", "Aspect property should save with trimmed name")
        assertEquals(res.properties[0].description, "d1", "Aspect property should save with trimmed description")
    }

    private fun prepareAspect(testName: String): AspectData {
        /*
         *  aspect
         *    aspectProperty
         *       aspect1
         *          aspectProperty1
         *              aspect2
         */

        val aspectData2 = AspectData(null, "$testName-aspect2", Kilogram.name, null, Decimal.name, emptyList())
        val aspect2: AspectData = aspectService.save(aspectData2, username)

        val aspectPropertyData1 = AspectPropertyData("", "prop1", aspect2.idStrict(), PropertyCardinality.INFINITY.name, null)
        val aspectData1 = AspectData(null, "$testName-aspect1", Metre.name, null, Decimal.name, listOf(aspectPropertyData1))
        val aspect1: AspectData = aspectService.save(aspectData1, username)

        val aspectPropertyData = AspectPropertyData("", "prop", aspect1.idStrict(), PropertyCardinality.INFINITY.name, null)
        val aspectData = AspectData(null, "$testName-aspect", Metre.name, null, Decimal.name, listOf(aspectPropertyData))
        return aspectService.save(aspectData, username)
    }

    private fun aspectDataWithSubject(aspectName: String, subjectName: String? = null): AspectData {
        val subjectData: SubjectData? = subjectName?.let {
            subjectService.createSubject(SubjectData(name = it, description = "some description"), username)
                .toSubjectData()
        }
        return AspectData(
            null, aspectName, Kilogram.name, null, Decimal.name, emptyList(),
            subject = subjectData
        )
    }
}