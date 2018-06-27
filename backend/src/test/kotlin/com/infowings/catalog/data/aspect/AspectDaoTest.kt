package com.infowings.catalog.data.aspect

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.*
import com.infowings.catalog.common.BaseType.Boolean
import com.infowings.catalog.common.BaseType.Decimal
import com.infowings.catalog.common.BaseType.Text
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.data.reference.book.ReferenceBookService
import com.infowings.catalog.data.toSubjectData
import com.orientechnologies.orient.core.id.ORID
import com.orientechnologies.orient.core.id.ORecordId
import org.hamcrest.core.Is
import org.junit.Assert
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import kotlin.test.assert
import kotlin.test.assertEquals


@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)

class AspectDaoTest {
    private val username = "admin"

    @Autowired
    lateinit var aspectDao: AspectDaoService

    @Autowired
    lateinit var aspectService: AspectService

    @Autowired
    lateinit var subjectService: SubjectService

    @Autowired
    lateinit var refBookService: ReferenceBookService

    @Autowired
    lateinit var historyService: HistoryService

    lateinit var complexAspect: AspectData
    lateinit var baseAspect: AspectData

    /**
     * complexAspect
     *     -> property
     *             -> baseAspect
     */
    @Before
    fun initialize() {
        val ad = AspectData("", "base", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        baseAspect = aspectService.save(ad, username)

        val property = AspectPropertyData("", "p", baseAspect.idStrict(), PropertyCardinality.INFINITY.name, null)

        val ad2 = AspectData(
            "",
            "complex",
            Kilometre.name,
            null,
            BaseType.Decimal.name,
            listOf(property)
        )
        complexAspect = aspectService.save(ad2, username)
    }

    @Test
    fun testGetDetailsPlain() {
        val ad = AspectData("", "newAspect", Kilometre.name, null, Decimal.name, emptyList())
        val createdAspect: AspectData = aspectService.save(ad, username)

        val details: Map<String, AspectDaoDetails> = aspectDao.getDetails(listOf(ORecordId(createdAspect.id)))
        val aspectId = createdAspect.id ?: throw IllegalStateException("aspect id is null")

        assertEquals(setOf(aspectId), details.keys)

        val aspectDetails = details.getValue(aspectId)

        val aspectEvents = historyService.allTimeline().filter { it.event.entityId == aspectId }

        assertEquals(null, aspectDetails.subject)
        assertEquals(null, aspectDetails.refBookName)
        assertEquals(emptyList(), aspectDetails.propertyIds)
        assertEquals(aspectEvents.first().event.timestamp, aspectDetails.lastChange.toEpochMilli())
    }

    @Test
    fun testGetDetailsPlainTwo() {
        val ad1 = AspectData("", "newAspect", Kilometre.name, null, Decimal.name, emptyList())
        val ad2 = AspectData("", "newAspect-2", Kilometre.name, null, Decimal.name, emptyList())
        val created1: AspectData = aspectService.save(ad1, username)
        val created2: AspectData = aspectService.save(ad2, username)

        val details: Map<String, AspectDaoDetails> = aspectDao.getDetails(listOf(created1, created2).map { ORecordId(it.id) })
        val aspectId1 = created1.id ?: throw IllegalStateException("aspect id is null")
        val aspectId2 = created2.id ?: throw IllegalStateException("aspect id is null")

        assertEquals(setOf(aspectId1, aspectId2), details.keys)

        val details1 = details.getValue(aspectId1)
        val details2 = details.getValue(aspectId2)

        assertEquals(null, details1.subject)
        assertEquals(null, details2.subject)
        assertEquals(null, details1.refBookName)
        assertEquals(null, details2.refBookName)
        assertEquals(emptyList(), details1.propertyIds)
        assertEquals(emptyList(), details2.propertyIds)
    }

    @Test
    fun testGetDetailsPlainOneOfTwo() {
        val ad1 = AspectData("", "newAspect", Kilometre.name, null, Decimal.name, emptyList())
        val ad2 = AspectData("", "newAspect-2", Kilometre.name, null, Decimal.name, emptyList())
        val created1: AspectData = aspectService.save(ad1, username)
        val created2: AspectData = aspectService.save(ad2, username)

        val details: Map<String, AspectDaoDetails> = aspectDao.getDetails(listOf(created1).map { ORecordId(it.id) })
        val aspectId1 = created1.id ?: throw IllegalStateException("aspect id is null")
        val aspectId2 = created2.id ?: throw IllegalStateException("aspect id is null")

        assertEquals(setOf(aspectId1), details.keys)

        val details1 = details.getValue(aspectId1)
        assertEquals(null, details1.subject)
        assertEquals(null, details1.refBookName)
    }

    @Test
    fun testGetDetailsWithSubject() {
        val sd = SubjectData(id = "", name = "subject", description = "subject description", deleted = false, version = 0)
        val subject = subjectService.createSubject(sd, "admin")

        val ad = AspectData("", "newAspect", Kilometre.name, null, Decimal.name, emptyList(), 0, subject.toSubjectData())
        val createdAspect: AspectData = aspectService.save(ad, username)

        val details: Map<String, AspectDaoDetails> = aspectDao.getDetails(listOf(ORecordId(createdAspect.id)))
        val aspectId = createdAspect.id ?: throw IllegalStateException("aspect id is null")

        assertEquals(setOf(aspectId), details.keys)

        val aspectDetails = details.getValue(aspectId)

        assertEquals(subject.id, aspectDetails.subject?.id)
        /*assertEquals(null, aspectDetails.refBookName)
        */
    }

    @Test
    fun testGetDetailsWithRefBook() {
        val ad = AspectData("", "newAspect", null, null, Text.name, emptyList())
        val createdAspect: AspectData = aspectService.save(ad, username)
        val aspectId = createdAspect.id ?: throw IllegalStateException("aspect without id")
        val refBook = refBookService.createReferenceBook("rb", aspectId, username)

        val details: Map<String, AspectDaoDetails> = aspectDao.getDetails(listOf(ORecordId(createdAspect.id)))

        assertEquals(setOf(aspectId), details.keys)

        val aspectDetails = details.getValue(aspectId)

        assertEquals(null, aspectDetails.subject)
/*        assertEquals(null, aspectDetails.refBookName)
        */
    }

    @Test
    fun testGetDetailsWithProperty() {
        val baseId = baseAspect.id
        val details: Map<String, AspectDaoDetails> = aspectDao.getDetails(listOf(ORecordId(baseId)))

        assertEquals(setOf(baseId), details.keys)

//        val aspectDetails = details.getValue(aspectId)

//        assertEquals(null, aspectDetails.subject)
/*        assertEquals(null, aspectDetails.refBookName)
        */
    }
}