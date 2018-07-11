package com.infowings.catalog.data.aspect

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.assertGreater
import com.infowings.catalog.common.*
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.history.HistoryDao
import com.infowings.catalog.data.history.HistoryFact
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.data.history.providers.AspectHistoryProvider
import com.infowings.catalog.data.reference.book.ReferenceBookService
import com.infowings.catalog.data.subject.SubjectDao
import com.infowings.catalog.data.toSubjectData
import com.infowings.catalog.storage.ASPECT_CLASS
import com.infowings.catalog.storage.ASPECT_PROPERTY_CLASS
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
class AspectHistoryTest {
    private val username = "admin"

    @Autowired
    lateinit var subjectService: SubjectService

    @Autowired
    lateinit var subjectDao: SubjectDao

    @Autowired
    private lateinit var aspectService: AspectService

    @Autowired
    private lateinit var refBookService: ReferenceBookService

    @Autowired
    private lateinit var historyService: HistoryService

    @Autowired
    private lateinit var historyProvider: AspectHistoryProvider

    @Autowired
    private lateinit var historyDao: HistoryDao

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
        val aspect = aspectService.save(
            AspectData(
                name = "aspect",
                baseType = BaseType.Decimal.name,
                description = "some description"
            ), username
        )

        val aspectHistory: List<AspectHistory> = historyProvider.getAllHistory()

        assertEquals(1, aspectHistory.size, "History must contain 1 element")

        val aspectHistoryElement = aspectHistory[0]
        assertEquals(ASPECT_CLASS, aspectHistoryElement.event.entityClass, "entity class is incorrect")
        assertEquals(aspect.id, aspectHistoryElement.event.entityId, "entity id is incorrect")
        assertNotNull(aspectHistoryElement.event.sessionId, "session id must be non-null")
        assertGreater(aspectHistoryElement.event.timestamp, 0)

