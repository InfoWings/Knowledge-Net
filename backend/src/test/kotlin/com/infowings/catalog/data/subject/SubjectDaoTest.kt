package com.infowings.catalog.data.subject

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.assertGreater
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

}