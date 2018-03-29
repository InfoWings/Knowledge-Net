package com.infowings.catalog.data.subject

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.*
import com.infowings.catalog.createTestAspect
import com.infowings.catalog.data.Subject
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectAlreadyExist
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
        val ad1 = AspectData(
            null,
            "aspect",
            Kilometre.name,
            null,
            BaseType.Decimal.name,
            emptyList(),
            subject = subject.toSubjectData()
        )
        aspectService.save(ad1)

        val ad2 = AspectData(
            null,
            "aspect",
            Metre.name,
            null,
            BaseType.Decimal.name,
            emptyList(),
            subject = subject.toSubjectData()
        )
        aspectService.save(ad2)
    }

    @Test
    fun testAddAspectsSameNameDiffSubject() {
        val subject1 = createTestSubject("TestSubjectUpdate")
        val subject2 = createTestSubject("TestSubjectUpdate1")
        val ad1 = AspectData(
            null,
            "DiffSubject",
            Kilometre.name,
            null,
            BaseType.Decimal.name,
            emptyList(),
            subject = subject1.toSubjectData()
        )
        val newAspect1 = aspectService.save(ad1)

        val ad2 = AspectData(
            null,
            "DiffSubject",
            Metre.name,
            null,
            BaseType.Decimal.name,
            emptyList(),
            subject = subject2.toSubjectData()
        )
        aspectService.save(ad2)
        Assert.assertThat(
            "aspect 'DiffSubject' should be saved and restored",
            aspectService.findByName("DiffSubject").firstOrNull(),
            Is.`is`(newAspect1)
        )

        Assert.assertThat(
            "aspect 'DiffSubject' should be saved and restored",
            aspectService.findByName("DiffSubject").firstOrNull(),
            Is.`is`(newAspect1)
        )
    }

    private fun createTestSubject(name: String, aspectNames: List<String> = listOf("TestSubjectAspect")): Subject =
        createTestSubject(name, aspectNames, aspectService, subjectService)
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
