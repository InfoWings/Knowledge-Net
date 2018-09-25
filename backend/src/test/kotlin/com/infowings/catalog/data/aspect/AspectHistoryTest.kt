package com.infowings.catalog.data.aspect

import com.infowings.catalog.common.*
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.history.HistoryDao
import com.infowings.catalog.data.history.HistoryFact
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.data.history.providers.AspectHistoryProvider
import com.infowings.catalog.data.reference.book.ReferenceBookService
import com.infowings.catalog.data.subject.SubjectDao
import com.infowings.catalog.data.toSubjectData
import com.infowings.catalog.forAspect
import com.infowings.catalog.randomName
import com.infowings.catalog.storage.ASPECT_CLASS
import com.infowings.catalog.storage.ASPECT_PROPERTY_CLASS
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.transaction
import io.kotlintest.matchers.beGreaterThan
import io.kotlintest.matchers.beGreaterThanOrEqualTo
import io.kotlintest.should
import io.kotlintest.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.fail

@ExtendWith(SpringExtension::class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD, methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
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

    @Autowired
    private lateinit var db: OrientDatabase

    @BeforeEach
    fun initTestData() {
        transaction(db) { it.command("delete vertex from HistoryEvent") }
    }

    @Test
    fun testAspectHistoryEmpty() {
        val aspectHistory: List<AspectHistory> = historyProvider.getAllHistory()

        assertEquals(0, aspectHistory.size, "History must contain no elements")
    }

    @Test
    @Suppress("MagicNumber")
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

        aspectHistoryElement.event.timestamp should beGreaterThan(0L)

        assertEquals(4, aspectHistoryElement.changes.size)
        val changedFields = aspectHistoryElement.changes.groupBy { it.fieldName }
        val fields = setOf(AspectField.NAME, AspectField.BASE_TYPE, AspectField.DESCRIPTION, AspectField.GUID)
        assertEquals(fields.map { it.view }.toSet(), changedFields.keys)
        val nameChange = changedFields.getValue("Name")[0]
        assertEquals(aspect.name, nameChange.after)
        assertEquals("", nameChange.before)

        val timeline = historyDao.timelineForEntity(aspect.idStrict())
        assertEquals(1, timeline.size)
        val entityVertex = timeline[0]
        assertEquals(aspect.version, entityVertex.entityVersion)
        assertEquals(ASPECT_CLASS, entityVertex.entityClass)
        val fact = transaction(db) {
            entityVertex.toFact()
        }
        assertEquals(fields.map { it.name }.toSet(), fact.payload.data.keys)
        assertEquals("aspect", fact.payload.data[AspectField.NAME.name])
    }

    @Test
    @Suppress("MagicNumber")
    fun testAspectHistoryCreateTwice() {
        val before: List<AspectHistory> = historyProvider.getAllHistory()

        /*  Проверяем, что создание двух разных аспектов адекватно отражается а истории */

        val aspect1 = aspectService.save(
            AspectData(
                name = "aspect-1",
                baseType = BaseType.Decimal.name,
                description = "some description-1"
            ), username
        )

        // quick workaround to make sure event are ordered properly (because now they are ordered by timestamp)
        Thread.sleep(10)

        val aspect2 = aspectService.save(
            AspectData(
                name = "aspect-2",
                baseType = BaseType.Decimal.name,
                description = "some description-2"
            ), username
        )

        val aspectHistory: List<AspectHistory> = historyProvider.getAllHistory().drop(before.size)

        assertEquals(2, aspectHistory.size, "History must contain 2 elements")

        val historyElement1 = aspectHistory[0]
        val historyElement2 = aspectHistory[1]

        historyElement1.event.timestamp should beGreaterThanOrEqualTo(historyElement2.event.timestamp)
        aspectHistory.zip(listOf(aspect1, aspect2).reversed()).forEach {
            val historyElement = it.first
            val aspect = it.second

            assertEquals(
                ASPECT_CLASS,
                historyElement.event.entityClass,
                "entity class is incorrect for $historyElement"
            )
            assertEquals(aspect.id, historyElement.event.entityId, "enity id must correspond with id of added aspect")

            assertEquals(4, historyElement.changes.size, "history element: $historyElement")
            assertEquals(0, historyElement.fullData.related.size, "history element: $historyElement")
            val changedFields = historyElement.changes.groupBy { it.fieldName }
            assertEquals(setOf(AspectField.NAME, AspectField.BASE_TYPE, AspectField.DESCRIPTION, AspectField.GUID).map { it.view }.toSet(), changedFields.keys)
            val nameChange = changedFields.getValue("Name")[0]
            assertEquals(aspect.name, nameChange.after)
        }


        val timeline1 = historyDao.timelineForEntity(aspect1.idStrict())
        assertEquals(1, timeline1.size)
        val timeline2 = historyDao.timelineForEntity(aspect2.idStrict())
        assertEquals(1, timeline2.size)

        val entityVertex1 = timeline1[0]
        val entityVertex2 = timeline2[0]

        assertEquals(aspect1.version, entityVertex1.entityVersion)
        assertEquals(aspect2.version, entityVertex2.entityVersion)
        assertEquals(ASPECT_CLASS, entityVertex1.entityClass)
        assertEquals(ASPECT_CLASS, entityVertex2.entityClass)
    }

    @Test
    fun testAspectHistoryCreateUpdate() {
        /*  Проверяем, что создание аспекта с последующим изменением адекватно отражается а истории */

        val subject1 =
            subjectService.createSubject(SubjectData(name = "subj1", description = "some description-1"), username)
        val aspect1 = aspectService.save(
            AspectData(
                name = randomName(),
                baseType = BaseType.Decimal.name,
                description = "some description-1"
            ), username
        )
        val aspect2 = aspectService.save(aspect1.copy(description = "other"), username = "admin")

        val aspectHistory: List<AspectHistory> = historyProvider.getAllHistory()

        assertEquals(2, aspectHistory.size, "History must contain 2 elements")

        val historyElement1 = aspectHistory[0]
        val historyElement2 = aspectHistory[1]

        historyElement1.event.timestamp should beGreaterThanOrEqualTo(historyElement2.event.timestamp)
        historyElement1.event.version should beGreaterThanOrEqualTo(historyElement2.event.version)
        assertNotEquals(historyElement1.event.sessionId, historyElement2.event.sessionId)

        assertEquals(1, historyElement1.changes.size)
        val delta = historyElement1.changes[0]
        assertEquals("Description", delta.fieldName)
        assertEquals(aspect1.description, delta.before)
        assertEquals(aspect2.description, delta.after)

        val timeline = historyDao.timelineForEntity(aspect1.idStrict())
        assertEquals(2, timeline.size)

        val entityVertex1 = timeline[0]
        val entityVertex2 = timeline[1]

        assertEquals(aspect1.version, entityVertex1.entityVersion)
        assertEquals(aspect2.version, entityVertex2.entityVersion)
        assertEquals(ASPECT_CLASS, entityVertex1.entityClass)
        assertEquals(ASPECT_CLASS, entityVertex2.entityClass)
    }

    @Test
    fun testAspectHistoryCreateAddProperty() {
        val aspect1 = aspectService.save(
            AspectData(
                name = randomName(),
                baseType = BaseType.Decimal.name,
                description = "some description-1"
            ), username
        )
        val aspect2 = aspectService.save(
            AspectData(
                name = randomName(),
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
        val createdProperty = aspect3.properties[0]

        val all = historyService.allTimeline()
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
            setOf(aspect1.name, aspect2.name),
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
        assertEquals(createdProperty.id, propertyFact.event.entityId)
        assertEquals(updateFact.event.sessionId, propertyFact.event.sessionId, "session ids must match")

        assertEquals(
            setOf(AspectPropertyField.CARDINALITY.name, AspectPropertyField.NAME.name, AspectPropertyField.ASPECT.name, AspectPropertyField.GUID.name),
            propertyFact.payload.data.keys
        )

        assertEquals(
            createdProperty.cardinality,
            propertyFact.payload.data[AspectPropertyField.CARDINALITY.name]
        )
        assertEquals(createdProperty.name, propertyFact.payload.data[AspectPropertyField.NAME.name])
        assertEquals(createdProperty.aspectId, propertyFact.payload.data[AspectPropertyField.ASPECT.name])

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
            setOf(aspect1.name, aspect2.name),
            createAspectFacts.map { it.fullData.aspectData.name }.toSet(),
            "names in create facts must be correct"
        )

        assertEquals(1, updateAspectFact.changes.size, "no data in update")
        val change = updateAspectFact.changes[0]
        assertEquals("Property " + aspect3.properties[0].name, change.fieldName)
        assertEquals(null, change.before)
        assertEquals(true, change.after?.contains("prop"))
        assertEquals(true, change.after?.contains(aspect2.name))

        val timelineParent = historyDao.timelineForEntity(aspect3.idStrict())
        assertEquals(2, timelineParent.size)

        val timelineChild = historyDao.timelineForEntity(aspect2.idStrict())
        assertEquals(1, timelineChild.size)

        val timelineProp = historyDao.timelineForEntity(createdProperty.id)
        assertEquals(1, timelineProp.size)
    }

    @Test
    fun testAspectHistoryUpdateProperty() {
        val aspect1 = aspectService.save(
            AspectData(
                name = randomName(),
                baseType = BaseType.Decimal.name,
                description = "some description-1"
            ), username
        )
        val aspect2 = aspectService.save(
            AspectData(
                name = randomName(),
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

        val factsBefore = historyService.allTimeline()
        val aspectHistoryBefore: List<AspectHistory> = historyProvider.getAllHistory()

        val aspect4 = aspectService.save(
            aspect3.copy(properties = aspect3.properties.map { it.copy(description = "descr") }),
            "admin"
        )

        val factsAfter = historyService.allTimeline()
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
        val change = aspectProviderFact.changes[0]
    }

    @Test
    @Suppress("MagicNumber")
    fun testAspectHistoryWithSubject() {
        val subject = subjectService
            .createSubject(SubjectData(name = "subject-1", description = "subject description"), username)
            .toSubjectData()

        //check if properly created
        subject.id?.let { subjectService.findById(it) } ?: fail("id must be non-null")

        val aspect = randomAspect(subject)
        aspectService.save(aspect, username)

        val history = historyProvider.getAllHistory().forAspect(aspect)

        history.size shouldBe 1

        val fact = history.first { it.fullData.aspectData.name == aspect.name }
        fact.changes.size shouldBe 5
    }

    @Test
    fun testAspectHistoryWithRefBook() {
        val aspect = aspectService.save(randomAspect(null, baseType = BaseType.Text), username)
        val aspectId = aspect.id ?: throw IllegalStateException("id of aspect is not defined")
        refBookService.createReferenceBook(randomName(), aspectId, username)

        val history = historyProvider.getAllHistory().forAspect(aspect)

        history.size shouldBe 2
    }

    @Test
    fun `Aspect history should be saved after removing subject`() {
        val subject = subjectService
            .createSubject(SubjectData(name = "subject-1", description = "subject description"), username)
            .toSubjectData()

        //check if properly created
        subject.id?.let { subjectService.findById(it) } ?: fail("id must be non-null")

        val aspect = aspectService.save(randomAspect(subject), username)
        aspectService.save(aspect.copy(subject = null), username)

        subjectService.remove(subject, username, force = false)

        val history = historyProvider.getAllHistory().forAspect(aspect)

        history.size shouldBe 2
    }

    fun randomAspect(subject: SubjectData?, baseType: BaseType = BaseType.Decimal): AspectData {
        return AspectData(
            name = randomName(),
            baseType = baseType.name,
            description = "some description-1",
            subject = subject
        )
    }

    @Test
    fun testAspectHistoryRemovedSubject2() {
        val subject =
            subjectService.createSubject(SubjectData(name = randomName(), description = "subject description"), username)
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
        val before = historyProvider.getAllHistory()

        val aspect1 = aspectService.save(
            AspectData(
                name = "testAspectHistoryCreateProperty-aspect-1",
                baseType = BaseType.Text.name,
                description = "some description-1"
            ), username
        )
        val aspectId = aspect1.id ?: throw IllegalStateException("id of aspect is not defined")

        val property = AspectPropertyData("", "p", aspect1.idStrict(), PropertyCardinality.INFINITY.name, null)
        val complexAspectData = AspectData(
            "",
            "testAspectHistoryCreateProperty-complex",
            Kilometre.name,
            null,
            BaseType.Decimal.name,
            listOf(property)
        )
        val complexAspect = aspectService.save(complexAspectData, username)

        val history = historyProvider.getAllHistory().drop(before.size)

        assertEquals(2, history.size)

        val latestFact = history.find { it.event.entityId == complexAspect.id } ?: throw IllegalStateException("Not found aspect")

        assertEquals(EventType.CREATE, latestFact.event.type)
        assertEquals(complexAspect.name, latestFact.fullData.aspectData.name)
        assertEquals(complexAspect.baseType, latestFact.fullData.aspectData.baseType)
    }

    @Test
    fun testAspectWithSubAspects() {
        val before = historyProvider.getAllHistory()

        val aspect1 = aspectService.save(
            AspectData(
                name = randomName(),
                baseType = BaseType.Text.name,
                description = "some description-1"
            ), username
        )
        val aspectId = aspect1.id ?: throw IllegalStateException("id of aspect is not defined")

        val property = AspectPropertyData("", "p", aspect1.idStrict(), PropertyCardinality.INFINITY.name, null)
        val complexAspectData = AspectData(
            "",
            randomName(),
            Kilometre.name,
            null,
            BaseType.Decimal.name,
            listOf(property)
        )
        val complexAspect = aspectService.save(complexAspectData, username)

        val property2 = AspectPropertyData("", "p", complexAspect.idStrict(), PropertyCardinality.INFINITY.name, null)
        val complexAspectData2 = AspectData(
            "",
            randomName(),
            Kilometre.name,
            null,
            BaseType.Decimal.name,
            listOf(property2)
        )
        val complexAspect2 = aspectService.save(complexAspectData2, username)

        val history = historyProvider.getAllHistory().drop(before.size)

        assertEquals(3, history.size)

        val latestFact = history.find { it.event.entityId == complexAspect2.id } ?: throw IllegalStateException("Not found aspect")

        /*
        assertEquals(EventType.CREATE, latestFact.event.type)
        assertEquals(complexAspect.name, latestFact.fullData.aspectData.name)
        assertEquals(complexAspect.baseType, latestFact.fullData.aspectData.baseType)
        */
    }
}