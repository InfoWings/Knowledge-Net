package com.infowings.catalog.data.aspect

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.assertGreater
import com.infowings.catalog.common.*
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.data.history.providers.AspectHistoryProvider
import com.infowings.catalog.data.subject.SubjectDao
import com.infowings.catalog.data.toSubjectData
import com.infowings.catalog.search.SuggestionService
import com.infowings.catalog.storage.ASPECT_CLASS
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail

@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AspectHistoryTest {
    private val username = "admin"

    @Autowired
    lateinit var subjectService: SubjectService

    @Autowired
    lateinit var subjectDao: SubjectDao

    @Autowired
    private lateinit var aspectService: AspectService

    @Autowired
    private lateinit var suggestionService: SuggestionService

    @Autowired
    private lateinit var historyService: HistoryService

    @Autowired
    private lateinit var historyProvider: AspectHistoryProvider

    @Before
    fun initTestData() {
    }

    @Test
    fun testAspectHistoryEmpty() {
        val aspectHistory: List<AspectHistory> = historyProvider.getAllHistory()

        assertEquals(0, aspectHistory.size, "History must contain no elements")
    }

    @Test
    fun testAspectHistoryCreate() {
        /* Проверяем, что создание одного аспекта адекватно отражается в истории */
        val aspect = aspectService.save(AspectData(name = "aspect", baseType = BaseType.Decimal.name, description = "some description"), username)

        val aspectHistory: List<AspectHistory> = historyProvider.getAllHistory()

        assertEquals(1, aspectHistory.size, "History must contain 1 element")

        val aspectHistoryElement = aspectHistory[0]
        assertEquals(ASPECT_CLASS, aspectHistoryElement.entityName, "entity class is incorrect")
        assertNotNull(aspectHistoryElement.info, "session id must be non-null")
        assertGreater(aspectHistoryElement.timestamp, 0)
    }

    @Test
    fun testAspectHistoryCreateTwice() {
        /*  Проверяем, что создание двух разных аспектов адекватно отражается а истории */

        val aspect1 = aspectService.save(AspectData(name = "aspect-1", baseType = BaseType.Decimal.name, description = "some description-1"), username)
        val aspect2 = aspectService.save(AspectData(name = "aspect-2", baseType = BaseType.Decimal.name, description = "some description-2"), username)

        val aspectHistory: List<AspectHistory> = historyProvider.getAllHistory()

        assertEquals(2, aspectHistory.size, "History must contain 2 elements")

        val historyElement1 = aspectHistory[0]
        val historyElement2 = aspectHistory[1]

        assertGreater(historyElement1.timestamp, historyElement2.timestamp)
        aspectHistory.zip(listOf(aspect1, aspect2).reversed()).forEach {
            val historyElement = it.first
            val aspect = it.second

            assertEquals(ASPECT_CLASS, historyElement.entityName, "entity class is incorrect for $historyElement")
            assertEquals(aspect.name, historyElement.info, "entity id must correspond with id of added aspect")
        }
    }

    @Test
    fun testAspectHistoryCreateUpdate() {
        /*  Проверяем, что создание аспекта с последующим изменением адекватно отражается а истории */

        val subject1 = subjectService.createSubject(SubjectData(name = "subj1", description = "some description-1"), username)
        val aspect1 = aspectService.save(AspectData(name = "aspect-1", baseType = BaseType.Decimal.name, description = "some description-1"), username)
        val aspect2 = aspectService.save(aspect1.toAspectData().copy(description = "other"), username = "admin")

        val aspectHistory: List<AspectHistory> = historyProvider.getAllHistory()

        assertEquals(2, aspectHistory.size, "History must contain 2 elements")

        val historyElement1 = aspectHistory[0]
        val historyElement2 = aspectHistory[1]

        assertGreater(historyElement1.timestamp, historyElement2.timestamp)
        assertGreater(historyElement1.version, historyElement2.version)
    }

    @Test
    fun testAspectHistoryCreateAddProperty() {
        val aspect1 = aspectService.save(AspectData(name = "aspect-1", baseType = BaseType.Decimal.name, description = "some description-1"), username)
        val aspect2 = aspectService.save(AspectData(name = "aspect-2", baseType = BaseType.Decimal.name, description = "some description-2"), username)
        val aspect3 = aspectService.save(
            aspect1.toAspectData().copy(properties = listOf(AspectPropertyData(id = "", name = "prop", aspectId = aspect2.id,
                cardinality = AspectPropertyCardinality.INFINITY.name, description = null))), username = "admin")

        val aspectHistory: List<AspectHistory> = historyProvider.getAllHistory()
        assertEquals(3, aspectHistory.size, "it must be 3 events")
        assertEquals(2, aspectHistory.filter { it.eventType == EventType.CREATE }.size, "it must be 2 creation events")
        assertEquals(1, aspectHistory.filter { it.eventType == EventType.UPDATE }.size, "it must be 1 update event")
    }

    @Test
    fun testAspectHistoryUpdateProperty() {
        val aspect1 = aspectService.save(AspectData(name = "aspect-1", baseType = BaseType.Decimal.name, description = "some description-1"), username)
        val aspect2 = aspectService.save(AspectData(name = "aspect-2", baseType = BaseType.Decimal.name, description = "some description-2"), username)
        val aspect3 = aspectService.save(
            aspect1.toAspectData().copy(properties = listOf(AspectPropertyData(id = "", name = "prop", aspectId = aspect2.id,
                cardinality = AspectPropertyCardinality.INFINITY.name, description = null))), username = "admin")
        aspectService.save(aspect3.copy(properties = aspect3.properties.map {it.copy(description = "description")}).toAspectData(), "admin")


        val aspectHistory: List<AspectHistory> = historyProvider.getAllHistory()


        assertEquals(4, aspectHistory.size, "it must be 4 elements")
    }

    @Test
    fun testAspectHistoryRemovedSubject() {
        val subject = subjectService.createSubject(SubjectData(name = "subject-1", description = "subject description"), username).toSubjectData()
        val aspect1 = aspectService.save(
            AspectData(name = "aspect-1", baseType = BaseType.Decimal.name, description = "some description-1", subject = subject), username)
        val aspect2 = aspectService.save(aspect1.toAspectData().copy(subject = null), username)

        val subjectId = subject.id
        if (subjectId == null) {
            fail("id must be non-null")
        } else {
            val subjectVertex = subjectService.findById(subjectId)

            subjectService.remove(subject, username, force = false)

            val history = historyProvider.getAllHistory()

            assertEquals(2, history.size, "it must be 2 elements")
        }
    }

    @Test
    fun testAspectHistoryRemovedSubject2() {
        val subject = subjectService.createSubject(SubjectData(name = "subject-1", description = "subject description"), username).toSubjectData()
        val aspect1 = aspectService.save(
            AspectData(name = "aspect-1", baseType = BaseType.Decimal.name, description = "some description-1", subject = subject), username)
        val aspect2 = aspectService.save(aspect1.toAspectData().copy(subject = null), username)

        val subjectId = subject.id
        if (subjectId == null) {
            fail("id must be non-null")
        } else {
            val subjectVertex = subjectService.findById(subjectId)

            subjectDao.remove(subjectVertex!!)

            val history = historyProvider.getAllHistory()
            assertEquals(2, history.size, "it must be 2 elements")
        }
    }
}
