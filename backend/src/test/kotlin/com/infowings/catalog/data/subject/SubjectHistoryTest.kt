package com.infowings.catalog.data.subject

import com.infowings.catalog.common.EventType
import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.data.history.HistorySnapshot
import com.infowings.catalog.data.history.providers.SubjectHistoryProvider
import com.infowings.catalog.data.toSubjectData
import com.infowings.catalog.storage.SUBJECT_CLASS
import io.kotlintest.matchers.beGreaterThanOrEqualTo
import io.kotlintest.should
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*
import kotlin.test.assertEquals

@ExtendWith(SpringExtension::class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD, methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
class SubjectHistoryTest {
    private val username = "admin"

    @Autowired
    lateinit var subjectService: SubjectService

    @Autowired
    private lateinit var historyService: HistoryService

    private lateinit var historyProvider: SubjectHistoryProvider

    @BeforeEach
    fun initTestData() {
        historyProvider = SubjectHistoryProvider(historyService)
    }

    @Test
    @Disabled
    fun testSubjectHistoryEmpty() {
        val subjectHistory: List<HistorySnapshot> = historyProvider.getAllHistory()

        assertEquals(0, subjectHistory.size, "History must contain no elements")
    }


    @Test
    fun testSubjectHistoryCreate() {
        val subjectHistoryBefore: List<HistorySnapshot> = historyProvider.getAllHistory()

        val subject =
            subjectService.createSubject(SubjectData(name = "subj" + UUID.randomUUID().toString(), description = "some description"), username)

        val subjectHistory: List<HistorySnapshot> = historyProvider.getAllHistory().drop(subjectHistoryBefore.size)

        assertEquals(1, subjectHistory.size, "History must contain 1 element")

        val historySnaphot = subjectHistory[0]
        assertEquals(SUBJECT_CLASS, historySnaphot.event.entityClass, "entity class is incorrect")
    }

    @Test
    @Suppress("MagicNumber")
    @Disabled
    fun testSubjectHistoryCreateTwice() {
        val before: List<HistorySnapshot> = historyProvider.getAllHistory()

        val subject1 =
            subjectService.createSubject(SubjectData(name = "subj" + UUID.randomUUID().toString(), description = "some description-1"), username)
        Thread.sleep(10)
        val subject2 =
            subjectService.createSubject(SubjectData(name = "subj" + UUID.randomUUID().toString(), description = "some description-2"), username)

        val subjectHistory: List<HistorySnapshot> = historyProvider.getAllHistory().drop(before.size)

        assertEquals(2, subjectHistory.size, "History must contain 2 elements")

        val snapshot1 = subjectHistory[0]
        val snapshot2 = subjectHistory[1]

        snapshot1.event.timestamp should beGreaterThanOrEqualTo(snapshot2.event.timestamp)
        assertEquals(1, snapshot1.event.version, "version must be 1")
        assertEquals(1, snapshot2.event.version, "version must be 1")
    }

    @Test
    fun testSubjectHistoryUpdate() {
        val before: List<HistorySnapshot> = historyProvider.getAllHistory()
        val subject1 = subjectService.createSubject(SubjectData(name = "subj${UUID.randomUUID()}", description = "some description-1"), username)
        val subject2 = subjectService.updateSubject(subject1.copy(name = "subj${UUID.randomUUID()}", description = "new description").toSubjectData(), username)

        val subjectHistory: List<HistorySnapshot> = historyProvider.getAllHistory().drop(before.size)

        assertEquals(2, subjectHistory.size, "History must contain 2 elements")

        val snapshot1 = subjectHistory[0]
        val snapshot2 = subjectHistory[1]

        snapshot1.event.timestamp should beGreaterThanOrEqualTo(snapshot2.event.timestamp)
        assertEquals(2, snapshot1.event.version, "version must be 2")
        assertEquals(1, snapshot2.event.version, "version must be 1")
        assertEquals(EventType.UPDATE, snapshot1.event.type, "version must be 2")
        assertEquals(setOf("name", "description", "guid"), snapshot1.after.data.keys, "data keys must be correct")
        assertEquals(subject2.name, snapshot1.after.data["name"], "name must be correct")
        assertEquals(subject2.description, snapshot1.after.data["description"], "description must be correct")
    }
}