package com.infowings.catalog.data.history

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.assertGreater
import com.infowings.catalog.common.*
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.history.providers.AspectHistoryProvider
import com.infowings.catalog.data.subject.SubjectDao
import com.infowings.catalog.data.toSubjectData
import com.infowings.catalog.search.SuggestionService
import com.infowings.catalog.storage.ASPECT_CLASS
import com.infowings.catalog.storage.ASPECT_PROPERTY_CLASS
import com.infowings.catalog.storage.SUBJECT_CLASS
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.fail

@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class HistoryDaoTest {
    private val username = "admin"

    @Autowired
    lateinit var subjectService: SubjectService

    @Autowired
    private lateinit var aspectService: AspectService

    @Autowired
    private lateinit var historyDao: HistoryDao

    @Before
    fun initTestData() {
    }

    @Test
    fun testHistoryDaoEmpty() {
        val events = historyDao.getAllHistoryEventsByTime()

        assertEquals(0, events.size, "History must contain no elements")
    }

    @Test
    fun testHistoryDaoSubject() {
        val subjectName = "subject"
        val subjectDescr = "subject description"
        val created = subjectService.createSubject(SubjectData(id = "", name = subjectName, description = subjectDescr, version = 0, deleted = false), username)

        val events = historyDao.getAllHistoryEventsByTime()
        val subjectEvents = historyDao.getAllHistoryEventsByTime(SUBJECT_CLASS)

        assertEquals(1, events.size)
        assertEquals(1, subjectEvents.size)
    }

    @Test
    fun testHistoryDaoAspect() {
        val aspectName = "aspect"
        val aspectDescr = "aspect description"
        val created = aspectService.save(AspectData(id = "", name = aspectName, description = aspectDescr, version = 0, deleted = false, baseType = BaseType.Decimal.name), username)

        val events = historyDao.getAllHistoryEventsByTime()
//        val subjectEvents = historyDao.getAllHistoryEventsByTime(SUBJECT_CLASS)

        assertEquals(1, events.size)
  //      assertEquals(1, subjectEvents.size)
    }
}
