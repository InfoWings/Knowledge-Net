package com.infowings.catalog.data.objekt

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.ObjectCreateRequest
import com.infowings.catalog.data.MeasureService
import com.infowings.catalog.data.Subject
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.Aspect
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.history.HistoryFactDto
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.storage.OBJECT_CLASS
import com.infowings.catalog.storage.OrientDatabase
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
class ObjectHistoryTest {
    @Autowired
    private lateinit var db: OrientDatabase
    @Autowired
    private lateinit var dao: ObjectDaoService
    @Autowired
    private lateinit var subjectService: SubjectService
    @Autowired
    private lateinit var aspectService: AspectService
    @Autowired
    private lateinit var measureService: MeasureService
    @Autowired
    private lateinit var objectService: ObjectService
    @Autowired
    private lateinit var historyService: HistoryService

    private lateinit var subject: Subject

    private lateinit var aspect: Aspect

    private lateinit var complexAspect: Aspect

    private val username = "admin"

    @Before
    fun initTestData() {
        subject = subjectService.createSubject(SubjectData(name = "subjectName", description = "descr"), username)
        aspect = aspectService.save(
            AspectData(name = "aspectName", description = "aspectDescr", baseType = BaseType.Text.name), username
        )
        val property = AspectPropertyData("", "p", aspect.id, PropertyCardinality.INFINITY.name, null)
        val complexAspectData = AspectData(
            "",
            "complex",
            Kilometre.name,
            null,
            BaseType.Decimal.name,
            listOf(property)
        )
        complexAspect = aspectService.save(complexAspectData, username)
    }

    @Test
    fun createObjectHistoryTest() {
        val testName = "createObjectHistoryTest"

        val eventsBefore: Set<HistoryFactDto> = historyService.getAll().toSet()
        val objectEventsBefore = objectEvents(eventsBefore)
        val subjectEventsBefore = subjectEvents(eventsBefore)

        val request = ObjectCreateRequest(testName, "object descr", subject.id, subject.version)
        val created = objectService.create(request, "user")

        val eventsAfter = historyService.getAll().toSet()
        val objectEventsAfter = objectEvents(eventsAfter)
        val subjectEventsAfter = subjectEvents(eventsAfter)

        val objectEventsAdded = objectEventsAfter - objectEventsBefore
        val subjectEventsAdded = subjectEventsAfter - subjectEventsBefore

        assertEquals(1, objectEventsAdded.size, "exactly one object event must appear")
        val objectEvent = objectEventsAdded.firstOrNull()?.event
        assertEquals(OBJECT_CLASS, objectEvent?.entityClass, "class must be correct")
        assertEquals(EventType.CREATE, objectEvent?.type, "event type must be correct")

        assertEquals(1, subjectEventsAdded.size, "exactly one subject event must appear")
        val subjectEvent = subjectEventsAdded.firstOrNull()?.event
        assertEquals(SUBJECT_CLASS, subjectEvent?.entityClass, "class must be correct")
        assertEquals(EventType.UPDATE, subjectEvent?.type, "event type must be correct")
    }

    private fun eventsByClass(events: Set<HistoryFactDto>, entityClass: String) =
        events.filter { it.event.entityClass == entityClass }

    private fun objectEvents(events: Set<HistoryFactDto>) = eventsByClass(events, OBJECT_CLASS)

    private fun subjectEvents(events: Set<HistoryFactDto>) = eventsByClass(events, SUBJECT_CLASS)
}