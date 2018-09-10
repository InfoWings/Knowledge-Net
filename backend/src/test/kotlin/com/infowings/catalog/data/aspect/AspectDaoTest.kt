package com.infowings.catalog.data.aspect

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.BaseType
import com.infowings.catalog.common.BaseType.Decimal
import com.infowings.catalog.common.BaseType.Text
import com.infowings.catalog.common.Kilometre
import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.guid.GuidDaoService
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.data.reference.book.ReferenceBookService
import com.infowings.catalog.data.toSubjectData
import com.infowings.catalog.randomName
import com.infowings.catalog.storage.OrientClass
import com.orientechnologies.orient.core.id.ORecordId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.test.assertEquals

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [MasterCatalog::class])
class AspectDaoTest {
    private val username = "admin"

    @Autowired
    lateinit var aspectDao: AspectDaoService

    @Autowired
    lateinit var subjectService: SubjectService

    @Autowired
    lateinit var aspectService: AspectService

    @Autowired
    lateinit var refBookService: ReferenceBookService

    @Autowired
    lateinit var historyService: HistoryService

    @Autowired
    lateinit var guidDao: GuidDaoService

    @Test
    fun testFindAspectsByIdsOne() {
        val aspect =
            aspectService.save(AspectData(name = "aspect", description = "some description", baseType = BaseType.Text.name), username)
        val aspectId = aspect.id ?: throw IllegalStateException("")

        val aspectVertices = aspectDao.findAspectsByIdsStr(listOf(aspectId))

        assertEquals(1, aspectVertices.size)
    }

    @Test
    fun testGetDetailsPlain() {
        val ad = AspectData("", "testGetDetailsPlain-newAspect", Kilometre.name, null, Decimal.name, emptyList())
        val createdAspect: AspectData = aspectService.save(ad, username)

        val details: Map<String, AspectDaoDetails> = aspectDao.getDetails(listOf(ORecordId(createdAspect.id)))
        val aspectId = createdAspect.id ?: throw IllegalStateException("aspect id is null")

        assertEquals(setOf(aspectId), details.keys)

        val aspectDetails = details.getValue(aspectId)

        val aspectEvents = historyService.allTimeline(OrientClass.ASPECT.extName).filter { it.event.entityId == aspectId }

        assertEquals(null, aspectDetails.subject)
        assertEquals(null, aspectDetails.refBookName)
        assertEquals(emptyList(), aspectDetails.propertyIds)
        assertEquals(aspectEvents.first().event.timestamp, aspectDetails.lastChange.toEpochMilli())
    }

    @Test
    fun testGetDetailsPlainTwo() {
        val ad1 = AspectData("", "testGetDetailsPlainTwo-newAspect", Kilometre.name, null, Decimal.name, emptyList())
        val ad2 = AspectData("", "testGetDetailsPlainTwo-newAspect-2", Kilometre.name, null, Decimal.name, emptyList())
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

        val allEvents = historyService.allTimeline(OrientClass.ASPECT.extName)
        val events1 = allEvents.filter { it.event.entityId == aspectId1 }
        val events2 = allEvents.filter { it.event.entityId == aspectId2 }

        assertEquals(events1.first().event.timestamp, details1.lastChange.toEpochMilli())
        assertEquals(events2.first().event.timestamp, details2.lastChange.toEpochMilli())
    }

    @Test
    fun testGetDetailsPlainOneOfTwo() {
        val ad1 = AspectData("", "testGetDetailsPlainOneOfTwo-newAspect", Kilometre.name, null, Decimal.name, emptyList())
        val ad2 = AspectData("", "testGetDetailsPlainOneOfTwo-newAspect-2", Kilometre.name, null, Decimal.name, emptyList())
        val created1: AspectData = aspectService.save(ad1, username)
        val created2: AspectData = aspectService.save(ad2, username)

        val details: Map<String, AspectDaoDetails> = aspectDao.getDetails(listOf(created1).map { ORecordId(it.id) })
        val aspectId1 = created1.id ?: throw IllegalStateException("aspect id is null")
        val aspectId2 = created2.id ?: throw IllegalStateException("aspect id is null")

        assertEquals(setOf(aspectId1), details.keys)

        val details1 = details.getValue(aspectId1)
        assertEquals(null, details1.subject)
        assertEquals(null, details1.refBookName)
        assertEquals(emptyList(), details1.propertyIds)

        val allEvents = historyService.allTimeline(OrientClass.ASPECT.extName)
        val events1 = allEvents.filter { it.event.entityId == aspectId1 }
        assertEquals(events1.first().event.timestamp, details1.lastChange.toEpochMilli())
    }

    @Test
    fun testGetDetailsWithSubject() {
        val sd = SubjectData(id = "", name = "testGetDetailsWithSubject-subject", description = "subject description", deleted = false, version = 0)
        val subject = subjectService.createSubject(sd, "admin")

        val ad = AspectData("", "testGetDetailsWithSubject-newAspect", Kilometre.name, null, Decimal.name, emptyList(), 0, subject.toSubjectData())
        val createdAspect: AspectData = aspectService.save(ad, username)

        val details: Map<String, AspectDaoDetails> = aspectDao.getDetails(listOf(ORecordId(createdAspect.id)))
        val aspectId = createdAspect.id ?: throw IllegalStateException("aspect id is null")

        assertEquals(setOf(aspectId), details.keys)

        val aspectDetails = details.getValue(aspectId)

        assertEquals(subject.id, aspectDetails.subject?.id)
        assertEquals(subject.name, aspectDetails.subject?.name)
        assertEquals(null, aspectDetails.refBookName)
    }

    @Test
    fun testGetDetailsWithRefBook() {
        val ad = AspectData("", "testGetDetailsWithRefBook-newAspect", null, null, Text.name, emptyList())
        val createdAspect: AspectData = aspectService.save(ad, username)
        val aspectId = createdAspect.id ?: throw IllegalStateException("aspect without id")
        val refBook = refBookService.createReferenceBook("rb", aspectId, username)

        val details: Map<String, AspectDaoDetails> = aspectDao.getDetails(listOf(ORecordId(createdAspect.id)))

        assertEquals(setOf(aspectId), details.keys)

        val aspectDetails = details.getValue(aspectId)

        assertEquals(null, aspectDetails.subject)
        assertEquals(refBook.name, aspectDetails.refBookName)
        assertEquals(emptyList(), aspectDetails.propertyIds)
    }

    @Test
    fun testGetDetailsWithProperty() {
        val ad = AspectData("", "testGetDetailsWithProperty", Kilometre.name, null, BaseType.Decimal.name, emptyList())
        val baseAspect = aspectService.save(ad, username)

        val baseId = baseAspect.id ?: throw IllegalStateException("base aspect has no id")
        val details: Map<String, AspectDaoDetails> = aspectDao.getDetails(listOf(ORecordId(baseId)))

        assertEquals(setOf(baseId), details.keys)

        val aspectDetails = details.getValue(baseId)

        assertEquals(null, aspectDetails.subject)
        assertEquals(null, aspectDetails.refBookName)
    }

    @Test
    fun testGuidDaoUnique() {
        val aspect = aspectService.save(AspectData(name = randomName("aspect"), description = "some description", baseType = BaseType.Text.name), username)
        guidDao.find(listOfNotNull(aspect.guid))
    }
}
