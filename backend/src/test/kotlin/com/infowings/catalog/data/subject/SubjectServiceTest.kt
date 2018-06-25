package com.infowings.catalog.data.subject

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.*
import com.infowings.catalog.createTestAspect
import com.infowings.catalog.data.*
import com.infowings.catalog.data.aspect.AspectAlreadyExist
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.search.CommonSuggestionParam
import com.infowings.catalog.search.SubjectSuggestionParam
import com.infowings.catalog.search.SuggestionService
import org.hamcrest.core.Is
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import kotlin.test.assertEquals

@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SubjectServiceTest {
    private val username = "admin"

    @Autowired
    lateinit var subjectService: SubjectService

    @Autowired
    private lateinit var aspectService: AspectService

    @Autowired
    private lateinit var suggestionService: SuggestionService

    @Test(expected = AspectAlreadyExist::class)
    fun testAddAspectsSameNameSameSubject() {
        val subject = createTestSubject("TestSubjectUpdate")
        val ad1 = createTestAspect(subject = subject.toSubjectData())
        aspectService.save(ad1, username)

        val ad2 = createTestAspect(subject = subject.toSubjectData())
        aspectService.save(ad2, username)
    }

    @Test(expected = AspectAlreadyExist::class)
    fun testAddAspectsSameNameGlobalSubject() {
        val ad1 = createTestAspect()
        aspectService.save(ad1, username)

        val ad2 = createTestAspect()
        aspectService.save(ad2, username)
    }

    @Test
    fun testAddAspectsSameNameDiffSubject() {
        val subject1 = createTestSubject("TestSubjectUpdate1")
        val subject2 = createTestSubject("TestSubjectUpdate2")
        val aspectName = "aspectDiffSubject"
        val ad1 = createTestAspect(aspectName, subject = subject1.toSubjectData())
        val newAspect1 = aspectService.save(ad1, username)
        val ad2 = createTestAspect(aspectName, subject = subject2.toSubjectData())
        val newAspect2 = aspectService.save(ad2, username)
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
        aspectService.remove(aspectService.save(ad1, username).toAspectData(), username)

        val ad2 = createTestAspect(subject = subject.toSubjectData())
        aspectService.save(ad2, username)

        val aspects = aspectService.findByName("aspect")
        Assert.assertThat(
            "aspect should be saved",
            aspectService.findByName("aspect").firstOrNull(),
            Is.`is`(aspects.first())
        )
    }

    @Test
    fun testAddAspectsAfterRemoveForce() {
        /*
         *  aspectBase
         *    level1Property
         *       aspect
         */
        val subject = createTestSubject("TestSubjectUpdate")
        val aspect = aspectService.save(createTestAspect(subject = subject.toSubjectData()), username)
        val level1Property = AspectPropertyData("", "p_level1", aspect.id, PropertyCardinality.INFINITY.name, null)
        aspectService.save(
            createTestAspect(
                "aspectBase",
                subject = subject.toSubjectData(),
                properties = listOf(level1Property)
            ),
            username
        )

        aspectService.remove(aspectService.findByName("aspect").first().toAspectData(), username, true)

        val ad2 = createTestAspect(subject = subject.toSubjectData())
        aspectService.save(ad2, username)

        val aspects = aspectService.findByName("aspect")
        Assert.assertThat(
            "aspect should be saved",
            aspectService.findByName("aspect").firstOrNull(),
            Is.`is`(aspects.first())
        )
    }

    @Test
    fun testSuggestionByDescription() {
        val subject = createTestSubject(
            "testSuggestionByDescription",
            description = "The subject in a simple English sentence such as John runs, John is a teacher, or John was hit by a car is the person or thing about whom the statement is made, in this case 'John'."
        )
        val res = suggestionService.findSubject(
            CommonSuggestionParam(text = "in this case 'John'"),
            SubjectSuggestionParam(null)
        )
        Assert.assertEquals("subject should be founded by description", res.first(), subject.toSubjectData())
    }

    @Test
    fun testDeleteStandaloneSubject() {
        val subjects = (1..3).map {
            val name = "testDeleteStandaloneSubject$it"
            val s = createTestSubject(name, aspectNames = emptyList())
            Assert.assertEquals("createTestSubject returned unexpected subject", name, s.name)
            s
        }

        val before = subjectService.getSubjects().map { it.id }
        val beforeSet = before.toSet()

        Assert.assertEquals("ids are not unique", before.size, beforeSet.size)

        val toRemove = subjects[1]

        subjectService.remove(toRemove.toSubjectData(), username)

        val after = subjectService.getSubjects().map { it.id }
        val afterSet = after.toSet()

        Assert.assertEquals("ids after removal are not unique", after.size, afterSet.size)

        Assert.assertEquals("exactly one element should disappear", beforeSet.size, afterSet.size + 1)

        Assert.assertEquals("incorrect element was removed", beforeSet - afterSet, setOf(toRemove.id))
    }

    @Test
    fun testDeleteForcedReferencedSubject() {
        val nameBase = "testDeleteReferencedSubject"
        val subjects = (1..3).map {
            val name = "$nameBase$it"
            val s = createTestSubject(name, aspectNames = listOf("a_$name"))
            Assert.assertEquals("createTestSubject returned unexpected subject", name, s.name)
            s
        }

        val before = subjectService.getSubjects().map { it.id }
        val beforeSet = before.toSet()

        Assert.assertEquals("ids are not unique", before.size, beforeSet.size)

        val toRemove = subjects[1]

        subjectService.remove(toRemove.toSubjectData(), username, force = true)

        val after = subjectService.getSubjects().map { it.id }
        val afterSet = after.toSet()

        Assert.assertEquals("ids after removal are not unique", after.size, afterSet.size)

        Assert.assertEquals("exactly one element should disappear", beforeSet.size, afterSet.size + 1)

        Assert.assertEquals("incorrect element was removed", beforeSet - afterSet, setOf(toRemove.id))
    }

    @Test
    fun testDeleteReferencedSubject() {
        val nameBase = "testDeleteForceReferencedSubject"
        val subjects = (1..3).map {
            val name = "$nameBase$it"
            val s = createTestSubject(name, aspectNames = listOf("a_$name"))
            Assert.assertEquals("createTestSubject returned unexpected subject", name, s.name)
            s
        }

        val before = subjectService.getSubjects().map { it.id }
        val beforeSet = before.toSet()

        Assert.assertEquals("ids are not unique", before.size, beforeSet.size)

        val toRemove = subjects[1]

        try {
            subjectService.remove(toRemove.toSubjectData(), username, force = false)
            Assert.fail("Nothing is thrown")
        } catch (e: SubjectIsLinkedByAspect) {
        } catch (e: Throwable) {
            Assert.fail("Unexpected error is thrown: $e")
        }
    }

    @Test
    fun testUpdateSameData() {
        val created = createTestSubject("testSubject")
        try {
            subjectService.updateSubject(created.toSubjectData(), username)
        } catch (e: SubjectEmptyChangeException) {
        }
        val updated = subjectService.findByIdStrict(created.id)
        Assert.assertEquals("Same data shouldn't be rewritten", created.version, updated.version)
    }

    @Test(expected = SubjectWithNameAlreadyExist::class)
    fun testCreateSubjectWithSpaces() {
        subjectService.createSubject(SubjectData(name = "testSubject", description = ""), username)
        subjectService.createSubject(SubjectData(name = "testSubject ", description = ""), username)
    }

    @Test(expected = SubjectWithNameAlreadyExist::class)
    fun testUpdateSubjectWithSameName() {
        subjectService.createSubject(SubjectData(name = "testSubject", description = ""), username)
        val res = subjectService.createSubject(SubjectData(name = "testSubject2", description = ""), username)
        subjectService.updateSubject(res.toSubjectData().copy(name = "testSubject   "), username)
    }

    @Test
    fun testSaveAroundWithSpaces() {
        val res = subjectService.createSubject(SubjectData(name = "    test ", description = "  description   "), username)
        assertEquals("test", res.name, "Name should be saved without around spaces")
        assertEquals("description", res.description, "Description should be saved without around spaces")
    }

    private fun createTestSubject(
        name: String,
        aspectNames: List<String> = listOf("TestSubjectAspect"),
        description: String? = null
    ): Subject =
        createTestSubject(name, aspectNames, aspectService, subjectService, description)

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
    subjectService: SubjectService,
    description: String? = null
): Subject {
    val sd = SubjectData(name = name, version = 0, description = description)
    val subject = try {
        subjectService.createSubject(sd, "admin")
    } catch (e: SubjectWithNameAlreadyExist) {
        e.subject
    }
    aspectNames.map { createTestAspect(it, aspectService, subject) }

    return subjectService.findByIdStrict(subject.id).toSubject()
}
