package com.infowings.catalog.data.subject

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.assertGreater
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.BaseType
import com.infowings.catalog.common.EventType
import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.data.history.HistorySnapshot
import com.infowings.catalog.data.history.providers.SubjectHistoryProvider
import com.infowings.catalog.data.toSubjectData
import com.infowings.catalog.search.SuggestionService
import com.infowings.catalog.storage.SUBJECT_CLASS
import org.junit.Before
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
class SubjectDaoTest {
    private val username = "admin"

    @Autowired
    lateinit var subjectDao: SubjectDao

    @Autowired
    lateinit var subjectService: SubjectService

    @Autowired
    lateinit var aspectService: AspectService


    @Before
    fun initTestData() {
    }

    @Test
    fun testSubjectsByIdsOne() {
        val subject =
            subjectService.createSubject(SubjectData(name = "subj", description = "some description"), username)

        val subjectVertices: List<SubjectVertex> = subjectDao.findByIds(listOf(subject.id))

        assertEquals(1, subjectVertices.size)
    }

    @Test
    fun testSubjectsByIdsEmptyResult() {
        val aspectName = "aspect"
        val aspectDescr = "aspect description"
        val created = aspectService.save(AspectData(id = "", name = aspectName, description = aspectDescr, version = 0, deleted = false, baseType = BaseType.Decimal.name), username)
        val aspectId = created.id ?: throw IllegalStateException("aspect id is null")

        val subject =
            subjectService.createSubject(SubjectData(name = "subj", description = "some description"), username)

        val subjectVertices: List<SubjectVertex> = subjectDao.findByIds(listOf(aspectId))

        assertEquals(0, subjectVertices.size)
    }

    @Test
    fun testSubjectsByIdsTwo() {
        val subject1 =
            subjectService.createSubject(SubjectData(name = "subj1", description = "some description-1"), username)
        val subject2 =
            subjectService.createSubject(SubjectData(name = "subj2", description = null), username)

        val subjectVertices: List<SubjectVertex> = subjectDao.findByIds(listOf(subject1.id, subject2.id))

        assertEquals(2, subjectVertices.size)
    }
}
