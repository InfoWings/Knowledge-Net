package com.infowings.catalog.data.reference.book

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.*
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.history.HistoryFact
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.data.history.providers.HISTORY_ENTITY_REFBOOK
import com.infowings.catalog.data.history.providers.RefBookHistoryProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import java.lang.Long.max
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(SpringJUnit4ClassRunner::class)
@SpringBootTest(classes = [MasterCatalog::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RefBookHistoryTest {
    private val username = "admin"

    @Autowired
    lateinit var subjectService: SubjectService

    @Autowired
    private lateinit var aspectService: AspectService

    @Autowired
    private lateinit var refBookService: ReferenceBookService

    @Autowired
    private lateinit var historyService: HistoryService

    @Autowired
    private lateinit var historyProvider: RefBookHistoryProvider

    private lateinit var aspect: AspectData

    private lateinit var userName: String

    @Before
    fun initTestData() {
        val aspectData = AspectData("", "newAspect", null, null, BaseType.Text.name, emptyList())
        aspect = aspectService.save(aspectData, username)
        userName = "admin"
    }

    @Test
    fun testRefBookCreateHistory() {
        val testName = "testRefBookCreateHistory"

        val historyBefore = historyService.getAll()
        val statesBefore = historyProvider.getAllHistory()

        val refBook = refBookService.createReferenceBook(name = testName, aspectId = aspect.idStrict(), username = userName)

        val historyAfter = historyService.getAll()
        val statesAfter = historyProvider.getAllHistory()

        val states: List<RefBookHistory> = statesAfter.dropLast(statesBefore.size)

        val facts = historyAfter - historyBefore
        val refBookFacts = facts.refBookFacts()

        // проверяем метаданные о факте
        assertEquals(1, refBookFacts.size, "History must contain 1 element about ref book")
        val refBookEvent = refBookFacts.first().event
        assertEquals(EventType.CREATE, refBookEvent.type)
        assertEquals(refBook.id, refBookEvent.entityId)

        // проверяем содержательную часть факта
        // сначала - ключи data/addedLinks/removedLinks
        val refBookPayload = refBookFacts.first().payload
        assertEquals(setOf("value"), refBookPayload.data.keys, "keys must be correct")
        assertEquals(setOf("aspect"), refBookPayload.addedLinks.keys, "added links must be correct")
        assertEquals(emptySet(), refBookPayload.removedLinks.keys, "removed links must be correct")

        // потом - значения
        assertEquals(testName, refBookPayload.data["value"])
        val aspectLinks = refBookPayload.addedLinks.getValue("aspect")
        assertEquals(listOf(aspect.id), aspectLinks.map { it.toString() })

        // теперь проверяем историю на уровне справочников

        // добавляется одно состояние
        assertEquals(1, states.size, "History must contain 1 element about ref book")
        val state = states[0]

        // проверяем мета-данные о состоянии
        assertEquals(userName, state.event.username)
        assertEquals(refBookEvent.timestamp, state.event.timestamp)
        assertEquals(EventType.CREATE, state.event.type)
        assertEquals(HISTORY_ENTITY_REFBOOK, state.event.entityClass)
        assertEquals(false, state.deleted)
        assertEquals(refBookEvent.version, state.event.version)
        assertEquals(refBook.name, state.info)

        // проверяем заголовочную часть
        assertEquals(refBook.id, state.fullData.header.id)
        assertEquals(refBook.name, state.fullData.header.name)
        assertEquals(refBook.description, state.fullData.header.description)
        assertEquals(refBook.aspectId, state.fullData.header.aspectId)
        assertEquals(aspect.name, state.fullData.header.aspectName)

        // item отсутствует
        assertEquals(null, state.fullData.item)

        // проверяем changes
        assertEquals(2, state.changes.size)
        val byField = state.changes.groupBy { it.fieldName }
        assertEquals(setOf("Name", "Aspect"), byField.keys)
        assertEquals(testName, byField.getValue("Name")[0].after)
//        assertEquals(aspect.id, byField.getValue("link_aspect")[0].after)
        assertEquals(aspect.name, byField.getValue("Aspect")[0].after)
        assertEquals(listOf("", ""), state.changes.map { it.before })
    }

    @Test
    fun testRefBookItemCreateHistory() {
        val testName = "testRefBookItemCreateHistory"

        val refBook = refBookService.createReferenceBook(name = testName, aspectId = aspect.idStrict(), username = "admin")

        val itemValue = "rbi-1"
        val itemDescription = "rbi-1 description"

        val historyBefore = historyService.getAll()
        val statesBefore = historyProvider.getAllHistory()

        val itemId = refBookService.addReferenceBookItem(
            ItemCreateRequest(parentId = refBook.id, value = itemValue, description = itemDescription), "admin"
        )

        val historyAfter = historyService.getAll()
        val statesAfter = historyProvider.getAllHistory()

        val facts = historyAfter - historyBefore
        val refBookFacts = facts.refBookFacts()

        val states = statesAfter.dropLast(statesBefore.size)

        // должно быть два элементарных факта
        assertEquals(2, refBookFacts.size, "History must contain 2 elements about ref book")

        val byType = refBookFacts.groupBy { it.event.type }

        // создание элемента и обновление корня (он же родитель в данном случае)
        assertEquals(setOf(EventType.CREATE, EventType.UPDATE), byType.keys)

        // извлекаем факты и проверяем id сущности для каждого
        val updateFact = byType.getValue(EventType.UPDATE)[0]
        val createFact = byType.getValue(EventType.CREATE)[0]
        val updateEvent = updateFact.event
        assertEquals(refBook.id, updateEvent.entityId)
        val createEvent = createFact.event
        assertEquals(itemId, createEvent.entityId)

        // проверяем содержание факта обновления родителя
        val updatePayload = updateFact.payload
        assertEquals(emptySet(), updatePayload.data.keys, "keys must be correct")
        assertEquals(setOf("children"), updatePayload.addedLinks.keys, "added links must be correct")
        assertEquals(emptySet(), updatePayload.removedLinks.keys, "removed links must be correct")
        val childrenLinks = updatePayload.addedLinks.getValue("children")
        assertEquals(listOf(itemId), childrenLinks.map { it.toString() })

        // проверяем содержание факта создания элемента
        val createPayload = createFact.payload
        assertEquals(setOf("value", "description"), createPayload.data.keys, "keys must be correct")
        assertEquals(setOf("parent", "root"), createPayload.addedLinks.keys, "added links must be correct")
        assertEquals(emptySet(), createPayload.removedLinks.keys, "removed links must be correct")

        assertEquals(itemValue, createPayload.data["value"])
        assertEquals(itemDescription, createPayload.data["description"])

        val parentLinks = createPayload.addedLinks.getValue("parent")
        assertEquals(listOf(refBook.id), parentLinks.map { it.toString() })

        val rootLinks = createPayload.addedLinks.getValue("root")
        assertEquals(listOf(refBook.id), rootLinks.map { it.toString() })

        // теперь проверяем историю в терминах справочников

        // ровно одно новое состояние
        assertEquals(1, states.size, "History must contain 1 element about ref book")
        val state = states[0]

        // проверяем мета-данные
        assertEquals(userName, state.event.username)
        assertEquals(max(updateEvent.timestamp, createEvent.timestamp), state.event.timestamp)
        assertEquals(EventType.UPDATE, state.event.type)
        assertEquals(HISTORY_ENTITY_REFBOOK, state.event.entityClass)
        assertEquals(false, state.deleted)
        assertEquals(refBook.name, state.info)

        // проверяем заголовок
        assertEquals(refBook.id, state.fullData.header.id)
        assertEquals(refBook.name, state.fullData.header.name)
        assertEquals(refBook.description, state.fullData.header.description)
        assertEquals(aspect.id, state.fullData.header.aspectId)
        assertEquals(aspect.name, state.fullData.header.aspectName)

        // проверяем элемент
        assertNotNull(state.fullData.item)
        val item = state.fullData.item ?: throw IllegalStateException("item is null")
        assertEquals(itemId, item.id)
        assertEquals(itemValue, item.name)
        assertEquals(itemDescription, item.description)

        // проверяем изменения
        assertEquals(2, state.changes.size)
        val byField = state.changes.groupBy { it.fieldName }
        assertEquals(setOf("Name", "description"), byField.keys)
        assertEquals(itemValue, byField.getValue("Name")[0].after)
        assertEquals(itemDescription, byField.getValue("description")[0].after)
        assertEquals(listOf("", ""), state.changes.map { it.before })
    }

    @Test
    fun testRefBookSecondItemCreateHistory() {
        val testName = "testRefBookSecondItemCreateHistory"

        val refBook = refBookService.createReferenceBook(name = testName, aspectId = aspect.idStrict(), username = "admin")

        val item1 = refBookService.addReferenceBookItem(
            ItemCreateRequest(parentId = refBook.id, value = "rbi-1", description = "rbi-1 description"), "admin"
        )

        val itemValue2 = "rbi-2"
        val itemDescription2 = "rbi2 description"

        val historyBefore = historyService.getAll()
        val statesBefore = historyProvider.getAllHistory()
        val item2 = refBookService.addReferenceBookItem(
            ItemCreateRequest(parentId = refBook.id, value = itemValue2, description = itemDescription2), "admin"
        )

        val historyAfter = historyService.getAll()
        val statesAfter = historyProvider.getAllHistory()

        val facts = historyAfter - historyBefore
        val refBookFacts = facts.refBookFacts()

        val states = statesAfter.dropLast(statesBefore.size)

        // должно быть два элементарных факта
        assertEquals(2, refBookFacts.size, "History must contain 2 elements about ref book")

        // создание элемента и обновление корня (он же родитель в данном случае)
        val byType = refBookFacts.groupBy { it.event.type }
        assertEquals(setOf(EventType.CREATE, EventType.UPDATE), byType.keys)

        // извлекаем факты и проверяем id сущности для каждого
        val updateFact = byType.getValue(EventType.UPDATE)[0]
        val createFact = byType.getValue(EventType.CREATE)[0]
        val updateEvent = updateFact.event
        assertEquals(refBook.id, updateEvent.entityId)
        val createEvent = createFact.event
        assertEquals(item2, createEvent.entityId)

        // проверяем содержание факта обновления элемента
        val updatePayload = updateFact.payload
        assertEquals(emptySet(), updatePayload.data.keys, "keys must be correct")
        assertEquals(setOf("children"), updatePayload.addedLinks.keys, "added links must be correct")
        assertEquals(emptySet(), updatePayload.removedLinks.keys, "removed links must be correct")

        val childrenLinks = updatePayload.addedLinks.getValue("children")
        assertEquals(listOf(item2), childrenLinks.map { it.toString() })

        // проверяем содержание факта создания элемента
        val createPayload = createFact.payload
        assertEquals(setOf("value", "description"), createPayload.data.keys, "keys must be correct")
        assertEquals(setOf("parent", "root"), createPayload.addedLinks.keys, "added links must be correct")
        assertEquals(emptySet(), createPayload.removedLinks.keys, "removed links must be correct")

        assertEquals(itemValue2, createPayload.data["value"])
        assertEquals(itemDescription2, createPayload.data["description"])

        val parentLinks = createPayload.addedLinks.getValue("parent")
        assertEquals(listOf(refBook.id), parentLinks.map { it.toString() })

        val rootLinks = createPayload.addedLinks.getValue("root")
        assertEquals(listOf(refBook.id), rootLinks.map { it.toString() })

        // проверяем историю в терминах справочников

        // ровно одно новое состояние
        assertEquals(1, states.size, "History must contain 1 element about ref book")

        val state = states[0]

        // проверяем мета-данные
        assertEquals(userName, state.event.username)
        assertEquals(max(updateEvent.timestamp, createEvent.timestamp), state.event.timestamp)
        assertEquals(EventType.UPDATE, state.event.type)
        assertEquals(HISTORY_ENTITY_REFBOOK, state.event.entityClass)
        assertEquals(false, state.deleted)
        assertEquals(refBook.name, state.info)

        // проверяем заголовок
        assertEquals(refBook.id, state.fullData.header.id)
        assertEquals(refBook.name, state.fullData.header.name)
        assertEquals(refBook.description, state.fullData.header.description)
        assertEquals(aspect.id, state.fullData.header.aspectId)

        // проверяем элемент
        assertNotNull(state.fullData.item)
        val item = state.fullData.item ?: throw IllegalStateException("item is null")
        assertEquals(item2, item.id)
        assertEquals(itemValue2, item.name)
        assertEquals(itemDescription2, item.description)

        // проверяем изменения
        assertEquals(2, state.changes.size)
        val byField = state.changes.groupBy { it.fieldName }
        assertEquals(setOf("Name", "description"), byField.keys)
        assertEquals(itemValue2, byField.getValue("Name")[0].after)
        assertEquals(itemDescription2, byField.getValue("description")[0].after)
        assertEquals(listOf("", ""), state.changes.map { it.before })
    }

    @Test
    fun testRefBookChildItemCreateHistory() {
        val testName = "testRefBookChildItemCreateHistory"

        val refBook = refBookService.createReferenceBook(name = testName, aspectId = aspect.idStrict(), username = "admin")

        val itemId1 = refBookService.addReferenceBookItem(
            ItemCreateRequest(parentId = refBook.id, value = "rbi-1", description = "rbi-1 description"), "admin"
        )

        val itemValue2 = "rbi-2"
        val itemDescription2 = "rbi2 description"

        val historyBefore = historyService.getAll()
        val statesBefore = historyProvider.getAllHistory()
        val itemId2 = refBookService.addReferenceBookItem(
            ItemCreateRequest(parentId = itemId1, value = itemValue2, description = itemDescription2), "admin"
        )

        val historyAfter = historyService.getAll()
        val statesAfter = historyProvider.getAllHistory()

        val facts = historyAfter - historyBefore
        val refBookFacts = facts.refBookFacts()

        // должно быть два элементарных факта
        assertEquals(2, refBookFacts.size, "History must contain 2 elements about ref book")

        // создание элемента и обновление родителя
        val byType = refBookFacts.groupBy { it.event.type }
        assertEquals(setOf(EventType.CREATE, EventType.UPDATE), byType.keys)

        // извлекаем факты и проверяем id сущности для каждого
        val updateFact = byType.getValue(EventType.UPDATE)[0]
        val createFact = byType.getValue(EventType.CREATE)[0]
        val updateEvent = updateFact.event
        assertEquals(itemId1, updateEvent.entityId)
        val createEvent = createFact.event
        assertEquals(itemId2, createEvent.entityId)

        // проверяем содержание факта обновления элемента
        val updatePayload = updateFact.payload
        assertEquals(emptySet(), updatePayload.data.keys, "keys must be correct")
        assertEquals(setOf("children"), updatePayload.addedLinks.keys, "added links must be correct")
        assertEquals(emptySet(), updatePayload.removedLinks.keys, "removed links must be correct")

        val childrenLinks = updatePayload.addedLinks.getValue("children")
        assertEquals(listOf(itemId2), childrenLinks.map { it.toString() })

        // проверяем содержание факта создания элемента
        val createPayload = createFact.payload
        assertEquals(setOf("value", "description"), createPayload.data.keys, "keys must be correct")
        assertEquals(setOf("parent", "root"), createPayload.addedLinks.keys, "added links must be correct")
        assertEquals(emptySet(), createPayload.removedLinks.keys, "removed links must be correct")

        assertEquals(itemValue2, createPayload.data["value"])
        assertEquals(itemDescription2, createPayload.data["description"])

        val parentLinks = createPayload.addedLinks.getValue("parent")
        assertEquals(listOf(itemId1), parentLinks.map { it.toString() })

        val rootLinks = createPayload.addedLinks.getValue("root")
        assertEquals(listOf(refBook.id), rootLinks.map { it.toString() })

        // проверяем историю в терминах справочников

        // ровно одно новое состояние
        val states = statesAfter.dropLast(statesBefore.size)
        assertEquals(1, states.size, "History must contain 1 element about ref book")

        val state = states[0]

        // проверяем мета-данные
        assertEquals(userName, state.event.username)
        assertEquals(max(updateEvent.timestamp, createEvent.timestamp), state.event.timestamp)
        assertEquals(EventType.UPDATE, state.event.type)
        assertEquals(HISTORY_ENTITY_REFBOOK, state.event.entityClass)
        assertEquals(false, state.deleted)
        assertEquals(refBook.name, state.info)

        // проверяем заголовок
        assertEquals(refBook.id, state.fullData.header.id)
        assertEquals(refBook.name, state.fullData.header.name)
        assertEquals(refBook.description, state.fullData.header.description)
        assertEquals(aspect.id, state.fullData.header.aspectId)

        // проверяем элемент
        assertNotNull(state.fullData.item)
        val item = state.fullData.item ?: throw IllegalStateException("item is null")
        assertEquals(itemId2, item.id)
        assertEquals(itemValue2, item.name)
        assertEquals(itemDescription2, item.description)

        // проверяем изменения
        assertEquals(2, state.changes.size)
        val byField = state.changes.groupBy { it.fieldName }
        assertEquals(setOf("Name", "description"), byField.keys)
        assertEquals(itemValue2, byField.getValue("Name")[0].after)
        assertEquals(itemDescription2, byField.getValue("description")[0].after)
        assertEquals(listOf("", ""), state.changes.map { it.before })
    }

    @Test
    fun testRefBookItemUpdateHistory() {
        val testName = "testRefBookItemUpdateHistory"

        val refBook = refBookService.createReferenceBook(name = testName, aspectId = aspect.idStrict(), username = "admin")

        val itemValue1 = "rbi-1"
        val itemDescription1 = "rbi-1 description"

        val itemValue2 = "rbi-2"
        val itemDescription2 = "rbi-2 description"

        val itemId = refBookService.addReferenceBookItem(
            ItemCreateRequest(parentId = refBook.id, value = itemValue1, description = itemDescription1), "admin"
        )

        val historyBefore = historyService.getAll()
        val statesBefore = historyProvider.getAllHistory()

        val itemId2 = refBookService.editReferenceBookItem(
            LeafEditRequest(id = itemId, value = itemValue2, description = itemDescription2, version = 1), "admin"
        )

        val historyAfter = historyService.getAll()
        val statesAfter = historyProvider.getAllHistory()

        val facts = historyAfter - historyBefore
        val refBookFacts = facts.refBookFacts()

        val states = statesAfter.dropLast(statesBefore.size)


        // должен быть один элементарный факт
        assertEquals(1, refBookFacts.size)

        // извлекаем факт и проверяем id сущности
        val fact = refBookFacts[0]
        val event = fact.event
        assertEquals(itemId, event.entityId)

        // проверяем содержание факта
        val payload = fact.payload
        assertEquals(setOf("value", "description"), payload.data.keys, "keys must be correct")
        assertEquals(emptySet(), payload.addedLinks.keys, "added links must be correct")
        assertEquals(emptySet(), payload.removedLinks.keys, "removed links must be correct")

        assertEquals(itemValue2, payload.data["value"])
        assertEquals(itemDescription2, payload.data["description"])

        // теперь проверяем историю в терминах справочников

        // ровно одно новое состояние
        assertEquals(1, states.size, "History must contain 1 element about ref book")
        val state = states[0]


        // проверяем мета-данные
        assertEquals(userName, state.event.username)
        assertEquals(event.timestamp, state.event.timestamp)
        assertEquals(EventType.UPDATE, state.event.type)
        assertEquals(HISTORY_ENTITY_REFBOOK, state.event.entityClass)
        assertEquals(false, state.deleted)
        assertEquals(refBook.name, state.info)

        // проверяем заголовок
        assertEquals(refBook.id, state.fullData.header.id)
        assertEquals(refBook.name, state.fullData.header.name)
        assertEquals(refBook.description, state.fullData.header.description)
        assertEquals(aspect.id, state.fullData.header.aspectId)
        assertEquals(aspect.name, state.fullData.header.aspectName)


        // проверяем элемент
        assertNotNull(state.fullData.item)
        val item = state.fullData.item ?: throw IllegalStateException("item is null")
        assertEquals(itemId, item.id)
        assertEquals(itemValue2, item.name)
        assertEquals(itemDescription2, item.description)

        // проверяем изменения
        assertEquals(2, state.changes.size)
        val byField = state.changes.groupBy { it.fieldName }
        assertEquals(setOf("Name", "description"), byField.keys)
        assertEquals(itemValue2, byField.getValue("Name")[0].after)
        assertEquals(itemDescription2, byField.getValue("description")[0].after)
        assertEquals(itemValue1, byField.getValue("Name")[0].before)
        assertEquals(itemDescription1, byField.getValue("description")[0].before)

    }

    @Test
    fun testRefBookUpdateHistory() {
        val testName = "testRefBookIUpdateHistory"

        val refBook = refBookService.createReferenceBook(name = testName, aspectId = aspect.idStrict(), username = "admin")

        val rbName2 = "SomeName"
        val rbDescription2 = "Some description"

        val historyBefore = historyService.getAll()
        val statesBefore = historyProvider.getAllHistory()

        val itemId2 = refBookService.editRoot(
            RootEditRequest(aspectId = aspect.idStrict(), value = rbName2, description = rbDescription2, version = 1), "admin"
        )

        val historyAfter = historyService.getAll()
        val statesAfter = historyProvider.getAllHistory()

        val facts = historyAfter - historyBefore
        val refBookFacts = facts.refBookFacts()

        val states = statesAfter.dropLast(statesBefore.size)

        println(refBookFacts)

        // должен быть один элементарный факт
        assertEquals(1, refBookFacts.size)

        // извлекаем факт и проверяем id сущности
        val fact = refBookFacts[0]
        val event = fact.event
        assertEquals(refBook.id, event.entityId)

        // проверяем содержание факта
        val payload = fact.payload
        assertEquals(setOf("value", "description"), payload.data.keys, "keys must be correct")
        assertEquals(emptySet(), payload.addedLinks.keys, "added links must be correct")
        assertEquals(emptySet(), payload.removedLinks.keys, "removed links must be correct")

        assertEquals(rbName2, payload.data["value"])
        assertEquals(rbDescription2, payload.data["description"])

        // теперь проверяем историю в терминах справочников

        // ровно одно новое состояние
        assertEquals(1, states.size, "History must contain 1 element about ref book")
        val state = states[0]


        // проверяем мета-данные
        assertEquals(userName, state.event.username)
        assertEquals(event.timestamp, state.event.timestamp)
        assertEquals(EventType.UPDATE, state.event.type)
        assertEquals(HISTORY_ENTITY_REFBOOK, state.event.entityClass)
        assertEquals(false, state.deleted)
        assertEquals(rbName2, state.info)

        // проверяем заголовок
        assertEquals(refBook.id, state.fullData.header.id)
        assertEquals(rbName2, state.fullData.header.name)
        assertEquals(rbDescription2, state.fullData.header.description)
        assertEquals(aspect.id, state.fullData.header.aspectId)
        assertEquals(aspect.name, state.fullData.header.aspectName)

        // проверяем элемент
        assertEquals(null, state.fullData.item)

        // проверяем изменения
        assertEquals(2, state.changes.size)
        val byField = state.changes.groupBy { it.fieldName }
        assertEquals(setOf("Name", "description"), byField.keys)
        assertEquals(rbName2, byField.getValue("Name")[0].after)
        assertEquals(rbDescription2, byField.getValue("description")[0].after)
        assertEquals(testName, byField.getValue("Name")[0].before)
        assertEquals(null, byField.getValue("description")[0].before)
    }

    private fun Set<HistoryFact>.factsByEntity(entity: String) = this.filter { it.event.entityClass == entity }

    private fun Set<HistoryFact>.refBookFacts() = factsByEntity(REFERENCE_BOOK_ITEM_VERTEX)
}