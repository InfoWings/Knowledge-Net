package com.infowings.catalog.data.objekt

import com.infowings.catalog.MasterCatalog
import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.ObjectCreateRequest
import com.infowings.catalog.common.objekt.PropertyCreateRequest
import com.infowings.catalog.common.objekt.ValueCreateRequest
import com.infowings.catalog.data.MeasureService
import com.infowings.catalog.data.Subject
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.history.HistoryFact
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.data.history.asString
import com.infowings.catalog.data.history.providers.HISTORY_ENTITY_OBJECT
import com.infowings.catalog.data.history.providers.ObjectHistoryProvider
import com.infowings.catalog.data.reference.book.ReferenceBookService
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.id.ORID
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import java.lang.Long
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail

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
    private lateinit var refBookService: ReferenceBookService
    @Autowired
    private lateinit var historyService: HistoryService
    @Autowired
    private lateinit var historyProvider: ObjectHistoryProvider

    private lateinit var subject: Subject

    private lateinit var aspect: AspectData

    private lateinit var complexAspect: AspectData

    private val username = "admin"

    @Before
    fun initTestData() {
        subject = subjectService.createSubject(SubjectData(name = "subjectName", description = "descr"), username)
        aspect = aspectService.save(
            AspectData(name = "aspectName", description = "aspectDescr", baseType = BaseType.Text.name), username
        )
        val property = AspectPropertyData("", "p", aspect.idStrict(), PropertyCardinality.INFINITY.name, null)
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

        val eventsBefore: Set<HistoryFact> = historyService.getAll().toSet()
        val objectEventsBefore = objectEvents(eventsBefore)
        val subjectEventsBefore = subjectEvents(eventsBefore)

        val factsBefore: Set<HistoryFact> = historyService.getAll().toSet()
        val objectFactsBefore = objectEvents(factsBefore)
        val subjectFactsBefore = subjectEvents(factsBefore)
        val statesBefore = historyProvider.getAllHistory()

        val objectName = testName
        val objectDescription = "object description"

        val request = ObjectCreateRequest(objectName, objectDescription, subject.id, subject.version)
        val created = objectService.create(request, username)

        val factsAfter = historyService.getAll().toSet()
        val objectFactsAfter = objectEvents(factsAfter)
        val subjectFactsAfter = subjectEvents(factsAfter)
        val statesAfter = historyProvider.getAllHistory()

        val objectFactsAdded = objectFactsAfter - objectFactsBefore
        val subjectFactsAdded = subjectFactsAfter - subjectFactsBefore

        assertEquals(1, objectFactsAdded.size, "exactly one object fact must appear")
        val objectEvent = objectFactsAdded.first().event
        assertEquals(OBJECT_CLASS, objectEvent.entityClass, "class must be correct")
        assertEquals(EventType.CREATE, objectEvent.type, "event type must be correct")

        assertEquals(1, subjectFactsAdded.size, "exactly one subject event must appear")
        val subjectEvent = subjectFactsAdded.firstOrNull()?.event
        assertEquals(SUBJECT_CLASS, subjectEvent?.entityClass, "class must be correct")
        assertEquals(EventType.UPDATE, subjectEvent?.type, "event type must be correct")

        // теперь проверяем историю на уровне справочников

        val states = statesAfter.dropLast(statesBefore.size)

        // добавляется одно состояние
        assertEquals(1, states.size, "History must contain 1 element about ref book")
        val state = states[0]

        // проверяем мета-данные о состоянии
        assertEquals(username, state.event.username)
        assertEquals(objectEvent.timestamp, state.event.timestamp)
        assertEquals(EventType.CREATE, state.event.type)
        assertEquals(HISTORY_ENTITY_OBJECT, state.event.entityClass)
        assertEquals(false, state.deleted)
        assertEquals(objectEvent.version, state.event.version)
        assertEquals(testName, state.info)

        // проверяем заголовочную часть
        assertEquals(created, state.fullData.objekt.id)
        assertEquals(objectName, state.fullData.objekt.name)
        assertEquals(objectDescription, state.fullData.objekt.description)
        assertEquals(subject.id, state.fullData.objekt.subjectId)
        assertEquals(subject.name, state.fullData.objekt.subjectName)


        // property и value отсутствует
        assertEquals(null, state.fullData.property)
        assertEquals(null, state.fullData.value)

        // проверяем changes
        assertEquals(3, state.changes.size)
        val byField = state.changes.groupBy { it.fieldName }
        assertEquals(setOf("name", "description", "subject"), byField.keys)
        assertEquals(testName, byField.getValue("name")[0].after)
        assertEquals(objectDescription, byField.getValue("description")[0].after)
        assertEquals(subject.name, byField.getValue("subject")[0].after)
        assertEquals(state.changes.map { "" }, state.changes.map { it.before })
    }

    @Test
    fun createPropertyHistoryTest() {
        val testName = "createPropertyHistoryTest"

        val objectName = testName
        val objectDescription = "object description"
        val createdObjectId = createObject(objectName, objectDescription)

        val factsBefore: Set<HistoryFact> = historyService.getAll().toSet()
        val objectFactsBefore = objectEvents(factsBefore)
        val propertyFactsBefore = propertyEvents(factsBefore)
        val statesBefore = historyProvider.getAllHistory()

        val propertyName = "prop_$testName"
        val propertyCardinality = PropertyCardinality.INFINITY.name

        val propertyRequest = PropertyCreateRequest(
            objectId = createdObjectId,
            name = propertyName, cardinality = propertyCardinality, aspectId = aspect.idStrict()
        )
        val createdPropertyId = objectService.create(propertyRequest, username)

        val factsAfter: Set<HistoryFact> = historyService.getAll().toSet()
        val objectFactsAfter = objectEvents(factsAfter)
        val propertyFactsAfter = propertyEvents(factsAfter)
        val statesAfter = historyProvider.getAllHistory()

        val objectFactsAdded = objectFactsAfter - objectFactsBefore
        val propertyFactsAdded = propertyFactsAfter - propertyFactsBefore

        assertEquals(1, propertyFactsAdded.size, "exactly one object property fact must appear")
        val propertyEvent = propertyFactsAdded.first().event
        val propertyPayload = propertyFactsAdded.first().payload
        assertEquals(OBJECT_PROPERTY_CLASS, propertyEvent.entityClass, "class must be correct")
        assertEquals(EventType.CREATE, propertyEvent.type, "event type must be correct")

        assertEquals(setOf("name", "cardinality"), propertyPayload.data.keys, "data keys must be correct")
        assertEquals(emptySet(), propertyPayload.removedLinks.keys, "there must be no removed links")
        assertEquals(setOf("aspect", "object"), propertyPayload.addedLinks.keys, "added links keys must be correct")

        assertEquals(propertyRequest.name, propertyPayload.data["name"], "name must be correct")
        assertEquals(propertyRequest.cardinality, propertyPayload.data["cardinality"], "cardinality must be correct")

        val aspectLinks = propertyPayload.addedLinks["aspect"] ?: fail("unexpected absence of aspect links")
        assertEquals(1, aspectLinks.size, "only 1 aspect must be here")
        assertEquals(aspect.id, aspectLinks.first().toString(), "aspect id must be correct")

        val objectLinks = propertyPayload.addedLinks["object"] ?: fail("unexpected absence of aspect links")
        assertEquals(1, objectLinks.size, "only 1 object must be here")
        assertEquals(createdObjectId, objectLinks.first().toString(), "object id must be correct")

        assertEquals(1, objectFactsAdded.size, "exactly one object event must appear")
        val objectEvent = objectFactsAdded.first().event
        val objectPayload = objectFactsAdded.first().payload
        assertEquals(OBJECT_CLASS, objectEvent.entityClass, "class must be correct")
        assertEquals(EventType.UPDATE, objectEvent.type, "event type must be correct")

        assertEquals(emptySet(), objectPayload.data.keys, "data keys must be empty")
        assertEquals(emptySet(), objectPayload.removedLinks.keys, "there must be no removed links")
        assertEquals(setOf("properties"), objectPayload.addedLinks.keys, "added links keys must be correct")

        val propertiesLinks = objectPayload.addedLinks["properties"] ?: fail("unexpected absence of properties links")
        assertEquals(1, propertiesLinks.size, "only 1 property must be here")
        assertEquals(createdPropertyId, propertiesLinks.first().toString(), "property id must be correct")

        // теперь проверяем историю на уровне справочников

        val states = statesAfter.dropLast(statesBefore.size)

        // ровно одно новое состояние
        assertEquals(1, states.size, "History must contain 1 element about ref book")
        val state = states[0]

        // проверяем мета-данные
        assertEquals(username, state.event.username)
        assertEquals(Long.max(propertyEvent.timestamp, objectEvent.timestamp), state.event.timestamp)
        assertEquals(EventType.UPDATE, state.event.type)
        assertEquals(HISTORY_ENTITY_OBJECT, state.event.entityClass)
        assertEquals(false, state.deleted)
        assertEquals(objectName, state.info)

        // проверяем объект
        assertEquals(createdObjectId, state.fullData.objekt.id)
        assertEquals(objectName, state.fullData.objekt.name)
        assertEquals(objectDescription, state.fullData.objekt.description)
        assertEquals(subject.id, state.fullData.objekt.subjectId)
        assertEquals(subject.name, state.fullData.objekt.subjectName)


        // проверяем свойство
        assertNotNull(state.fullData.property)
        val property = state.fullData.property ?: throw IllegalStateException("property is null")
        assertEquals(createdPropertyId, property.id)
        assertEquals(propertyName, property.name)
        assertEquals(propertyCardinality, property.cardinality)
        assertEquals(aspect.id, property.aspectId)
        assertEquals(aspect.name, property.aspectName)

        assertEquals(null, state.fullData.value)

        // проверяем изменения
        assertEquals(3, state.changes.size)
        val byField = state.changes.groupBy { it.fieldName }
        assertEquals(setOf("name", "cardinality", "aspect"), byField.keys)
        assertEquals(propertyName, byField.getValue("name")[0].after)
        assertEquals(propertyCardinality, byField.getValue("cardinality")[0].after)
        assertEquals(aspect.name, byField.getValue("aspect")[0].after)
        assertEquals(state.changes.map { "" }, state.changes.map { it.before })
    }

    private data class PreparedValueInfo(
        val propertyRequest: PropertyCreateRequest,
        val value: ObjectPropertyValue,
        val propertyId: String,
        val objectId: String,
        val propertyFacts: List<HistoryFact>,
        val valueFacts: List<HistoryFact>,
        val states: List<ObjectHistory>
    )

    private fun prepareValue(
        objectName: String,
        objectDescription: String,
        value: ObjectValueData,
        aspectPropertyId: String? = null,
        measureId: String? = null
    ): PreparedValueInfo {
        val createdObjectId = createObject(objectName, objectDescription)

        val propertyRequest = PropertyCreateRequest(
            objectId = createdObjectId,
            name = "prop_$objectName", cardinality = PropertyCardinality.INFINITY.name, aspectId = aspect.idStrict()
        )
        val createdPropertyId = objectService.create(propertyRequest, "user")

        val factsBefore: Set<HistoryFact> = historyService.getAll().toSet()

        val valueRequest = ValueCreateRequest(
            value = value, objectPropertyId = createdPropertyId,
            aspectPropertyId = aspectPropertyId, measureId = measureId, parentValueId = null
        )
        val propertyFactsBefore = propertyEvents(factsBefore)
        val valueFactsBefore = valueEvents(factsBefore)
        val statesBefore = historyProvider.getAllHistory()

        val createdValue = objectService.create(valueRequest, username)

        val factsAfter: Set<HistoryFact> = historyService.getAll().toSet()
        val propertyFactsAfter = propertyEvents(factsAfter)
        val valueFactsAfter = valueEvents(factsAfter)
        val statesAfter = historyProvider.getAllHistory()

        val propertyFactsAdded = propertyFactsAfter - propertyFactsBefore
        val valueFactsAdded = valueFactsAfter - valueFactsBefore

        val states = statesAfter.dropLast(statesBefore.size)

        return PreparedValueInfo(
            propertyRequest,
            createdValue,
            createdPropertyId,
            createdObjectId,
            propertyFactsAdded,
            valueFactsAdded,
            states
        )
    }

    private fun prepareAnotherValue(prepared: PreparedValueInfo, value: ObjectValueData): PreparedValueInfo {
        val factsBefore: Set<HistoryFact> = historyService.getAll().toSet()

        val valueRequest = ValueCreateRequest(value = value, objectPropertyId = prepared.propertyId)
        val propertyFactsBefore = propertyEvents(factsBefore)
        val valueFactsBefore = valueEvents(factsBefore)
        val statesBefore = historyProvider.getAllHistory()

        val createdValue = objectService.create(valueRequest, username)

        val factsAfter: Set<HistoryFact> = historyService.getAll().toSet()
        val propertyFactsAfter = propertyEvents(factsAfter)
        val valueFactsAfter = valueEvents(factsAfter)
        val statesAfter = historyProvider.getAllHistory()

        val propertyFactsAdded = propertyFactsAfter - propertyFactsBefore
        val valueFactsAdded = valueFactsAfter - valueFactsBefore
        val states = statesAfter.dropLast(statesBefore.size)

        return PreparedValueInfo(
            prepared.propertyRequest,
            createdValue,
            prepared.propertyId,
            prepared.objectId,
            propertyFactsAdded,
            valueFactsAdded,
            states
        )
    }

    private fun prepareChildValue(prepared: PreparedValueInfo, value: ObjectValueData): PreparedValueInfo {
        val factsBefore: Set<HistoryFact> = historyService.getAll().toSet()

        val valueRequest = ValueCreateRequest(
            value = value, objectPropertyId = prepared.propertyId,
            parentValueId = prepared.value.id?.toString(), aspectPropertyId = null, measureId = null
        )

        val propertyFactsBefore = propertyEvents(factsBefore)
        val valueFactsBefore = valueEvents(factsBefore)
        val statesBefore = historyProvider.getAllHistory()

        val createdValue = objectService.create(valueRequest, username)

        val factsAfter: Set<HistoryFact> = historyService.getAll().toSet()
        val statesAfter = historyProvider.getAllHistory()

        val propertyFactsAfter = propertyEvents(factsAfter)
        val valueFactsAfter = valueEvents(factsAfter)

        val propertyFactsAdded = propertyFactsAfter - propertyFactsBefore
        val valueFactsAdded = valueFactsAfter - valueFactsBefore
        val states = statesAfter.dropLast(statesBefore.size)

        return PreparedValueInfo(
            prepared.propertyRequest,
            createdValue,
            prepared.propertyId,
            prepared.objectId,
            propertyFactsAdded,
            valueFactsAdded,
            states
        )
    }

    private fun checkPropertyFacts(propertyFacts: List<HistoryFact>, propertyId: String, valueId: ORID) {
        assertEquals(1, propertyFacts.size, "one property event is expected")
        val propertyEvent = propertyFacts.first().event
        val propertyPayload = propertyFacts.first().payload
        assertEquals(propertyId, propertyEvent.entityId, "id must be correct")
        assertEquals(EventType.UPDATE, propertyEvent.type, "type must be correct")
        assertEquals(emptySet(), propertyPayload.data.keys, "there must be no data keys")
        assertEquals(emptySet(), propertyPayload.removedLinks.keys, "there must be no removed links")
        assertEquals(setOf("values"), propertyPayload.addedLinks.keys, "added links keys must be correct")
        val valueLinks = propertyPayload.addedLinks["values"]
        if (valueLinks != null) {
            assertEquals(listOf(valueId), valueLinks, "value id must be correct")
        } else {
            fail("value links must present")
        }
    }

    private fun checkPropertyFacts(prepared: PreparedValueInfo) {
        val valueId = prepared.value.id
        if (valueId != null) {
            checkPropertyFacts(prepared.propertyFacts, prepared.propertyId, valueId)
        } else {
            fail("value id is null")
        }
    }

    @Test
    fun createValueNullHistoryTest() {
        val testName = "createValueNullHistoryTest"

        val prepared = prepareValue(testName, "descr", ObjectValueData.NullValue)
        val valueFacts = prepared.valueFacts

        assertEquals(1, valueFacts.size, "exactly one object property event must appear")
        val valueEvent = valueFacts.first().event
        val valuePayload = valueFacts.first().payload
        assertEquals(OBJECT_PROPERTY_VALUE_CLASS, valueEvent.entityClass, "class must be correct")
        assertEquals(EventType.CREATE, valueEvent.type, "event type must be correct")

        assertEquals(setOf("typeTag"), valuePayload.data.keys, "data keys must be correct")
        assertEquals(emptySet(), valuePayload.removedLinks.keys, "there must be no removed links")
        assertEquals(setOf("objectProperty"), valuePayload.addedLinks.keys, "added links keys must be correct")

        assertEquals(ScalarTypeTag.NULL.name, valuePayload.data["typeTag"], "type tag must be correct")

        val propertyLinks = valuePayload.addedLinks["objectProperty"] ?: fail("unexpected absence of property links")
        assertEquals(1, propertyLinks.size, "only 1 property must be here")
        assertEquals(prepared.propertyId, propertyLinks.first().toString(), "property id must be correct")

        checkPropertyFacts(prepared)
    }

    @Test
    fun createValueStrHistoryTest() {
        val testName = "createValueStrHistoryTest"
        val value = "hello"
        val objectDescription = "object description"

        val prepared = prepareValue(testName, objectDescription, ObjectValueData.StringValue(value))
        val valueFacts = prepared.valueFacts

        assertEquals(1, valueFacts.size, "exactly one object property event must appear")
        val valueEvent = valueFacts.first().event
        val valuePayload = valueFacts.first().payload
        assertEquals(OBJECT_PROPERTY_VALUE_CLASS, valueEvent.entityClass, "class must be correct")
        assertEquals(EventType.CREATE, valueEvent.type, "event type must be correct")

        assertEquals(setOf("typeTag", "strValue"), valuePayload.data.keys, "data keys must be correct")
        assertEquals(emptySet(), valuePayload.removedLinks.keys, "there must be no removed links")
        assertEquals(setOf("objectProperty"), valuePayload.addedLinks.keys, "added links keys must be correct")

        assertEquals(ScalarTypeTag.STRING.name, valuePayload.data["typeTag"], "type tag must be correct")
        assertEquals(value, valuePayload.data["strValue"], "value must be correct")

        val propertyLinks = valuePayload.addedLinks["objectProperty"] ?: fail("unexpected absence of property links")
        assertEquals(1, propertyLinks.size, "only 1 property must be here")
        assertEquals(prepared.propertyId, propertyLinks.first().toString(), "property id must be correct")

        checkPropertyFacts(prepared)

        // ровно одно новое состояние
        assertEquals(1, prepared.states.size, "History must contain 1 element about ref book")
        val state = prepared.states[0]

        // проверяем мета-данные
        assertEquals(username, state.event.username)
        assertEquals(
            Long.max(valueEvent.timestamp, prepared.propertyFacts.first().event.timestamp),
            state.event.timestamp
        )
        assertEquals(EventType.UPDATE, state.event.type)
        assertEquals(HISTORY_ENTITY_OBJECT, state.event.entityClass)
        assertEquals(false, state.deleted)
        assertEquals(testName, state.info)

        // проверяем объект
        assertEquals(prepared.objectId, state.fullData.objekt.id)
        assertEquals(testName, state.fullData.objekt.name)
        assertEquals(objectDescription, state.fullData.objekt.description)
        assertEquals(subject.id, state.fullData.objekt.subjectId)
        assertEquals(subject.name, state.fullData.objekt.subjectName)

        // проверяем свойство
        assertNotNull(state.fullData.property)
        val property = state.fullData.property ?: throw IllegalStateException("property is null")
        assertEquals(prepared.propertyId, property.id)
        assertEquals(prepared.propertyRequest.name, property.name)
        assertEquals(prepared.propertyRequest.cardinality, property.cardinality)
        assertEquals(aspect.id, property.aspectId)
        assertEquals(aspect.name, property.aspectName)

        // проверяем значение
        assertNotNull(state.fullData.value)
        val propertyValue = state.fullData.value ?: throw IllegalStateException("value is null")
        assertEquals(prepared.value.id.toString(), propertyValue.id)
        assertEquals(ScalarTypeTag.STRING.name, propertyValue.typeTag)
        assertEquals(value, propertyValue.repr)
        assertEquals(null, propertyValue.aspectPropertyId)
        assertEquals(null, propertyValue.aspectPropertyName)
        assertEquals(null, propertyValue.precision)
        assertEquals(null, propertyValue.measureName)

        // проверяем изменения
        assertEquals(2, state.changes.size)
        val byField = state.changes.groupBy { it.fieldName }
        assertEquals(
            setOf("typeTag", "strValue").map { prepared.propertyRequest.name + ":" + it }.toSet(),
            byField.keys
        )
        assertEquals(ScalarTypeTag.STRING.name, byField.getValue(prepared.propertyRequest.name + ":typeTag")[0].after)
        assertEquals(value, byField.getValue(prepared.propertyRequest.name + ":strValue")[0].after)
        assertEquals(state.changes.map { "" }, state.changes.map { it.before })
    }

    @Test
    fun createValueDecimalHistoryTest() {
        val testName = "createValueDecimalHistoryTest"
        val value = "123.12"
        val objectDescription = "object description"

        val prepared = prepareValue(testName, objectDescription, ObjectValueData.DecimalValue(value))
        val valueFacts = prepared.valueFacts

        assertEquals(1, valueFacts.size, "exactly one object property event must appear")
        val valueEvent = valueFacts.first().event
        val valuePayload = valueFacts.first().payload
        assertEquals(OBJECT_PROPERTY_VALUE_CLASS, valueEvent.entityClass, "class must be correct")
        assertEquals(EventType.CREATE, valueEvent.type, "event type must be correct")

        assertEquals(setOf("typeTag", "decimalValue"), valuePayload.data.keys, "data keys must be correct")
        assertEquals(emptySet(), valuePayload.removedLinks.keys, "there must be no removed links")
        assertEquals(setOf("objectProperty"), valuePayload.addedLinks.keys, "added links keys must be correct")

        assertEquals(ScalarTypeTag.DECIMAL.name, valuePayload.data["typeTag"], "type tag must be correct")
        assertEquals(value, valuePayload.data["decimalValue"], "value must be correct")

        val propertyLinks = valuePayload.addedLinks["objectProperty"] ?: fail("unexpected absence of property links")
        assertEquals(1, propertyLinks.size, "only 1 property must be here")
        assertEquals(prepared.propertyId, propertyLinks.first().toString(), "property id must be correct")

        checkPropertyFacts(prepared)

        // ровно одно новое состояние
        assertEquals(1, prepared.states.size, "History must contain 1 element about ref book")
        val state = prepared.states[0]
        // проверяем мета-данные
        assertEquals(username, state.event.username)
        assertEquals(
            Long.max(valueEvent.timestamp, prepared.propertyFacts.first().event.timestamp),
            state.event.timestamp
        )
        assertEquals(EventType.UPDATE, state.event.type)
        assertEquals(HISTORY_ENTITY_OBJECT, state.event.entityClass)
        assertEquals(false, state.deleted)
        assertEquals(testName, state.info)

        // проверяем объект
        assertEquals(prepared.objectId, state.fullData.objekt.id)
        assertEquals(testName, state.fullData.objekt.name)
        assertEquals(objectDescription, state.fullData.objekt.description)
        assertEquals(subject.id, state.fullData.objekt.subjectId)
        assertEquals(subject.name, state.fullData.objekt.subjectName)


        // проверяем свойство
        assertNotNull(state.fullData.property)
        val property = state.fullData.property ?: throw IllegalStateException("property is null")
        assertEquals(prepared.propertyId, property.id)
        assertEquals(prepared.propertyRequest.name, property.name)
        assertEquals(prepared.propertyRequest.cardinality, property.cardinality)
        assertEquals(aspect.id, property.aspectId)
        assertEquals(aspect.name, property.aspectName)

        // проверяем значение
        assertNotNull(state.fullData.value)
        val propertyValue = state.fullData.value ?: throw IllegalStateException("value is null")
        assertEquals(prepared.value.id.toString(), propertyValue.id)
        assertEquals(ScalarTypeTag.DECIMAL.name, propertyValue.typeTag)
        assertEquals(value, propertyValue.repr)
        assertEquals(null, propertyValue.aspectPropertyId)
        assertEquals(null, propertyValue.aspectPropertyName)
        assertEquals(null, propertyValue.precision)
        assertEquals(null, propertyValue.measureName)

        // проверяем изменения
        assertEquals(2, state.changes.size)
        val byField = state.changes.groupBy { it.fieldName }
        assertEquals(
            setOf("typeTag", "decimalValue").map { prepared.propertyRequest.name + ":" + it }.toSet(),
            byField.keys
        )
        assertEquals(ScalarTypeTag.DECIMAL.name, byField.getValue(prepared.propertyRequest.name + ":typeTag")[0].after)
        assertEquals(value, byField.getValue(prepared.propertyRequest.name + ":decimalValue")[0].after)
        assertEquals(state.changes.map { "" }, state.changes.map { it.before })
    }

    @Test
    fun createValueRangeHistoryTest() {
        val testName = "createValueRangeHistoryTest"
        val range = Range(3, 5)
        val objectDescription = "object description"

        val prepared = prepareValue(testName, objectDescription, ObjectValueData.RangeValue(range))
        val valueFacts = prepared.valueFacts

        assertEquals(1, valueFacts.size, "exactly one object property event must appear")
        val valueEvent = valueFacts.first().event
        val valuePayload = valueFacts.first().payload
        assertEquals(OBJECT_PROPERTY_VALUE_CLASS, valueEvent.entityClass, "class must be correct")
        assertEquals(EventType.CREATE, valueEvent.type, "event type must be correct")

        assertEquals(setOf("typeTag", "range"), valuePayload.data.keys, "data keys must be correct")
        assertEquals(emptySet(), valuePayload.removedLinks.keys, "there must be no removed links")
        assertEquals(setOf("objectProperty"), valuePayload.addedLinks.keys, "added links keys must be correct")

        assertEquals(ScalarTypeTag.RANGE.name, valuePayload.data["typeTag"], "type tag must be correct")
        assertEquals(range.asString(), valuePayload.data["range"], "value must be correct")

        val propertyLinks = valuePayload.addedLinks["objectProperty"] ?: fail("unexpected absence of property links")
        assertEquals(1, propertyLinks.size, "only 1 property must be here")
        assertEquals(prepared.propertyId, propertyLinks.first().toString(), "property id must be correct")

        checkPropertyFacts(prepared)
        // ровно одно новое состояние
        assertEquals(1, prepared.states.size, "History must contain 1 element about ref book")
        val state = prepared.states[0]
        // проверяем мета-данные
        assertEquals(username, state.event.username)
        assertEquals(
            Long.max(valueEvent.timestamp, prepared.propertyFacts.first().event.timestamp),
            state.event.timestamp
        )
        assertEquals(EventType.UPDATE, state.event.type)
        assertEquals(HISTORY_ENTITY_OBJECT, state.event.entityClass)
        assertEquals(false, state.deleted)
        assertEquals(testName, state.info)

        // проверяем объект
        assertEquals(prepared.objectId, state.fullData.objekt.id)
        assertEquals(testName, state.fullData.objekt.name)
        assertEquals(objectDescription, state.fullData.objekt.description)
        assertEquals(subject.id, state.fullData.objekt.subjectId)
        assertEquals(subject.name, state.fullData.objekt.subjectName)

        // проверяем свойство
        assertNotNull(state.fullData.property)
        val property = state.fullData.property ?: throw IllegalStateException("property is null")
        assertEquals(prepared.propertyId, property.id)
        assertEquals(prepared.propertyRequest.name, property.name)
        assertEquals(prepared.propertyRequest.cardinality, property.cardinality)
        assertEquals(aspect.id, property.aspectId)
        assertEquals(aspect.name, property.aspectName)

        // проверяем значение
        assertNotNull(state.fullData.value)
        val propertyValue = state.fullData.value ?: throw IllegalStateException("value is null")
        assertEquals(prepared.value.id.toString(), propertyValue.id)
        assertEquals(ScalarTypeTag.RANGE.name, propertyValue.typeTag)
        assertEquals("3:5", propertyValue.repr)
        assertEquals(null, propertyValue.aspectPropertyId)
        assertEquals(null, propertyValue.aspectPropertyName)
        assertEquals(null, propertyValue.precision)
        assertEquals(null, propertyValue.measureName)

        // проверяем изменения
        assertEquals(2, state.changes.size)
        val byField = state.changes.groupBy { it.fieldName }
        assertEquals(setOf("typeTag", "range").map { prepared.propertyRequest.name + ":" + it }.toSet(), byField.keys)
        assertEquals(ScalarTypeTag.RANGE.name, byField.getValue(prepared.propertyRequest.name + ":typeTag")[0].after)
        assertEquals("3:5", byField.getValue(prepared.propertyRequest.name + ":range")[0].after)
        assertEquals(state.changes.map { "" }, state.changes.map { it.before })
    }

    @Test
    fun createValueIntHistoryTest() {
        val testName = "createValueIntHistoryTest"
        val intValue = 234
        val objectDescription = "object description"

        val prepared = prepareValue(testName, objectDescription, ObjectValueData.IntegerValue(intValue, null))
        val valueFacts = prepared.valueFacts

        assertEquals(1, valueFacts.size, "exactly one object property event must appear")
        val valueEvent = valueFacts.first().event
        val valuePayload = valueFacts.first().payload
        assertEquals(OBJECT_PROPERTY_VALUE_CLASS, valueEvent.entityClass, "class must be correct")
        assertEquals(EventType.CREATE, valueEvent.type, "event type must be correct")

        assertEquals(setOf("typeTag", "intValue"), valuePayload.data.keys, "data keys must be correct")
        assertEquals(emptySet(), valuePayload.removedLinks.keys, "there must be no removed links")
        assertEquals(setOf("objectProperty"), valuePayload.addedLinks.keys, "added links keys must be correct")

        assertEquals(ScalarTypeTag.INTEGER.name, valuePayload.data["typeTag"], "type tag must be correct")
        assertEquals(intValue.toString(), valuePayload.data["intValue"], "value must be correct")

        val propertyLinks = valuePayload.addedLinks["objectProperty"] ?: fail("unexpected absence of property links")
        assertEquals(1, propertyLinks.size, "only 1 property must be here")
        assertEquals(prepared.propertyId, propertyLinks.first().toString(), "property id must be correct")

        checkPropertyFacts(prepared)

        // ровно одно новое состояние
        assertEquals(1, prepared.states.size, "History must contain 1 element about ref book")
        val state = prepared.states[0]

        // проверяем мета-данные
        assertEquals(username, state.event.username)
        assertEquals(
            Long.max(valueEvent.timestamp, prepared.propertyFacts.first().event.timestamp),
            state.event.timestamp
        )
        assertEquals(EventType.UPDATE, state.event.type)
        assertEquals(HISTORY_ENTITY_OBJECT, state.event.entityClass)
        assertEquals(false, state.deleted)
        assertEquals(testName, state.info)

        // проверяем объект
        assertEquals(prepared.objectId, state.fullData.objekt.id)
        assertEquals(testName, state.fullData.objekt.name)
        assertEquals(objectDescription, state.fullData.objekt.description)
        assertEquals(subject.id, state.fullData.objekt.subjectId)
        assertEquals(subject.name, state.fullData.objekt.subjectName)

        // проверяем свойство
        assertNotNull(state.fullData.property)
        val property = state.fullData.property ?: throw IllegalStateException("property is null")
        assertEquals(prepared.propertyId, property.id)
        assertEquals(prepared.propertyRequest.name, property.name)
        assertEquals(prepared.propertyRequest.cardinality, property.cardinality)
        assertEquals(aspect.id, property.aspectId)
        assertEquals(aspect.name, property.aspectName)

        // проверяем значение
        assertNotNull(state.fullData.value)
        val propertyValue = state.fullData.value ?: throw IllegalStateException("value is null")
        assertEquals(prepared.value.id.toString(), propertyValue.id)
        assertEquals(ScalarTypeTag.INTEGER.name, propertyValue.typeTag)
        assertEquals(intValue.toString(), propertyValue.repr)
        assertEquals(null, propertyValue.aspectPropertyId)
        assertEquals(null, propertyValue.aspectPropertyName)
        assertEquals(null, propertyValue.precision)
        assertEquals(null, propertyValue.measureName)

        // проверяем изменения
        assertEquals(2, state.changes.size)
        val byField = state.changes.groupBy { it.fieldName }
        assertEquals(
            setOf("typeTag", "intValue").map { prepared.propertyRequest.name + ":" + it }.toSet(),
            byField.keys
        )
        assertEquals(ScalarTypeTag.INTEGER.name, byField.getValue(prepared.propertyRequest.name + ":typeTag")[0].after)
        assertEquals(intValue.toString(), byField.getValue(prepared.propertyRequest.name + ":intValue")[0].after)
        assertEquals(state.changes.map { "" }, state.changes.map { it.before })
    }


    @Test
    fun createValueIntPrecHistoryTest() {
        val testName = "createValueIntPrecHistoryTest"
        val intValue = 234
        val precision = 2
        val objectDescription = "object description"

        val prepared = prepareValue(testName, objectDescription, ObjectValueData.IntegerValue(intValue, precision))
        val valueFacts = prepared.valueFacts

        assertEquals(1, valueFacts.size, "exactly one object property event must appear")
        val valueEvent = valueFacts.first().event
        val valuePayload = valueFacts.first().payload
        assertEquals(OBJECT_PROPERTY_VALUE_CLASS, valueEvent.entityClass, "class must be correct")
        assertEquals(EventType.CREATE, valueEvent.type, "event type must be correct")

        assertEquals(setOf("typeTag", "intValue", "precision"), valuePayload.data.keys, "data keys must be correct")
        assertEquals(emptySet(), valuePayload.removedLinks.keys, "there must be no removed links")
        assertEquals(setOf("objectProperty"), valuePayload.addedLinks.keys, "added links keys must be correct")

        assertEquals(ScalarTypeTag.INTEGER.name, valuePayload.data["typeTag"], "type tag must be correct")
        assertEquals(intValue, valuePayload.data["intValue"]?.toInt(), "value must be correct")
        assertEquals(precision, valuePayload.data["precision"]?.toInt(), "precision must be correct")

        val propertyLinks = valuePayload.addedLinks["objectProperty"] ?: fail("unexpected absence of property links")
        assertEquals(1, propertyLinks.size, "only 1 property must be here")
        assertEquals(prepared.propertyId, propertyLinks.first().toString(), "property id must be correct")

        checkPropertyFacts(prepared)

        // ровно одно новое состояние
        assertEquals(1, prepared.states.size, "History must contain 1 element about ref book")
        val state = prepared.states[0]

        // проверяем мета-данные
        assertEquals(username, state.event.username)
        assertEquals(
            Long.max(valueEvent.timestamp, prepared.propertyFacts.first().event.timestamp),
            state.event.timestamp
        )
        assertEquals(EventType.UPDATE, state.event.type)
        assertEquals(HISTORY_ENTITY_OBJECT, state.event.entityClass)
        assertEquals(false, state.deleted)
        assertEquals(testName, state.info)

        // проверяем объект
        assertEquals(prepared.objectId, state.fullData.objekt.id)
        assertEquals(testName, state.fullData.objekt.name)
        assertEquals(objectDescription, state.fullData.objekt.description)
        assertEquals(subject.id, state.fullData.objekt.subjectId)
        assertEquals(subject.name, state.fullData.objekt.subjectName)

        // проверяем свойство
        assertNotNull(state.fullData.property)
        val property = state.fullData.property ?: throw IllegalStateException("property is null")
        assertEquals(prepared.propertyId, property.id)
        assertEquals(prepared.propertyRequest.name, property.name)
        assertEquals(prepared.propertyRequest.cardinality, property.cardinality)
        assertEquals(aspect.id, property.aspectId)
        assertEquals(aspect.name, property.aspectName)

        // проверяем значение
        assertNotNull(state.fullData.value)
        val propertyValue = state.fullData.value ?: throw IllegalStateException("value is null")
        assertEquals(prepared.value.id.toString(), propertyValue.id)
        assertEquals(ScalarTypeTag.INTEGER.name, propertyValue.typeTag)
        assertEquals(intValue.toString(), propertyValue.repr)
        assertEquals(null, propertyValue.aspectPropertyId)
        assertEquals(null, propertyValue.aspectPropertyName)
        assertEquals(precision.toString(), propertyValue.precision)
        assertEquals(null, propertyValue.measureName)

        // проверяем изменения
        assertEquals(3, state.changes.size)
        val byField = state.changes.groupBy { it.fieldName }
        assertEquals(
            setOf("typeTag", "intValue", "precision").map { prepared.propertyRequest.name + ":" + it }.toSet(),
            byField.keys
        )
        assertEquals(ScalarTypeTag.INTEGER.name, byField.getValue(prepared.propertyRequest.name + ":typeTag")[0].after)
        assertEquals(intValue.toString(), byField.getValue(prepared.propertyRequest.name + ":intValue")[0].after)
        assertEquals(precision.toString(), byField.getValue(prepared.propertyRequest.name + ":precision")[0].after)
        assertEquals(state.changes.map { "" }, state.changes.map { it.before })
    }

    @Test
    fun createAnotherValueHistoryTest() {
        val testName = "createAnotherValueHistoryTest"
        val value1 = "hello"
        val value2 = "world"
        val objectDescription = "object description"

        val prepared1 = prepareValue(testName, objectDescription, ObjectValueData.StringValue(value1))
        val prepared2 = prepareAnotherValue(prepared1, ObjectValueData.StringValue(value2))

        val valueFacts = prepared2.valueFacts

        assertEquals(1, valueFacts.size, "exactly two value facts must appear")

        val childEvent = valueFacts.first().event
        val childPayload = valueFacts.first().payload
        assertEquals(OBJECT_PROPERTY_VALUE_CLASS, childEvent.entityClass, "class must be correct")
        assertEquals(EventType.CREATE, childEvent.type, "event type must be correct")

        assertEquals(setOf("typeTag", "strValue"), childPayload.data.keys, "data keys must be correct")
        assertEquals(emptySet(), childPayload.removedLinks.keys, "there must be no removed links")
        assertEquals(setOf("objectProperty"), childPayload.addedLinks.keys, "added links keys must be correct")

        assertEquals(ScalarTypeTag.STRING.name, childPayload.data["typeTag"], "type tag must be correct")
        assertEquals(value2, childPayload.data["strValue"], "value must be correct")

        val propertyLinks = childPayload.addedLinks["objectProperty"] ?: fail("unexpected absence of property links")
        assertEquals(1, propertyLinks.size, "only 1 property must be here")
        assertEquals(prepared2.propertyId, propertyLinks.first().toString(), "property id must be correct")

        checkPropertyFacts(prepared2)

        // ровно одно новое состояние
        assertEquals(1, prepared2.states.size, "History must contain 1 element about ref book")
        val state = prepared2.states[0]

        // проверяем мета-данные
        assertEquals(username, state.event.username)
        assertEquals(
            Long.max(childEvent.timestamp, prepared2.propertyFacts.first().event.timestamp),
            state.event.timestamp
        )
        assertEquals(EventType.UPDATE, state.event.type)
        assertEquals(HISTORY_ENTITY_OBJECT, state.event.entityClass)
        assertEquals(false, state.deleted)
        assertEquals(testName, state.info)

        // проверяем объект
        assertEquals(prepared2.objectId, state.fullData.objekt.id)
        assertEquals(testName, state.fullData.objekt.name)
        assertEquals(objectDescription, state.fullData.objekt.description)
        assertEquals(subject.id, state.fullData.objekt.subjectId)
        assertEquals(subject.name, state.fullData.objekt.subjectName)

        // проверяем свойство
        assertNotNull(state.fullData.property)
        val property = state.fullData.property ?: throw IllegalStateException("property is null")
        assertEquals(prepared2.propertyId, property.id)
        assertEquals(prepared2.propertyRequest.name, property.name)
        assertEquals(prepared2.propertyRequest.cardinality, property.cardinality)
        assertEquals(aspect.id, property.aspectId)
        assertEquals(aspect.name, property.aspectName)

        // проверяем значение
        assertNotNull(state.fullData.value)
        val propertyValue = state.fullData.value ?: throw IllegalStateException("value is null")
        assertEquals(prepared2.value.id.toString(), propertyValue.id)
        assertEquals(ScalarTypeTag.STRING.name, propertyValue.typeTag)
        assertEquals(value2, propertyValue.repr)
        assertEquals(null, propertyValue.aspectPropertyId)
        assertEquals(null, propertyValue.aspectPropertyName)
        assertEquals(null, propertyValue.precision)
        assertEquals(null, propertyValue.measureName)

        // проверяем изменения
        assertEquals(2, state.changes.size)
        val byField = state.changes.groupBy { it.fieldName }
        assertEquals(
            setOf("typeTag", "strValue").map { prepared2.propertyRequest.name + ":" + it }.toSet(),
            byField.keys
        )
        assertEquals(ScalarTypeTag.STRING.name, byField.getValue(prepared2.propertyRequest.name + ":typeTag")[0].after)
        assertEquals(value2, byField.getValue(prepared2.propertyRequest.name + ":strValue")[0].after)
        assertEquals(state.changes.map { "" }, state.changes.map { it.before })
    }


    @Test
    fun createChildValueHistoryTest() {
        val testName = "createChildValuePrecHistoryTest"
        val value1 = "hello"
        val value2 = "world"
        val objectDescription = "object description"

        val prepared1 = prepareValue(testName, objectDescription, ObjectValueData.StringValue(value1))
        val prepared2 = prepareChildValue(prepared1, ObjectValueData.StringValue(value2))

        val valueFacts = prepared2.valueFacts

        assertEquals(2, valueFacts.size, "exactly two value facts must appear")

        val byEntity = valueFacts.groupBy { it.event.entityId }

        assertEquals(
            setOf(prepared1.value.id.toString(), prepared2.value.id.toString()),
            byEntity.keys,
            "value facts must be for proper entities"
        )

        val childFact =
            byEntity[prepared2.value.id.toString()]?.firstOrNull()
                    ?: throw IllegalStateException("no fact for child value")
        val parentFact =
            byEntity[prepared1.value.id.toString()]?.firstOrNull()
                    ?: throw IllegalStateException("no fact for child value")

        val childEvent = childFact.event
        val childPayload = childFact.payload
        assertEquals(OBJECT_PROPERTY_VALUE_CLASS, childEvent.entityClass, "class must be correct")
        assertEquals(EventType.CREATE, childEvent.type, "event type must be correct")

        assertEquals(setOf("typeTag", "strValue"), childPayload.data.keys, "data keys must be correct")
        assertEquals(emptySet(), childPayload.removedLinks.keys, "there must be no removed links")
        assertEquals(
            setOf("objectProperty", "parentValue"),
            childPayload.addedLinks.keys,
            "added links keys must be correct"
        )

        assertEquals(ScalarTypeTag.STRING.name, childPayload.data["typeTag"], "type tag must be correct")
        assertEquals(value2, childPayload.data["strValue"], "value must be correct")

        val propertyLinks = childPayload.addedLinks["objectProperty"] ?: fail("unexpected absence of property links")
        assertEquals(1, propertyLinks.size, "only 1 property must be here")
        assertEquals(prepared2.propertyId, propertyLinks.first().toString(), "property id must be correct")

        val parentLinks = childPayload.addedLinks["parentValue"] ?: fail("unexpected absence of parent value links")
        assertEquals(1, parentLinks.size, "only 1 parent value link must be here")
        assertEquals(prepared1.value.id, parentLinks.first(), "parent id must be correct")

        val parentEvent = parentFact.event
        val parentPayload = parentFact.payload

        assertEquals(OBJECT_PROPERTY_VALUE_CLASS, parentEvent.entityClass, "class must be correct")
        assertEquals(EventType.UPDATE, parentEvent.type, "event type must be correct")

        assertEquals(emptySet(), parentPayload.data.keys, "data keys must be correct")
        assertEquals(emptySet(), parentPayload.removedLinks.keys, "there must be no removed links")
        assertEquals(setOf("children"), parentPayload.addedLinks.keys, "added links keys must be correct")

        checkPropertyFacts(prepared2)

        // ровно одно новое состояние
        assertEquals(1, prepared2.states.size, "History must contain 1 element about ref book")
        val state = prepared2.states[0]

        // проверяем мета-данные
        assertEquals(username, state.event.username)
        assertEquals(
            Long.max(childEvent.timestamp, prepared2.propertyFacts.first().event.timestamp),
            state.event.timestamp
        )
        assertEquals(EventType.UPDATE, state.event.type)
        assertEquals(HISTORY_ENTITY_OBJECT, state.event.entityClass)
        assertEquals(false, state.deleted)
        assertEquals(testName, state.info)

        // проверяем объект
        assertEquals(prepared2.objectId, state.fullData.objekt.id)
        assertEquals(testName, state.fullData.objekt.name)
        assertEquals(objectDescription, state.fullData.objekt.description)
        assertEquals(subject.id, state.fullData.objekt.subjectId)
        assertEquals(subject.name, state.fullData.objekt.subjectName)

        // проверяем свойство
        assertNotNull(state.fullData.property)
        val property = state.fullData.property ?: throw IllegalStateException("property is null")
        assertEquals(prepared2.propertyId, property.id)
        assertEquals(prepared2.propertyRequest.name, property.name)
        assertEquals(prepared2.propertyRequest.cardinality, property.cardinality)
        assertEquals(aspect.id, property.aspectId)
        assertEquals(aspect.name, property.aspectName)

        // проверяем значение
        assertNotNull(state.fullData.value)
        val propertyValue = state.fullData.value ?: throw IllegalStateException("value is null")
        assertEquals(prepared2.value.id.toString(), propertyValue.id)
        assertEquals(ScalarTypeTag.STRING.name, propertyValue.typeTag)
        assertEquals(value2, propertyValue.repr)
        assertEquals(null, propertyValue.aspectPropertyId)
        assertEquals(null, propertyValue.aspectPropertyName)
        assertEquals(null, propertyValue.precision)
        assertEquals(null, propertyValue.measureName)

        // проверяем изменения
        assertEquals(2, state.changes.size)
        val byField = state.changes.groupBy { it.fieldName }
        assertEquals(
            setOf("typeTag", "strValue").map { prepared2.propertyRequest.name + ":" + it }.toSet(),
            byField.keys
        )
        assertEquals(ScalarTypeTag.STRING.name, byField.getValue(prepared2.propertyRequest.name + ":typeTag")[0].after)
        assertEquals(value2, byField.getValue(prepared2.propertyRequest.name + ":strValue")[0].after)
        assertEquals(state.changes.map { "" }, state.changes.map { it.before })
    }

    @Test
    fun createValueSubjectHistoryTest() {
        val testName = "createValueSubjectHistoryTest"
        val linkValue = ObjectValueData.Link(LinkValueData.Subject(subject.id))
        val objectDescription = "object description"

        val prepared = prepareValue(testName, objectDescription, linkValue)
        val valueFacts = prepared.valueFacts

        assertEquals(1, valueFacts.size, "exactly one object property event must appear")
        val valueEvent = valueFacts.first().event
        val valuePayload = valueFacts.first().payload
        assertEquals(OBJECT_PROPERTY_VALUE_CLASS, valueEvent.entityClass, "class must be correct")
        assertEquals(EventType.CREATE, valueEvent.type, "event type must be correct")

        assertEquals(setOf("typeTag"), valuePayload.data.keys, "data keys must be correct")
        assertEquals(emptySet(), valuePayload.removedLinks.keys, "there must be no removed links")
        assertEquals(
            setOf("objectProperty", "refValueSubject"),
            valuePayload.addedLinks.keys,
            "added links keys must be correct"
        )

        assertEquals(ScalarTypeTag.SUBJECT.name, valuePayload.data["typeTag"], "type tag must be correct")

        val subjectLinks = valuePayload.addedLinks["refValueSubject"] ?: fail("unexpected absence of subject links")
        assertEquals(1, subjectLinks.size, "only 1 subject must be here")
        assertEquals(subject.id, subjectLinks.first().toString(), "value must be correct")

        val propertyLinks = valuePayload.addedLinks["objectProperty"] ?: fail("unexpected absence of property links")
        assertEquals(1, propertyLinks.size, "only 1 property must be here")
        assertEquals(prepared.propertyId, propertyLinks.first().toString(), "property id must be correct")

        checkPropertyFacts(prepared)

        // ровно одно новое состояние
        assertEquals(1, prepared.states.size, "History must contain 1 element about ref book")
        val state = prepared.states[0]

        // проверяем мета-данные
        assertEquals(username, state.event.username)
        assertEquals(
            Long.max(valueEvent.timestamp, prepared.propertyFacts.first().event.timestamp),
            state.event.timestamp
        )
        assertEquals(EventType.UPDATE, state.event.type)
        assertEquals(HISTORY_ENTITY_OBJECT, state.event.entityClass)
        assertEquals(false, state.deleted)
        assertEquals(testName, state.info)

        // проверяем объект
        assertEquals(prepared.objectId, state.fullData.objekt.id)
        assertEquals(testName, state.fullData.objekt.name)
        assertEquals(objectDescription, state.fullData.objekt.description)
        assertEquals(subject.id, state.fullData.objekt.subjectId)
        assertEquals(subject.name, state.fullData.objekt.subjectName)

        // проверяем свойство
        assertNotNull(state.fullData.property)
        val property = state.fullData.property ?: throw IllegalStateException("property is null")
        assertEquals(prepared.propertyId, property.id)
        assertEquals(prepared.propertyRequest.name, property.name)
        assertEquals(prepared.propertyRequest.cardinality, property.cardinality)
        assertEquals(aspect.id, property.aspectId)
        assertEquals(aspect.name, property.aspectName)

        // проверяем значение
        assertNotNull(state.fullData.value)
        val propertyValue = state.fullData.value ?: throw IllegalStateException("value is null")
        assertEquals(prepared.value.id.toString(), propertyValue.id)
        assertEquals(ScalarTypeTag.SUBJECT.name, propertyValue.typeTag)
        assertEquals(subject.name, propertyValue.repr)
        assertEquals(null, propertyValue.aspectPropertyId)
        assertEquals(null, propertyValue.aspectPropertyName)
        assertEquals(null, propertyValue.precision)
        assertEquals(null, propertyValue.measureName)

        // проверяем изменения
        assertEquals(2, state.changes.size)
        val byField = state.changes.groupBy { it.fieldName }
        assertEquals(
            setOf("typeTag", "refValueSubject").map { prepared.propertyRequest.name + ":" + it }.toSet(),
            byField.keys
        )
        assertEquals(ScalarTypeTag.SUBJECT.name, byField.getValue(prepared.propertyRequest.name + ":typeTag")[0].after)
        assertEquals(subject.name, byField.getValue(prepared.propertyRequest.name + ":refValueSubject")[0].after)
        assertEquals(state.changes.map { "" }, state.changes.map { it.before })
    }

    @Test
    fun createValueObjectHistoryTest() {
        val testName = "createValueObjectHistoryTest"
        val anotherObjectName = "another_obj"
        val objectId = objectService.create(
            ObjectCreateRequest(
                name = "another_obj",
                description = null,
                subjectId = subject.id,
                subjectVersion = null
            ), "admin"
        )
        val linkValue = ObjectValueData.Link(LinkValueData.Object(objectId))
        val objectDescription = "object description"

        val prepared = prepareValue(testName, objectDescription, linkValue)
        val valueFacts = prepared.valueFacts

        assertEquals(1, valueFacts.size, "exactly one object property event must appear")
        val valueEvent = valueFacts.first().event
        val valuePayload = valueFacts.first().payload
        assertEquals(OBJECT_PROPERTY_VALUE_CLASS, valueEvent.entityClass, "class must be correct")
        assertEquals(EventType.CREATE, valueEvent.type, "event type must be correct")

        assertEquals(setOf("typeTag"), valuePayload.data.keys, "data keys must be correct")
        assertEquals(emptySet(), valuePayload.removedLinks.keys, "there must be no removed links")
        assertEquals(
            setOf("objectProperty", "refValueObject"),
            valuePayload.addedLinks.keys,
            "added links keys must be correct"
        )

        assertEquals(ScalarTypeTag.OBJECT.name, valuePayload.data["typeTag"], "type tag must be correct")

        val objectLinks = valuePayload.addedLinks["refValueObject"] ?: fail("unexpected absence of object links")
        assertEquals(1, objectLinks.size, "only 1 object must be here")
        assertEquals(objectId, objectLinks.first().toString(), "value must be correct")

        val propertyLinks = valuePayload.addedLinks["objectProperty"] ?: fail("unexpected absence of property links")
        assertEquals(1, propertyLinks.size, "only 1 property must be here")
        assertEquals(prepared.propertyId, propertyLinks.first().toString(), "property id must be correct")

        checkPropertyFacts(prepared)

        // ровно одно новое состояние
        assertEquals(1, prepared.states.size, "History must contain 1 element about ref book")
        val state = prepared.states[0]

        // проверяем мета-данные
        assertEquals(username, state.event.username)
        assertEquals(
            Long.max(valueEvent.timestamp, prepared.propertyFacts.first().event.timestamp),
            state.event.timestamp
        )
        assertEquals(EventType.UPDATE, state.event.type)
        assertEquals(HISTORY_ENTITY_OBJECT, state.event.entityClass)
        assertEquals(false, state.deleted)
        assertEquals(testName, state.info)

        // проверяем объект
        assertEquals(prepared.objectId, state.fullData.objekt.id)
        assertEquals(testName, state.fullData.objekt.name)
        assertEquals(objectDescription, state.fullData.objekt.description)
        assertEquals(subject.id, state.fullData.objekt.subjectId)
        assertEquals(subject.name, state.fullData.objekt.subjectName)

        // проверяем свойство
        assertNotNull(state.fullData.property)
        val property = state.fullData.property ?: throw IllegalStateException("property is null")
        assertEquals(prepared.propertyId, property.id)
        assertEquals(prepared.propertyRequest.name, property.name)
        assertEquals(prepared.propertyRequest.cardinality, property.cardinality)
        assertEquals(aspect.id, property.aspectId)
        assertEquals(aspect.name, property.aspectName)

        // проверяем значение
        assertNotNull(state.fullData.value)
        val propertyValue = state.fullData.value ?: throw IllegalStateException("value is null")
        assertEquals(prepared.value.id.toString(), propertyValue.id)
        assertEquals(ScalarTypeTag.OBJECT.name, propertyValue.typeTag)
        assertEquals(anotherObjectName, propertyValue.repr)
        assertEquals(null, propertyValue.aspectPropertyId)
        assertEquals(null, propertyValue.aspectPropertyName)
        assertEquals(null, propertyValue.precision)
        assertEquals(null, propertyValue.measureName)

        // проверяем изменения
        assertEquals(2, state.changes.size)
        val byField = state.changes.groupBy { it.fieldName }
        assertEquals(
            setOf("typeTag", "refValueObject").map { prepared.propertyRequest.name + ":" + it }.toSet(),
            byField.keys
        )
        assertEquals(ScalarTypeTag.OBJECT.name, byField.getValue(prepared.propertyRequest.name + ":typeTag")[0].after)
        assertEquals(anotherObjectName, byField.getValue(prepared.propertyRequest.name + ":refValueObject")[0].after)
        assertEquals(state.changes.map { "" }, state.changes.map { it.before })
    }

    @Test
    fun createValueRefBookHistoryTest() {
        val testName = "createValueObjectHistoryTest"
        val objectDescription = "object description"

        val rbiValue = "rbi_$testName"

        val refBook = refBookService.createReferenceBook("rb_$testName", aspect.idStrict(), "admin")
        val rbiId = refBookService.addReferenceBookItem(
            refBook.id,
            ReferenceBookItem(
                id = "",
                value = rbiValue,
                description = null,
                children = emptyList(),
                deleted = false,
                version = 0
            ), "admin"
        )

        val linkValue = ObjectValueData.Link(LinkValueData.DomainElement(rbiId))

        val prepared = prepareValue(testName, objectDescription, linkValue)
        val valueFacts = prepared.valueFacts

        assertEquals(1, valueFacts.size, "exactly one object property event must appear")
        val valueEvent = valueFacts.first().event
        val valuePayload = valueFacts.first().payload
        assertEquals(OBJECT_PROPERTY_VALUE_CLASS, valueEvent.entityClass, "class must be correct")
        assertEquals(EventType.CREATE, valueEvent.type, "event type must be correct")

        assertEquals(setOf("typeTag"), valuePayload.data.keys, "data keys must be correct")
        assertEquals(emptySet(), valuePayload.removedLinks.keys, "there must be no removed links")
        assertEquals(
            setOf("objectProperty", "refValueDomainElement"),
            valuePayload.addedLinks.keys,
            "added links keys must be correct"
        )

        assertEquals(ScalarTypeTag.DOMAIN_ELEMENT.name, valuePayload.data["typeTag"], "type tag must be correct")

        val domainElementLinks =
            valuePayload.addedLinks["refValueDomainElement"] ?: fail("unexpected absence of domain element links")
        assertEquals(1, domainElementLinks.size, "only 1 domain element must be here")
        assertEquals(rbiId, domainElementLinks.first().toString(), "value must be correct")

        val propertyLinks = valuePayload.addedLinks["objectProperty"] ?: fail("unexpected absence of property links")
        assertEquals(1, propertyLinks.size, "only 1 property must be here")
        assertEquals(prepared.propertyId, propertyLinks.first().toString(), "property id must be correct")

        checkPropertyFacts(prepared)

        // ровно одно новое состояние
        assertEquals(1, prepared.states.size, "History must contain 1 element about ref book")
        val state = prepared.states[0]

        // проверяем мета-данные
        assertEquals(username, state.event.username)
        assertEquals(
            Long.max(valueEvent.timestamp, prepared.propertyFacts.first().event.timestamp),
            state.event.timestamp
        )
        assertEquals(EventType.UPDATE, state.event.type)
        assertEquals(HISTORY_ENTITY_OBJECT, state.event.entityClass)
        assertEquals(false, state.deleted)
        assertEquals(testName, state.info)

        // проверяем объект
        assertEquals(prepared.objectId, state.fullData.objekt.id)
        assertEquals(testName, state.fullData.objekt.name)
        assertEquals(objectDescription, state.fullData.objekt.description)
        assertEquals(subject.id, state.fullData.objekt.subjectId)
        assertEquals(subject.name, state.fullData.objekt.subjectName)

        // проверяем свойство
        assertNotNull(state.fullData.property)
        val property = state.fullData.property ?: throw IllegalStateException("property is null")
        assertEquals(prepared.propertyId, property.id)
        assertEquals(prepared.propertyRequest.name, property.name)
        assertEquals(prepared.propertyRequest.cardinality, property.cardinality)
        assertEquals(aspect.id, property.aspectId)
        assertEquals(aspect.name, property.aspectName)

        // проверяем значение
        assertNotNull(state.fullData.value)
        val propertyValue = state.fullData.value ?: throw IllegalStateException("value is null")
        assertEquals(prepared.value.id.toString(), propertyValue.id)
        assertEquals(ScalarTypeTag.DOMAIN_ELEMENT.name, propertyValue.typeTag)
        assertEquals(rbiValue, propertyValue.repr)
        assertEquals(null, propertyValue.aspectPropertyId)
        assertEquals(null, propertyValue.aspectPropertyName)
        assertEquals(null, propertyValue.precision)
        assertEquals(null, propertyValue.measureName)

        // проверяем изменения
        assertEquals(2, state.changes.size)
        val byField = state.changes.groupBy { it.fieldName }
        assertEquals(
            setOf("typeTag", "refValueDomainElement").map { prepared.propertyRequest.name + ":" + it }.toSet(),
            byField.keys
        )
        assertEquals(
            ScalarTypeTag.DOMAIN_ELEMENT.name,
            byField.getValue(prepared.propertyRequest.name + ":typeTag")[0].after
        )
        assertEquals(rbiValue, byField.getValue(prepared.propertyRequest.name + ":refValueDomainElement")[0].after)
        assertEquals(state.changes.map { "" }, state.changes.map { it.before })
    }

    @Test
    fun createValueWithAspectPropertyHistoryTest() {
        val testName = "createValueWithAspectPropertyHistoryTest"
        val value = "hello"
        val objectDescription = "object description"
        val aspectProperty = complexAspect.properties[0]

        val prepared = prepareValue(
            objectName = testName,
            objectDescription = objectDescription,
            value = ObjectValueData.StringValue(value),
            aspectPropertyId = aspectProperty.id
        )
        val valueFacts = prepared.valueFacts

        assertEquals(1, valueFacts.size, "exactly one object property event must appear")
        val valueEvent = valueFacts.first().event
        val valuePayload = valueFacts.first().payload
        assertEquals(OBJECT_PROPERTY_VALUE_CLASS, valueEvent.entityClass, "class must be correct")
        assertEquals(EventType.CREATE, valueEvent.type, "event type must be correct")

        assertEquals(setOf("typeTag", "strValue"), valuePayload.data.keys, "data keys must be correct")
        assertEquals(emptySet(), valuePayload.removedLinks.keys, "there must be no removed links")
        assertEquals(
            setOf("objectProperty", "aspectProperty"),
            valuePayload.addedLinks.keys,
            "added links keys must be correct"
        )

        assertEquals(ScalarTypeTag.STRING.name, valuePayload.data["typeTag"], "type tag must be correct")
        assertEquals(value, valuePayload.data["strValue"], "value must be correct")

        val propertyLinks = valuePayload.addedLinks["objectProperty"] ?: fail("unexpected absence of property links")
        assertEquals(1, propertyLinks.size, "only 1 property must be here")
        assertEquals(prepared.propertyId, propertyLinks.first().toString(), "property id must be correct")

        val aspectPropertyLinks =
            valuePayload.addedLinks["aspectProperty"] ?: fail("unexpected absence of aspect property links")
        assertEquals(1, aspectPropertyLinks.size, "only 1 aspect property must be here")
        assertEquals(
            complexAspect.properties[0].id,
            aspectPropertyLinks.first().toString(),
            "aspect property id must be correct"
        )

        checkPropertyFacts(prepared)

        // ровно одно новое состояние
        assertEquals(1, prepared.states.size, "History must contain 1 element about ref book")
        val state = prepared.states[0]

        // проверяем мета-данные
        assertEquals(username, state.event.username)
        assertEquals(
            Long.max(valueEvent.timestamp, prepared.propertyFacts.first().event.timestamp),
            state.event.timestamp
        )
        assertEquals(EventType.UPDATE, state.event.type)
        assertEquals(HISTORY_ENTITY_OBJECT, state.event.entityClass)
        assertEquals(false, state.deleted)
        assertEquals(testName, state.info)

        // проверяем объект
        assertEquals(prepared.objectId, state.fullData.objekt.id)
        assertEquals(testName, state.fullData.objekt.name)
        assertEquals(objectDescription, state.fullData.objekt.description)
        assertEquals(subject.id, state.fullData.objekt.subjectId)
        assertEquals(subject.name, state.fullData.objekt.subjectName)

        // проверяем свойство
        assertNotNull(state.fullData.property)
        val property = state.fullData.property ?: throw IllegalStateException("property is null")
        assertEquals(prepared.propertyId, property.id)
        assertEquals(prepared.propertyRequest.name, property.name)
        assertEquals(prepared.propertyRequest.cardinality, property.cardinality)
        assertEquals(aspect.id, property.aspectId)
        assertEquals(aspect.name, property.aspectName)

        // проверяем значение
        assertNotNull(state.fullData.value)
        val propertyValue = state.fullData.value ?: throw IllegalStateException("value is null")
        assertEquals(prepared.value.id.toString(), propertyValue.id)
        assertEquals(ScalarTypeTag.STRING.name, propertyValue.typeTag)
        assertEquals(value, propertyValue.repr)
        assertEquals(aspectProperty.id, propertyValue.aspectPropertyId)
        assertEquals(aspectProperty.name, propertyValue.aspectPropertyName)
        assertEquals(null, propertyValue.measureName)
        assertEquals(null, propertyValue.precision)

        // проверяем изменения
        assertEquals(3, state.changes.size)
        val byField = state.changes.groupBy { it.fieldName }
        assertEquals(
            setOf(
                "typeTag",
                "strValue",
                "aspectProperty"
            ).map { prepared.propertyRequest.name + ":" + it }.toSet(), byField.keys
        )
        assertEquals(ScalarTypeTag.STRING.name, byField.getValue(prepared.propertyRequest.name + ":typeTag")[0].after)
        assertEquals(value, byField.getValue(prepared.propertyRequest.name + ":strValue")[0].after)
        assertEquals(aspectProperty.name, byField.getValue(prepared.propertyRequest.name + ":aspectProperty")[0].after)
        assertEquals(state.changes.map { "" }, state.changes.map { it.before })
    }

    @Test
    fun createValueWithMeasureHistoryTest() {
        val testName = "createValueWithMeasureHistoryTest"
        val value = "hello"
        val measure = measureService.findMeasure(VoltAmpere.name) ?: throw IllegalStateException("Not found measure")
        val objectDescription = "object description"
        val aspectProperty = complexAspect.properties[0]

        val prepared = prepareValue(
            objectName = testName, objectDescription = objectDescription, value = ObjectValueData.StringValue(value),
            aspectPropertyId = aspectProperty.id, measureId = measure?.id
        )
        val valueFacts = prepared.valueFacts

        assertEquals(1, valueFacts.size, "exactly one object property event must appear")
        val valueEvent = valueFacts.first().event
        val valuePayload = valueFacts.first().payload
        assertEquals(OBJECT_PROPERTY_VALUE_CLASS, valueEvent.entityClass, "class must be correct")
        assertEquals(EventType.CREATE, valueEvent.type, "event type must be correct")

        assertEquals(setOf("typeTag", "strValue"), valuePayload.data.keys, "data keys must be correct")
        assertEquals(emptySet(), valuePayload.removedLinks.keys, "there must be no removed links")
        assertEquals(
            setOf("objectProperty", "aspectProperty", "measure"),
            valuePayload.addedLinks.keys,
            "added links keys must be correct"
        )

        assertEquals(ScalarTypeTag.STRING.name, valuePayload.data["typeTag"], "type tag must be correct")
        assertEquals(value, valuePayload.data["strValue"], "value must be correct")

        val propertyLinks = valuePayload.addedLinks["objectProperty"] ?: fail("unexpected absence of property links")
        assertEquals(1, propertyLinks.size, "only 1 property must be here")
        assertEquals(prepared.propertyId, propertyLinks.first().toString(), "property id must be correct")

        val aspectPropertyLinks =
            valuePayload.addedLinks["aspectProperty"] ?: fail("unexpected absence of aspect property links")
        assertEquals(1, aspectPropertyLinks.size, "only 1 aspect property must be here")
        assertEquals(
            complexAspect.properties[0].id,
            aspectPropertyLinks.first().toString(),
            "aspect property id must be correct"
        )

        val measureLinks = valuePayload.addedLinks["measure"] ?: fail("unexpected absence of measure links")
        assertEquals(1, measureLinks.size, "only 1 measure must be here")
        assertEquals(measure?.id, measureLinks.first().toString(), "measure id must be correct")

        checkPropertyFacts(prepared)

        // ровно одно новое состояние
        assertEquals(1, prepared.states.size, "History must contain 1 element about ref book")
        val state = prepared.states[0]

        // проверяем мета-данные
        assertEquals(username, state.event.username)
        assertEquals(
            Long.max(valueEvent.timestamp, prepared.propertyFacts.first().event.timestamp),
            state.event.timestamp
        )
        assertEquals(EventType.UPDATE, state.event.type)
        assertEquals(HISTORY_ENTITY_OBJECT, state.event.entityClass)
        assertEquals(false, state.deleted)
        assertEquals(testName, state.info)

        // проверяем объект
        assertEquals(prepared.objectId, state.fullData.objekt.id)
        assertEquals(testName, state.fullData.objekt.name)
        assertEquals(objectDescription, state.fullData.objekt.description)
        assertEquals(subject.id, state.fullData.objekt.subjectId)
        assertEquals(subject.name, state.fullData.objekt.subjectName)

        // проверяем свойство
        assertNotNull(state.fullData.property)
        val property = state.fullData.property ?: throw IllegalStateException("property is null")
        assertEquals(prepared.propertyId, property.id)
        assertEquals(prepared.propertyRequest.name, property.name)
        assertEquals(prepared.propertyRequest.cardinality, property.cardinality)
        assertEquals(aspect.id, property.aspectId)
        assertEquals(aspect.name, property.aspectName)

        // проверяем значение
        assertNotNull(state.fullData.value)
        val propertyValue = state.fullData.value ?: throw IllegalStateException("value is null")
        assertEquals(prepared.value.id.toString(), propertyValue.id)
        assertEquals(ScalarTypeTag.STRING.name, propertyValue.typeTag)
        assertEquals(value, propertyValue.repr)
        assertEquals(aspectProperty.id, propertyValue.aspectPropertyId)
        assertEquals(aspectProperty.name, propertyValue.aspectPropertyName)
        assertEquals(measure.name, propertyValue.measureName)
        assertEquals(null, propertyValue.precision)

        // проверяем изменения
        assertEquals(4, state.changes.size)
        val byField = state.changes.groupBy { it.fieldName }
        assertEquals(
            setOf(
                "typeTag",
                "strValue",
                "aspectProperty",
                "measure"
            ).map { prepared.propertyRequest.name + ":" + it }.toSet(), byField.keys
        )
        assertEquals(ScalarTypeTag.STRING.name, byField.getValue(prepared.propertyRequest.name + ":typeTag")[0].after)
        assertEquals(value, byField.getValue(prepared.propertyRequest.name + ":strValue")[0].after)
        assertEquals(aspectProperty.name, byField.getValue(prepared.propertyRequest.name + ":aspectProperty")[0].after)
        assertEquals(measure.name, byField.getValue(prepared.propertyRequest.name + ":measure")[0].after)
        assertEquals(state.changes.map { "" }, state.changes.map { it.before })
    }

    private fun eventsByClass(events: Set<HistoryFact>, entityClass: String) =
        events.filter { it.event.entityClass == entityClass }

    private fun objectEvents(events: Set<HistoryFact>) = eventsByClass(events, OBJECT_CLASS)

    private fun subjectEvents(events: Set<HistoryFact>) = eventsByClass(events, SUBJECT_CLASS)

    private fun propertyEvents(events: Set<HistoryFact>) = eventsByClass(events, OBJECT_PROPERTY_CLASS)

    private fun valueEvents(events: Set<HistoryFact>) = eventsByClass(events, OBJECT_PROPERTY_VALUE_CLASS)

    private fun createObject(name: String, description: String = "obj descr"): String {
        val request = ObjectCreateRequest(name, description, subject.id, subject.version)
        return objectService.create(request, "user")
    }
}