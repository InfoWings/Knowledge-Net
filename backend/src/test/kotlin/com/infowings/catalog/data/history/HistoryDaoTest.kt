package com.infowings.catalog.data.history

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.BaseType
import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.reference.book.REFERENCE_BOOK_ITEM_VERTEX
import com.infowings.catalog.data.reference.book.ReferenceBookService
import com.infowings.catalog.storage.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.test.assertEquals

@ExtendWith(SpringExtension::class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS, methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
class HistoryDaoTest {
    private val username = "admin"

    @Autowired
    lateinit var subjectService: SubjectService

    @Autowired
    private lateinit var aspectService: AspectService

    @Autowired
    private lateinit var refBookService: ReferenceBookService

    @Autowired
    private lateinit var historyDao: HistoryDao

    val classes =
        listOf(SUBJECT_CLASS, ASPECT_CLASS, ASPECT_PROPERTY_CLASS, REFERENCE_BOOK_ITEM_VERTEX, OBJECT_CLASS, OBJECT_PROPERTY_CLASS, OBJECT_PROPERTY_VALUE_CLASS)

    @Test
    fun testHistoryDaoEmpty() {
        val events = historyDao.getAllHistoryEventsByTime()
        assertEquals(0, events.size, "History must contain no elements")

        classes.forEach {
            val classEvents = historyDao.getAllHistoryEventsByTime(it)
            assertEquals(0, classEvents.size, "class: $it")
        }
    }

    @Test
    fun testHistoryDaoSubject() {
        val subjectName = "testHistoryDaoSubject subject"
        val subjectDescr = "subject description"
        val created = subjectService.createSubject(SubjectData(id = "", name = subjectName, description = subjectDescr, version = 0, deleted = false), username)

        val events = historyDao.getAllHistoryEventsByTime()
        val subjectEvents = historyDao.getAllHistoryEventsByTime(SUBJECT_CLASS)
        val subjectEventsL = historyDao.getAllHistoryEventsByTime(listOf(SUBJECT_CLASS))

        assertEquals(1, events.size)
        assertEquals(1, subjectEvents.size)
        assertEquals(1, subjectEventsL.size)

        assertEquals(events, subjectEvents)

        classes.minus(SUBJECT_CLASS).forEach {
            val classEvents = historyDao.getAllHistoryEventsByTime(it)
            val classEventsL = historyDao.getAllHistoryEventsByTime(listOf(it))
            assertEquals(0, classEvents.size, "class: $it")
            assertEquals(0, classEventsL.size, "class: $it")
        }

        classes.minus(SUBJECT_CLASS).forEach {
            val classEvents = historyDao.getAllHistoryEventsByTime(listOf(it, SUBJECT_CLASS))
            assertEquals(1, classEvents.size, "class: $it")
        }
    }

    @Test
    fun testHistoryDaoAspect() {
        val aspectName = "testHistoryDaoAspect aspect"
        val aspectDescr = "aspect description"
        val created = aspectService.save(
            AspectData(
                id = "",
                name = aspectName,
                description = aspectDescr,
                version = 0,
                deleted = false,
                baseType = BaseType.Decimal.name
            ), username
        )

        val events = historyDao.getAllHistoryEventsByTime()
        val aspectEvents = historyDao.getAllHistoryEventsByTime(ASPECT_CLASS)
        val aspectEventsL = historyDao.getAllHistoryEventsByTime(listOf(ASPECT_CLASS))

        assertEquals(1, events.size)
        assertEquals(1, aspectEvents.size)
        assertEquals(1, aspectEventsL.size)

        classes.minus(ASPECT_CLASS).forEach {
            val classEvents = historyDao.getAllHistoryEventsByTime(it)
            val classEventsL = historyDao.getAllHistoryEventsByTime(listOf(it))
            assertEquals(0, classEvents.size, "class: $it")
            assertEquals(0, classEventsL.size, "class: $it")
        }

        classes.minus(ASPECT_CLASS).forEach {
            val classEvents = historyDao.getAllHistoryEventsByTime(listOf(ASPECT_CLASS, it))
            assertEquals(1, classEvents.size, "class: $it")
        }
    }

    @Test
    fun testHistoryDaoRefBook() {
        val aspectName = "testHistoryDaoRefBook aspect"
        val aspectDescr = "aspect description"
        val created = aspectService.save(
            AspectData(
                id = "", name = aspectName, description = aspectDescr,
                version = 0, deleted = false, baseType = BaseType.Text.name
            ), username
        )
        val aspectId = created.id ?: throw IllegalStateException("aspect id is null")

        val rbName = "rb"
        val refBook = refBookService.createReferenceBook(name = rbName, aspectId = aspectId, username = username)

        val events = historyDao.getAllHistoryEventsByTime()
        val aspectEvents = historyDao.getAllHistoryEventsByTime(ASPECT_CLASS)
        val refBookEvents = historyDao.getAllHistoryEventsByTime(REFERENCE_BOOK_ITEM_VERTEX)

        assertEquals(3, events.size)
        assertEquals(2, aspectEvents.size)
        assertEquals(1, refBookEvents.size)

        val aspectAndRefBookEvents = historyDao.getAllHistoryEventsByTime(listOf(ASPECT_CLASS, REFERENCE_BOOK_ITEM_VERTEX))
        assertEquals(3, aspectAndRefBookEvents.size)
    }
}