        val timeline = historyDao.timelineForEntity(aspect.idStrict())
        println("timeline: " + timeline)
        assertEquals(3, aspectHistoryElement.changes.size)
        val changedFields = aspectHistoryElement.changes.groupBy { it.fieldName }
        assertEquals(setOf("Name", "Base type", "Description"), changedFields.keys)
        val nameChange = changedFields.getValue("Name")[0]
        assertEquals(aspect.name, nameChange.after)
        assertEquals("", nameChange.before)
    }

    @Test
    fun testAspectHistoryCreateTwice() {
        /*  Проверяем, что создание двух разных аспектов адекватно отражается а истории */

        val aspect1 = aspectService.save(
            AspectData(
                name = "aspect-1",
                baseType = BaseType.Decimal.name,
                description = "some description-1"
            ), username
        )
        val aspect2 = aspectService.save(
            AspectData(
                name = "aspect-2",
                baseType = BaseType.Decimal.name,
                description = "some description-2"
            ), username
        )

        val aspectHistory: List<AspectHistory> = historyProvider.getAllHistory()

        assertEquals(2, aspectHistory.size, "History must contain 2 elements")

        val historyElement1 = aspectHistory[0]
        val historyElement2 = aspectHistory[1]

        assertGreater(historyElement1.event.timestamp, historyElement2.event.timestamp)
        aspectHistory.zip(listOf(aspect1, aspect2).reversed()).forEach {
            val historyElement = it.first
            val aspect = it.second

            assertEquals(
                ASPECT_CLASS,
                historyElement.event.entityClass,
                "entity class is incorrect for $historyElement"
            )
            assertEquals(aspect.id, historyElement.event.entityId, "enity id must correspond with id of added aspect")

            assertEquals(3, historyElement.changes.size, "history element: $historyElement")
            assertEquals(0, historyElement.fullData.related.size, "history element: $historyElement")
            val changedFields = historyElement.changes.groupBy { it.fieldName }
            assertEquals(setOf("Name", "Base type", "Description"), changedFields.keys)
            val nameChange = changedFields.getValue("Name")[0]
            assertEquals(aspect.name, nameChange.after)
        }
    }

    @Test
    fun testAspectHistoryCreateUpdate() {
        /*  Проверяем, что создание аспекта с последующим изменением адекватно отражается а истории */

        val subject1 =
            subjectService.createSubject(SubjectData(name = "subj1", description = "some description-1"), username)
        val aspect1 = aspectService.save(
            AspectData(
                name = "aspect-1",
                baseType = BaseType.Decimal.name,
                description = "some description-1"
            ), username
        )
        val aspect2 = aspectService.save(aspect1.copy(description = "other"), username = "admin")

        val aspectHistory: List<AspectHistory> = historyProvider.getAllHistory()

        assertEquals(2, aspectHistory.size, "History must contain 2 elements")

        val historyElement1 = aspectHistory[0]
        val historyElement2 = aspectHistory[1]

        assertGreater(historyElement1.event.timestamp, historyElement2.event.timestamp)
        assertGreater(historyElement1.event.version, historyElement2.event.version)
        assertNotEquals(historyElement1.event.sessionId, historyElement2.event.sessionId)

        assertEquals(1, historyElement1.changes.size)
        val delta = historyElement1.changes[0]
        assertEquals("Description", delta.fieldName)
        assertEquals(aspect1.description, delta.before)
        assertEquals(aspect2.description, delta.after)
    }

    @Test
    fun testAspectHistoryCreateAddProperty() {
        val aspect1 = aspectService.save(
            AspectData(
                name = "aspect-1",
                baseType = BaseType.Decimal.name,
                description = "some description-1"
            ), username
        )
        val aspect2 = aspectService.save(
            AspectData(
                name = "aspect-2",
                baseType = BaseType.Decimal.name,
                description = "some description-2"
            ), username
        )
        val aspect3 = aspectService.save(
            aspect1.copy(
                properties = listOf(
                    AspectPropertyData(
                        id = "", name = "prop", aspectId = aspect2.id ?: throw IllegalStateException("aspect2 id is null"),
                        cardinality = PropertyCardinality.INFINITY.name, description = null
                    )
                )
            ), username = "admin"
        )

        val all = historyService.getAll()
        val byClass = all.groupBy { it.event.entityClass }
        assertEquals(setOf(ASPECT_CLASS, ASPECT_PROPERTY_CLASS), byClass.keys, "facts must be of proper classes")

        val aspectFacts = byClass[ASPECT_CLASS] ?: throw IllegalStateException("no aspect facts")
        val propertyFacts = byClass[ASPECT_PROPERTY_CLASS] ?: throw IllegalStateException("no property facts")

        assertEquals(1, propertyFacts.size, "1 property fact must be present")
        assertEquals(3, aspectFacts.size, "3 aspect fact must be present")

        val byType: Map<EventType, List<HistoryFact>> = aspectFacts.groupBy { it.event.type }
        assertEquals(setOf(EventType.CREATE, EventType.UPDATE), byType.keys, "facts must be of proper types")

        val createFacts = byType[EventType.CREATE] ?: throw IllegalStateException("no create facts")
        val updateFacts = byType[EventType.UPDATE] ?: throw IllegalStateException("no update facts")

        assertEquals(1, updateFacts.size, "1 update fact must be present")
        assertEquals(2, createFacts.size, "2 create fact must be present")

        val updateFact = updateFacts[0]

        assertEquals(
            setOf("aspect-1", "aspect-2"),
            createFacts.map { it.payload.data[AspectField.NAME.name] }.toSet(),
            "names in create facts must be correct"
        )

        assertEquals(emptySet(), updateFact.payload.data.keys, "no data in update")
        assertEquals(emptySet(), updateFact.payload.removedLinks.keys, "no removed links in update")
        assertEquals(setOf(AspectField.PROPERTY), updateFact.payload.addedLinks.keys)

        val addedPropertyLinks = updateFact.payload.addedLinks[AspectField.PROPERTY]
                ?: throw IllegalStateException("no added property links")

        val propertyLink = addedPropertyLinks[0]

        assertEquals(aspect3.properties[0].id, propertyLink.toString())


        val propertyFact = propertyFacts[0]

        assertEquals(EventType.CREATE, propertyFact.event.type)
        assertEquals(aspect3.properties[0].id, propertyFact.event.entityId)
        assertEquals(updateFact.event.sessionId, propertyFact.event.sessionId, "session ids must match")

        assertEquals(
            setOf(AspectPropertyField.CARDINALITY.name, AspectPropertyField.NAME.name, AspectPropertyField.ASPECT.name),
            propertyFact.payload.data.keys
        )

        assertEquals(
            aspect3.properties[0].cardinality,
            propertyFact.payload.data[AspectPropertyField.CARDINALITY.name]
        )
        assertEquals(aspect3.properties[0].name, propertyFact.payload.data[AspectPropertyField.NAME.name])
        assertEquals(aspect3.properties[0].aspectId, propertyFact.payload.data[AspectPropertyField.ASPECT.name])

        assertEquals(emptySet(), propertyFact.payload.removedLinks.keys, "no removed links in property fact")
        assertEquals(emptySet(), propertyFact.payload.addedLinks.keys, "no added links in property fact")

        val aspectHistory: List<AspectHistory> = historyProvider.getAllHistory()
        val byTypeAspectHistory = aspectHistory.groupBy { it.event.type }

        assertEquals(
            setOf(EventType.CREATE, EventType.UPDATE),
            byTypeAspectHistory.keys,
            "aspect history facts must be of proper types"
        )

        val createAspectFacts = byTypeAspectHistory[EventType.CREATE] ?: throw IllegalStateException("no create facts")
        val updateAspectFacts = byTypeAspectHistory[EventType.UPDATE] ?: throw IllegalStateException("no update facts")

        assertEquals(1, updateAspectFacts.size, "1 update fact must be present")
        assertEquals(2, createAspectFacts.size, "2 create facts must be present")

        val updateAspectFact = updateAspectFacts[0]

        assertEquals(
            setOf("aspect-1", "aspect-2"),
            createAspectFacts.map { it.fullData.aspectData.name }.toSet(),
            "names in create facts must be correct"
        )

        assertEquals(1, updateAspectFact.changes.size, "no data in update")
        val change = updateAspectFact.changes[0]
        assertEquals("Property " + aspect3.properties[0].name, change.fieldName)
        assertEquals(null, change.before)
        assertEquals(true, change.after?.contains("prop"))
        assertEquals(true, change.after?.contains("aspect-2"))
    }

    @Test
    fun testAspectHistoryUpdateProperty() {
        val aspect1 = aspectService.save(
            AspectData(
                name = "aspect-1",
                baseType = BaseType.Decimal.name,
                description = "some description-1"
            ), username
        )
        val aspect2 = aspectService.save(
            AspectData(
                name = "aspect-2",
                baseType = BaseType.Decimal.name,
                description = "some description-2"
            ), username
        )
        val aspect3 = aspectService.save(
            aspect1.copy(
                properties = listOf(
                    AspectPropertyData(
                        id = "", name = "prop", aspectId = aspect2.id ?: throw IllegalStateException("aspect2 id is null"),
                        cardinality = PropertyCardinality.INFINITY.name, description = null
                    )
                )
            ), username = "admin"
        )

        val factsBefore = historyService.getAll()
        val aspectHistoryBefore: List<AspectHistory> = historyProvider.getAllHistory()

        val aspect4 = aspectService.save(
            aspect3.copy(properties = aspect3.properties.map { it.copy(description = "descr") }),
            "admin"
        )

        val factsAfter = historyService.getAll()
        val aspectHistoryAfter: List<AspectHistory> = historyProvider.getAllHistory()

        val factsAdded = factsAfter - factsBefore
        val aspectHistoryAdded = aspectHistoryAfter.dropLast(aspectHistoryBefore.size)

        assertEquals(2, factsAdded.size, "2 facts must be added")
        assertEquals(1, aspectHistoryAdded.size, "1 aspect history fact must be added")

        val byClass = factsAdded.groupBy { it.event.entityClass }

        assertEquals(setOf(ASPECT_CLASS, ASPECT_PROPERTY_CLASS), byClass.keys, "2 facts of two classes must be found")

        val aspectFacts = byClass[ASPECT_CLASS] ?: throw IllegalStateException("aspect facts")
        val propertyFacts = byClass[ASPECT_PROPERTY_CLASS] ?: throw IllegalStateException("no property facts")

        val aspectFact = aspectFacts[0]
        val propertyFact = propertyFacts[0]
        val aspectProviderFact = aspectHistoryAdded[0]

        assertEquals(aspect3.id, aspectFact.event.entityId)
        assertEquals(aspect3.properties[0].id, propertyFact.event.entityId)

        assertEquals(propertyFact.event.sessionId, aspectFact.event.sessionId)

        assertEquals(aspect3.id, aspectProviderFact.event.entityId)

        assertEquals(1, aspectProviderFact.changes.size)
        //val change = aspectProviderFact.changes[0]
        //assertEquals("Property " + aspect4.properties[0].name, change.fieldName)
    }

    @Test
    fun testAspectHistoryWithSubject() {
        val subject =
            subjectService.createSubject(SubjectData(name = "subject-1", description = "subject description"), username)
                .toSubjectData()
        val aspect1 = aspectService.save(
            AspectData(
                name = "aspect-1",
                baseType = BaseType.Decimal.name,
                description = "some description-1",
                subject = subject
            ), username
        )

        val subjectId = subject.id
        if (subjectId == null) {
            fail("id must be non-null")
        } else {
            val subjectVertex = subjectService.findById(subjectId)

            val history = historyProvider.getAllHistory()

            assertEquals(1, history.size)
            val fact = history.first()
            assertEquals(4, fact.changes.size)
        }
    }

    @Test
    fun testAspectHistoryWithRefBook() {
        val aspect1 = aspectService.save(
            AspectData(
                name = "aspect-1",
                baseType = BaseType.Text.name,
                description = "some description-1"
            ), username
        )
        val aspectId = aspect1.id ?: throw IllegalStateException("id of aspect is not defined")
        val refBook = refBookService.createReferenceBook("ref book name", aspectId, username)

        val history = historyProvider.getAllHistory()

        assertEquals(2, history.size)
        //val fact = history.first()
        //assertEquals(4, fact.changes.size)
    }

    @Test
    fun testAspectHistoryRemovedSubject() {
        val subject =
            subjectService.createSubject(SubjectData(name = "subject-1", description = "subject description"), username)
                .toSubjectData()
        val aspect1 = aspectService.save(
            AspectData(
                name = "aspect-1",
                baseType = BaseType.Decimal.name,
                description = "some description-1",
                subject = subject
            ), username
        )
        val aspect2 = aspectService.save(aspect1.copy(subject = null), username)

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
        val subject =
            subjectService.createSubject(SubjectData(name = "subject-1", description = "subject description"), username)
                .toSubjectData()
        val aspect1 = aspectService.save(
            AspectData(name = "aspect-1", baseType = BaseType.Decimal.name, description = "some description-1", subject = subject), username
        )
        val aspect2 = aspectService.save(aspect1.copy(subject = null), username)

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

    @Test
    fun testAspectHistoryCreateProperty() {
        val aspect1 = aspectService.save(
            AspectData(
                name = "aspect-1",
                baseType = BaseType.Text.name,
                description = "some description-1"
            ), username
        )
        val aspectId = aspect1.id ?: throw IllegalStateException("id of aspect is not defined")

        val property = AspectPropertyData("", "p", aspect1.idStrict(), PropertyCardinality.INFINITY.name, null)
        val complexAspectData = AspectData(
            "",
            "complex",
            Kilometre.name,
            null,
            BaseType.Decimal.name,
            listOf(property)
        )
        val complexAspect = aspectService.save(complexAspectData, username)

        val history = historyProvider.getAllHistory()

        assertEquals(2, history.size)

        val latestFact = history.first()

        println("latest fact: " + latestFact)
        println("complex aspect: " + complexAspect)

        assertEquals(EventType.CREATE, latestFact.event.type)
        assertEquals(complexAspect.name, latestFact.fullData.aspectData.name)
        assertEquals(complexAspect.baseType, latestFact.fullData.aspectData.baseType)

        //assertEquals(complexAspect.measure, latestFact.fullData.aspectData.measure)
    }
}