package com.infowings.catalog.data.subject

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.*
import com.infowings.catalog.createTestAspect
import com.infowings.catalog.data.Subject
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectAlreadyExist
import com.infowings.catalog.data.aspect.AspectPropertyCardinality
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.toSubjectData
import org.hamcrest.core.Is
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SubjectServiceTest {

    @Autowired
    lateinit var subjectService: SubjectService

    @Autowired
    private lateinit var aspectService: AspectService

    @Test(expected = AspectAlreadyExist::class)
    fun testAddAspectsSameNameSameSubject() {
        val subject = createTestSubject("TestSubjectUpdate")
        val ad1 = createTestAspect(subject = subject.toSubjectData())
        aspectService.save(ad1)

        val ad2 = createTestAspect(subject = subject.toSubjectData())
        aspectService.save(ad2)
    }

    @Test(expected = AspectAlreadyExist::class)
    fun testAddAspectsSameNameGlobalSubject() {
        val ad1 = createTestAspect()
        aspectService.save(ad1)

        val ad2 = createTestAspect()
        aspectService.save(ad2)
    }

    @Test
    fun testAddAspectsSameNameDiffSubject() {
        val subject1 = createTestSubject("TestSubjectUpdate1")
        val subject2 = createTestSubject("TestSubjectUpdate2")
        val aspectName = "aspectDiffSubject"
        val ad1 = createTestAspect(aspectName, subject = subject1.toSubjectData())
        val newAspect1 = aspectService.save(ad1)
        val ad2 = createTestAspect(aspectName, subject = subject2.toSubjectData())
        val newAspect2 = aspectService.save(ad2)
        aspectService.findByName(aspectName).forEach {
            if (it.subject?.name == subject1.name) {
                Assert.assertThat("aspect '$aspectName' should be saved", newAspect1, Is.`is`(it))
            }
            if (it.subject?.name == subject2.name) {
                Assert.assertThat("aspect '$aspectName' should be saved", newAspect2, Is.`is`(it))
            }
        }

    }

    @Test
    fun testAddAspectsAfterRemoveSameSubject() {
        val subject = createTestSubject("TestSubjectUpdate")
        val ad1 = createTestAspect(subject = subject.toSubjectData())
        aspectService.remove(aspectService.save(ad1).toAspectData())

        val ad2 = createTestAspect(subject = subject.toSubjectData())
        aspectService.save(ad2)

        val aspects = aspectService.findByName("aspect")
        Assert.assertThat(
            "aspect should be saved",
            aspectService.findByName("aspect").firstOrNull(),
            Is.`is`(aspects.first())
        )
    }

    @Test
    fun testAddAspectsAfterRemoveForceSameSubject() {
        /*
         *  aspectBase
         *    level1_property
         *       aspect
         */
        val subject = createTestSubject("TestSubjectUpdate")
        val aspect = aspectService.save(createTestAspect(subject = subject.toSubjectData()))
        val level1_property = AspectPropertyData("", "p_level1", aspect.id, AspectPropertyCardinality.INFINITY.name)
        aspectService.save(
            createTestAspect(
                "aspectBase",
                subject = subject.toSubjectData(),
                properties = listOf(level1_property)
            )
        )

        aspectService.remove(aspectService.findByName("aspect").first().toAspectData(), true)

        val ad2 = createTestAspect(subject = subject.toSubjectData())
        aspectService.save(ad2)

        val aspects = aspectService.findByName("aspect")
        Assert.assertThat(
            "aspect should be saved",
            aspectService.findByName("aspect").firstOrNull(),
            Is.`is`(aspects.first())
        )
    }


    private fun createTestSubject(name: String, aspectNames: List<String> = listOf("TestSubjectAspect")): Subject =
        createTestSubject(name, aspectNames, aspectService, subjectService)

    private fun createTestAspect(
        name: String = "aspect",
        measure: String = Kilometre.name,
        subject: SubjectData? = null,
        properties: List<AspectPropertyData> = emptyList()
    ) =
        AspectData(
            null,
            name,
            measure,
            null,
            BaseType.Decimal.name,
            properties,
            subject = subject
        )

}

fun createTestSubject(
    name: String,
    aspectNames: List<String> = listOf("TestSubjectAspect"),
    aspectService: AspectService,
    subjectService: SubjectService
): Subject {
    val sd = SubjectData(name = name)
    val subject = subjectService.findByName(name) ?: subjectService.createSubject(sd)
    aspectNames.map { createTestAspect(it, aspectService, subject) }
    return subject
}
