package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.*
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
import com.infowings.catalog.randomName
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.id.ORID
import com.orientechnologies.orient.core.id.ORecordId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail

@ExtendWith(SpringExtension::class)
@SpringBootTest
@Suppress("StringLiteralDuplication")
class ObjectHistoryTest {
    @Autowired
    private lateinit var db: OrientDatabase
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
    private lateinit var rangeAspect: AspectData
    private lateinit var intAspect: AspectData
    private lateinit var refAspect: AspectData

    private lateinit var complexAspect: AspectData

    private val username = "admin"

    @BeforeEach
    fun initTestData() {
        transaction(db) {
            it.command("delete vertex from HistoryEvent")
        }

        subject = subjectService.createSubject(SubjectData(name = randomName(), description = "descr"), username)
        aspect = aspectService.save(AspectData(name = randomName(), description = "aspectDescr", baseType = BaseType.Text.name), username)
        rangeAspect = aspectService.save(AspectData(name = randomName(), description = "aspectDescr", baseType = BaseType.Range.name), username)
        intAspect = aspectService.save(AspectData(name = randomName(), description = "aspectDescr", baseType = BaseType.Integer.name), username)
        refAspect = aspectService.save(AspectData(name = randomName(), description = "aspectDescr", baseType = BaseType.Reference.name), username)

        val property = AspectPropertyData("", "p", aspect.idStrict(), PropertyCardinality.INFINITY.name, null)
        val complexAspectData = AspectData(
            id = "",
            name = randomName(),
            measure = Kilometre.name,
            domain = null,
            baseType = BaseType.Decimal.name,
            properties = listOf(property)
        )
        complexAspect = aspectService.save(complexAspectData, username)
    }

    private fun propertyOfComplex() = complexAspect.properties[0]

    @Test
    @Suppress("MagicNumber")
    fun createObjectHistoryTest() {
        val factsBefore: List<HistoryFact> = historyService.allTimeline(
            listOf(OrientClass.OBJECT, OrientClass.SUBJECT).map { it.extName }
        )
        val objectFactsBefore = objectEvents(factsBefore)
        val subjectFactsBefore = subjectEvents(factsBefore)
        val statesBefore = historyProvider.getAllHistory()

        val objectName = "createObjectHistoryTest"
        val objectDescription = "object description"

        val request = ObjectCreateRequest(objectName, objectDescription, subject.id)
        val createResponse = objectService.create(request, username)

        val factsAfter = historyService.allTimeline(listOf(OrientClass.OBJECT, OrientClass.SUBJECT).map { it.extName })
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
        assertEquals(objectName, state.info)

        // проверяем заголовочную часть
        assertEquals(createResponse.id, state.fullData.objekt.id)
        assertEquals(objectName, state.fullData.objekt.name)
        assertEquals(objectDescription, state.fullData.objekt.description)
        assertEquals(subject.id, state.fullData.objekt.subjectId)
        assertEquals(subject.name, state.fullData.objekt.subjectName)


        // property и valueCreateResponse отсутствует
        assertEquals(null, state.fullData.property)
        assertEquals(null, state.fullData.value)

        // проверяем changes
        assertEquals(4, state.changes.size)
        val byField = state.changes.groupBy { it.fieldName }
        assertEquals(setOf("name", "description", "subject", "guid"), byField.keys)
        assertEquals(objectName, byField.getValue("name")[0].after)
        assertEquals(objectDescription, byField.getValue("description")[0].after)
        assertEquals(subject.name, byField.getValue("subject")[0].after)
        assertEquals(state.changes.map { "" }, state.changes.map { it.before })
    }

    @Test
    @Suppress("MagicNumber")
    fun createPropertyHistoryTest() {
        val objectName = "createPropertyHistoryTest"
        val objectDescription = "object description"
        val objectCreateResponse = createObject(objectName, objectDescription)

        val factsBefore: List<HistoryFact> = historyService.allTimeline(
            listOf(OrientClass.OBJECT, OrientClass.OBJECT_PROPERTY, OrientClass.OBJECT_VALUE).map { it.extName }
        )
        val objectFactsBefore = objectEvents(factsBefore)
        val propertyFactsBefore = propertyEvents(factsBefore)
        val valueFactsBefore = valueEvents(factsBefore)
        val statesBefore = historyProvider.getAllHistory()

        val propertyName = "prop_$objectName"

        val propertyRequest = PropertyCreateRequest(
            objectId = objectCreateResponse.id,
            name = propertyName, description = null, aspectId = aspect.idStrict()
        )
        val propertyCreateResponse = objectService.create(propertyRequest, username)

        val factsAfter: List<HistoryFact> = historyService.allTimeline(
            listOf(OrientClass.OBJECT, OrientClass.OBJECT_PROPERTY, OrientClass.OBJECT_VALUE).map { it.extName }
        )
        val objectFactsAfter = objectEvents(factsAfter)
        val propertyFactsAfter = propertyEvents(factsAfter)
        val valueFactsAfter = valueEvents(factsAfter)
        val statesAfter = historyProvider.getAllHistory()

        val objectFactsAdded = objectFactsAfter - objectFactsBefore
        val propertyFactsAdded = propertyFactsAfter - propertyFactsBefore
        val valueFactsAdded = valueFactsAfter - valueFactsBefore

        assertEquals(2, propertyFactsAdded.size, "exactly one object property fact must appear")

        val propertyFactsByType = propertyFactsAdded.groupBy { it.event.type }
        assertEquals(setOf(EventType.CREATE, EventType.UPDATE), propertyFactsByType.keys)

        //val firstPropertyEvent = propertyFactsByT.event
        val createFact = propertyFactsByType[EventType.CREATE]?.first() ?: throw IllegalStateException("no creation facts")
        assertEquals(OBJECT_PROPERTY_CLASS, createFact.event.entityClass, "class must be correct")
        assertEquals(EventType.CREATE, createFact.event.type, "event type must be correct")

        val createPayload = createFact.payload
        assertEquals(setOf("name", "cardinality", "guid"), createPayload.data.keys, "data keys must be correct")
        assertEquals(emptySet(), createPayload.removedLinks.keys, "there must be no removed links")
        assertEquals(setOf("aspect", "object"), createPayload.addedLinks.keys, "added links keys must be correct")
        assertEquals(propertyRequest.name, createPayload.data["name"], "name must be correct")
        assertEquals(PropertyCardinality.ZERO.name, createPayload.data["cardinality"], "cardinality must be correct")

        val aspectLinks = createPayload.addedLinks["aspect"] ?: fail("unexpected absence of aspect links")
        assertEquals(1, aspectLinks.size, "only 1 aspect must be here")
        assertEquals(aspect.id, aspectLinks.first().toString(), "aspect id must be correct")

        val objectLinks = createPayload.addedLinks["object"] ?: fail("unexpected absence of aspect links")
        assertEquals(1, objectLinks.size, "only 1 object must be here")
        assertEquals(objectCreateResponse.id, objectLinks.first().toString(), "object id must be correct")

        val updateFact = propertyFactsByType[EventType.UPDATE]?.first() ?: throw IllegalStateException("no update facts")
        assertEquals(OBJECT_PROPERTY_CLASS, updateFact.event.entityClass, "class must be correct")
        assertEquals(EventType.UPDATE, updateFact.event.type, "event type must be correct")

        val valueLinks = updateFact.payload.addedLinks["values"] ?: fail("unexpected absence of value links")
        assertEquals(1, valueLinks.size, "only 1 default value must be attached")
        assertEquals(propertyCreateResponse.rootValue.id, valueLinks.first().toString(), "value id must be correct")

        assertEquals(1, valueFactsAdded.size, "exactly one value event must appear")
        val valueEvent = valueFactsAdded.first().event
        val valuePayload = valueFactsAdded.first().payload
        assertEquals(OBJECT_PROPERTY_VALUE_CLASS, valueEvent.entityClass, "class must be correct")
        assertEquals(EventType.CREATE, valueEvent.type, "event type must be correct")
        assertEquals(setOf("typeTag", "guid"), valuePayload.data.keys, "data keys must be correct")
        assertEquals(emptySet(), valuePayload.removedLinks.keys, "there must be no removed links")

        val valueToPropertyLinks = valuePayload.addedLinks["objectProperty"] ?: fail("unexpected absence of value links")
        assertEquals(propertyCreateResponse.id, valueToPropertyLinks.first().toString(), "object property must be correct")

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
        assertEquals(propertyCreateResponse.id, propertiesLinks.first().toString(), "property id must be correct")

        // теперь проверяем историю на уровне справочников
        val states = statesAfter.dropLast(statesBefore.size)

        // ровно одно новое состояние
        assertEquals(1, states.size, "History must contain 1 element about ref book")
        val state = states[0]

        // проверяем мета-данные
        assertEquals(username, state.event.username)
        assertEquals(valueEvent.timestamp, state.event.timestamp)
        assertEquals(EventType.UPDATE, state.event.type)
        assertEquals(HISTORY_ENTITY_OBJECT, state.event.entityClass)
        assertEquals(false, state.deleted)
        assertEquals(objectName, state.info)

        // проверяем объект
        assertEquals(objectCreateResponse.id, state.fullData.objekt.id)
        assertEquals(objectName, state.fullData.objekt.name)
        assertEquals(objectDescription, state.fullData.objekt.description)
        assertEquals(subject.id, state.fullData.objekt.subjectId)
        assertEquals(subject.name, state.fullData.objekt.subjectName)

        // проверяем свойство
        assertNotNull(state.fullData.property)
        val property = state.fullData.property ?: throw IllegalStateException("property is null")
        assertEquals(propertyCreateResponse.id, property.id)
        assertEquals(propertyName, property.name)
        assertEquals(PropertyCardinality.ZERO.name, property.cardinality)
        assertEquals(aspect.id, property.aspectId)
        assertEquals(aspect.name, property.aspectName)

        assertEquals(null, state.fullData.value)

        // проверяем изменения
        assertEquals(4, state.changes.size)
        val byField = state.changes.groupBy { it.fieldName }
        assertEquals(setOf("name", "cardinality", "aspect", "guid"), byField.keys)
        assertEquals(propertyName, byField.getValue("name")[0].after)
        assertEquals(PropertyCardinality.ZERO.name, byField.getValue("cardinality")[0].after)
        assertEquals(aspect.name, byField.getValue("aspect")[0].after)
        assertEquals(state.changes.map { "" }, state.changes.map { it.before })
    }

    private data class PreparedValueInfo(
        val propertyRequest: PropertyCreateRequest,
        val valueId: String,
        val propertyId: String,
        val objectCreateResponse: ObjectChangeResponse,
        val propertyFacts: List<HistoryFact>,
        val valueFacts: List<HistoryFact>,
        val states: List<ObjectHistory>
    )

    private fun prepareValue(
        objectName: String,
        objectDescription: String,
        value: ObjectValueData,
        aspectId: String,
        measureName: String? = null
    ): PreparedValueInfo {
        val objectCreateResponse = createObject(objectName, objectDescription)

        val propertyRequest = PropertyCreateRequest(objectId = objectCreateResponse.id, name = "prop_$objectName", description = null, aspectId = aspectId)
        val propertyCreateResponse = objectService.create(propertyRequest, "user")

        val factsBefore = historyService.allTimeline(listOf(OrientClass.OBJECT_PROPERTY, OrientClass.OBJECT_VALUE).map { it.extName })

        val valueRequest = ValueUpdateRequest(propertyCreateResponse.rootValue.id, value, measureName, null, propertyCreateResponse.rootValue.version)
        val propertyFactsBefore = propertyEvents(factsBefore)
        val valueFactsBefore = valueEvents(factsBefore)
        val statesBefore = historyProvider.getAllHistory()

        val valueUpdateResponse = objectService.update(valueRequest, username)

        val factsAfter = historyService.allTimeline(listOf(OrientClass.OBJECT_PROPERTY, OrientClass.OBJECT_VALUE).map { it.extName })
        val propertyFactsAfter = propertyEvents(factsAfter)
        val valueFactsAfter = valueEvents(factsAfter)
        val statesAfter = historyProvider.getAllHistory()

        val propertyFactsAdded = propertyFactsAfter - propertyFactsBefore
        val valueFactsAdded = valueFactsAfter.filterNot { valueFactsBefore.toSet().contains(it) }

        val states = statesAfter.dropLast(statesBefore.size)

        return PreparedValueInfo(
            propertyRequest,
            valueUpdateResponse.id,
            propertyCreateResponse.id,
            objectCreateResponse,
            propertyFactsAdded,
            valueFactsAdded,
            states
        )
    }

    private fun prepareNewValue(
        objectName: String,
        objectDescription: String,
        value: ObjectValueData,
        aspectId: String, measureId: String
    ): PreparedValueInfo {
        val objectCreateResponse = createObject(objectName, objectDescription)

        val propertyRequest = PropertyCreateRequest(objectId = objectCreateResponse.id, name = "prop_$objectName", description = null, aspectId = aspectId)
        val propertyCreateResponse = objectService.create(propertyRequest, "user")

        val factsBefore = historyService.allTimeline(listOf(OrientClass.OBJECT_PROPERTY, OrientClass.OBJECT_VALUE).map { it.extName })

        val valueRequest = ValueCreateRequest(value, null, propertyCreateResponse.id, measureId)
        val propertyFactsBefore = propertyEvents(factsBefore)
        val valueFactsBefore = valueEvents(factsBefore)
        val statesBefore = historyProvider.getAllHistory()

        val valueUpdateResponse = objectService.create(valueRequest, username)

        val factsAfter = historyService.allTimeline(listOf(OrientClass.OBJECT_PROPERTY, OrientClass.OBJECT_VALUE).map { it.extName })
        val propertyFactsAfter = propertyEvents(factsAfter)
        val valueFactsAfter = valueEvents(factsAfter)
        val statesAfter = historyProvider.getAllHistory()

        val propertyFactsAdded = propertyFactsAfter - propertyFactsBefore
        val valueFactsAdded = valueFactsAfter.filterNot { valueFactsBefore.toSet().contains(it) }

        val states = statesAfter.dropLast(statesBefore.size)

        return PreparedValueInfo(
            propertyRequest,
            valueUpdateResponse.id,
            propertyCreateResponse.id,
            objectCreateResponse,
            propertyFactsAdded,
            valueFactsAdded,
            states
        )
    }

    private fun prepareAnotherValue(prepared: PreparedValueInfo, value: ObjectValueData): PreparedValueInfo {
        val factsBefore = historyService.allTimeline(listOf(OrientClass.OBJECT_PROPERTY, OrientClass.OBJECT_VALUE).map { it.extName })

        val valueRequest = ValueCreateRequest(value, null, prepared.propertyId)
        val propertyFactsBefore = propertyEvents(factsBefore)
        val valueFactsBefore = valueEvents(factsBefore)
        val statesBefore = historyProvider.getAllHistory()

        val valueCreateResponse = objectService.create(valueRequest, username)

        val factsAfter = historyService.allTimeline(listOf(OrientClass.OBJECT_PROPERTY, OrientClass.OBJECT_VALUE).map { it.extName })
        val propertyFactsAfter = propertyEvents(factsAfter)
        val valueFactsAfter = valueEvents(factsAfter)
        val statesAfter = historyProvider.getAllHistory()

        val propertyFactsAdded = propertyFactsAfter - propertyFactsBefore
        val valueFactsAdded = valueFactsAfter - valueFactsBefore
        val states = statesAfter.dropLast(statesBefore.size)

        return PreparedValueInfo(
            prepared.propertyRequest,
            valueCreateResponse.id,
            prepared.propertyId,
            prepared.objectCreateResponse,
            propertyFactsAdded,
            valueFactsAdded,
            states
        )
    }

    private fun prepareChildValue(prepared: PreparedValueInfo, aspectPropertyId: String?, value: ObjectValueData): PreparedValueInfo {
        val factsBefore = historyService.allTimeline(listOf(OrientClass.OBJECT_PROPERTY, OrientClass.OBJECT_VALUE).map { it.extName })

        val valueRequest = ValueCreateRequest(value, null, prepared.propertyId, null, aspectPropertyId, prepared.valueId)

        val propertyFactsBefore = propertyEvents(factsBefore)
        val valueFactsBefore = valueEvents(factsBefore)
        val statesBefore = historyProvider.getAllHistory()

        val valueCreateResponse = objectService.create(valueRequest, username)

        val factsAfter = historyService.allTimeline(listOf(OrientClass.OBJECT_PROPERTY, OrientClass.OBJECT_VALUE).map { it.extName })
        val statesAfter = historyProvider.getAllHistory()

        val propertyFactsAfter = propertyEvents(factsAfter)
        val valueFactsAfter = valueEvents(factsAfter)

        val propertyFactsAdded = propertyFactsAfter - propertyFactsBefore
        val valueFactsAdded = valueFactsAfter - valueFactsBefore
        val states = statesAfter.dropLast(statesBefore.size)

        return PreparedValueInfo(
            prepared.propertyRequest,
            valueCreateResponse.id,
            prepared.propertyId,
            prepared.objectCreateResponse,
            propertyFactsAdded,
            valueFactsAdded,
            states
        )
    }

    private fun checkPropertyFacts(
        propertyFacts: List<HistoryFact>, propertyId: String,
        valueId: ORID, cardinalityUpdate: String? = PropertyCardinality.ONE.name
    ) {
        assertEquals(1, propertyFacts.size, "one property event is expected")
        val propertyEvent = propertyFacts.first().event
        val propertyPayload = propertyFacts.first().payload
        assertEquals(propertyId, propertyEvent.entityId, "id must be correct")
        assertEquals(EventType.UPDATE, propertyEvent.type, "type must be correct")
        assertEquals(cardinalityUpdate?.let { setOf("cardinality") } ?: emptySet(), propertyPayload.data.keys)
        assertEquals(cardinalityUpdate, propertyPayload.data["cardinality"])
        assertEquals(emptySet(), propertyPayload.removedLinks.keys, "there must be no removed links")
        assertEquals(setOf("values"), propertyPayload.addedLinks.keys, "added links keys must be correct")
        val valueLinks = propertyPayload.addedLinks["values"]
        if (valueLinks != null) {
            assertEquals(listOf(valueId), valueLinks, "valueCreateResponse id must be correct")
        } else {
            fail("valueCreateResponse links must present")
        }
    }

    private fun checkPropertyFacts(prepared: PreparedValueInfo, cardinalityUpdate: String? = PropertyCardinality.ONE.name) {
        val valueId = prepared.valueId
        checkPropertyFacts(prepared.propertyFacts, prepared.propertyId, ORecordId(valueId), cardinalityUpdate)
    }

    @Test
    fun createValueNullHistoryTest() {
        val testName = "createValueNullHistoryTest"

        val prepared = prepareValue(testName, "createValueNullHistoryTest-descr", ObjectValueData.NullValue, complexAspect.idStrict())
        val valueFacts = prepared.valueFacts

        assertEquals(1, valueFacts.size, "exactly one object property event must appear")
        val valueEvent = valueFacts.first().event
        val valuePayload = valueFacts.first().payload
        assertEquals(OBJECT_PROPERTY_VALUE_CLASS, valueEvent.entityClass, "class must be correct")
        assertEquals(EventType.UPDATE, valueEvent.type, "event type must be correct")

        assertEquals(emptySet(), valuePayload.data.keys, "data keys must be correct")
        assertEquals(emptySet(), valuePayload.removedLinks.keys, "there must be no removed links")
        assertEquals(emptySet(), valuePayload.addedLinks.keys, "added links keys must be correct")

        assertEquals(0, prepared.propertyFacts.size, "no property facts are expected")
    }

    @Test
    fun createValueStrHistoryTest() {
        val testName = "createValueStrHistoryTest"
        val value = "hello"
        val objectDescription = "object description"

        val prepared = prepareValue(testName, objectDescription, ObjectValueData.StringValue(value), aspect.idStrict())
        val valueFacts = prepared.valueFacts

        assertEquals(1, valueFacts.size, "exactly one object property event must appear")
        val valueEvent = valueFacts.first().event
        val valuePayload = valueFacts.first().payload
        assertEquals(OBJECT_PROPERTY_VALUE_CLASS, valueEvent.entityClass, "class must be correct")
        assertEquals(EventType.UPDATE, valueEvent.type, "event type must be correct")

        assertEquals(setOf("typeTag", "strValue"), valuePayload.data.keys, "data keys must be correct")
        assertEquals(emptySet(), valuePayload.removedLinks.keys, "there must be no removed links")
        assertEquals(emptySet(), valuePayload.addedLinks.keys, "added links keys must be correct")

        assertEquals(ScalarTypeTag.STRING.name, valuePayload.data["typeTag"], "type tag must be correct")
        assertEquals(value, valuePayload.data["strValue"], "valueCreateResponse must be correct")

        assertEquals(1, prepared.propertyFacts.size, "no property facts are expected")

        // ровно одно новое состояние
        assertEquals(1, prepared.states.size, "History must contain 1 element")
        val state = prepared.states[0]

        // проверяем мета-данные
        assertEquals(username, state.event.username)
        assertEquals(prepared.propertyFacts.first().event.timestamp, state.event.timestamp)
        assertEquals(EventType.UPDATE, state.event.type)
        assertEquals(HISTORY_ENTITY_OBJECT, state.event.entityClass)
        assertEquals(false, state.deleted)

        /*
         * Next asserts are not supported yet (for value update)
         */
        // проверяем объект
        assertEquals(prepared.objectCreateResponse.id, state.fullData.objekt.id)
        assertEquals(testName, state.fullData.objekt.name)
        assertEquals(objectDescription, state.fullData.objekt.description)
        assertEquals(subject.id, state.fullData.objekt.subjectId)
        assertEquals(subject.name, state.fullData.objekt.subjectName)

        // проверяем свойство
        assertNotNull(state.fullData.property)
        val property = state.fullData.property ?: throw IllegalStateException("property is null")
        assertEquals(prepared.propertyId, property.id)
        assertEquals(prepared.propertyRequest.name, property.name)
        assertEquals(PropertyCardinality.ONE.name, property.cardinality)
        assertEquals(aspect.id, property.aspectId)
        assertEquals(aspect.name, property.aspectName)

        // проверяем значение
        assertNotNull(state.fullData.value)
        val propertyValue = state.fullData.value ?: throw IllegalStateException("valueCreateResponse is null")
        assertEquals(prepared.valueId, propertyValue.id)
        assertEquals(ScalarTypeTag.STRING.name, propertyValue.typeTag)
        assertEquals(value, propertyValue.repr)
        assertEquals(null, propertyValue.aspectPropertyId)
        assertEquals(null, propertyValue.aspectPropertyName)
        assertEquals(null, propertyValue.precision)
        assertEquals(null, propertyValue.measureName)

        // проверяем изменения
        assertEquals(3, state.changes.size)
        val byField = state.changes.groupBy { it.fieldName }
        assertEquals(setOf("typeTag", "strValue", "cardinality").map { prepared.propertyRequest.name + ":" + it }.toSet(), byField.keys)
        assertEquals(ScalarTypeTag.STRING.name, byField.getValue(prepared.propertyRequest.name + ":typeTag")[0].after)
        assertEquals(value, byField.getValue(prepared.propertyRequest.name + ":strValue")[0].after)
        assertEquals(state.changes.map { "" }, state.changes.map { it.before })
    }

    @Test
    @Suppress("MagicNumber")
    fun createValueDecimalHistoryTest() {
        val testName = "createValueDecimalHistoryTest"
        val value = "123.12"
        val valueRepr = "1.2312E+5"
        val objectDescription = "object description"

        val prepared = prepareValue(testName, objectDescription, ObjectValueData.DecimalValue.single(value), complexAspect.idStrict(), Kilometre.name)
        val valueFacts = prepared.valueFacts

        assertEquals(1, valueFacts.size, "exactly one object property event must appear")
        val valueEvent = valueFacts.first().event
        val valuePayload = valueFacts.first().payload
        assertEquals(OBJECT_PROPERTY_VALUE_CLASS, valueEvent.entityClass, "class must be correct")
        assertEquals(EventType.UPDATE, valueEvent.type, "event type must be correct")

        assertEquals(setOf("typeTag", "decimalValue"), valuePayload.data.keys, "data keys must be correct")
        assertEquals(emptySet(), valuePayload.removedLinks.keys, "there must be no removed links")
        assertEquals(setOf("measure"), valuePayload.addedLinks.keys, "added links keys must be correct")

        assertEquals(ScalarTypeTag.DECIMAL.name, valuePayload.data["typeTag"], "type tag must be correct")
        assertEquals(valueRepr, valuePayload.data["decimalValue"], "valueCreateResponse must be correct")

        assertEquals(1, prepared.propertyFacts.size, "no property facts are expected")

        // ровно одно новое состояние
        assertEquals(1, prepared.states.size)
        val state = prepared.states[0]
        // проверяем мета-данные
        assertEquals(username, state.event.username)
        assertEquals(prepared.propertyFacts.first().event.timestamp, state.event.timestamp)
        assertEquals(EventType.UPDATE, state.event.type)
        assertEquals(HISTORY_ENTITY_OBJECT, state.event.entityClass)
        assertEquals(false, state.deleted)
        assertEquals(testName, state.info)

        /*
         * Next asserts are not supported yet (for value update)
         */
        // проверяем объект
        assertEquals(prepared.objectCreateResponse.id, state.fullData.objekt.id)
        assertEquals(testName, state.fullData.objekt.name)
        assertEquals(objectDescription, state.fullData.objekt.description)
        assertEquals(subject.id, state.fullData.objekt.subjectId)
        assertEquals(subject.name, state.fullData.objekt.subjectName)

        // проверяем свойство
        assertNotNull(state.fullData.property)
        val property = state.fullData.property ?: throw IllegalStateException("property is null")
        assertEquals(prepared.propertyId, property.id)
        assertEquals(prepared.propertyRequest.name, property.name)
        assertEquals(PropertyCardinality.ONE.name, property.cardinality)
        assertEquals(complexAspect.id, property.aspectId)
        assertEquals(complexAspect.name, property.aspectName)

        // проверяем значение
        assertNotNull(state.fullData.value)
        val propertyValue = state.fullData.value ?: throw IllegalStateException("valueCreateResponse is null")
        assertEquals(prepared.valueId, propertyValue.id)
        assertEquals(ScalarTypeTag.DECIMAL.name, propertyValue.typeTag)
        assertEquals(valueRepr, propertyValue.repr)
        assertEquals(null, propertyValue.aspectPropertyId)
        assertEquals(null, propertyValue.aspectPropertyName)
        assertEquals(null, propertyValue.precision)
        assertEquals(Kilometre.name, propertyValue.measureName)

        // проверяем изменения
        assertEquals(4, state.changes.size)
        val byField = state.changes.groupBy { it.fieldName }
        assertEquals(setOf("typeTag", "decimalValue", "cardinality", "measure").map { prepared.propertyRequest.name + ":" + it }.toSet(), byField.keys)
        assertEquals(ScalarTypeTag.DECIMAL.name, byField.getValue(prepared.propertyRequest.name + ":typeTag")[0].after)
        assertEquals(valueRepr, byField.getValue(prepared.propertyRequest.name + ":decimalValue")[0].after)
        assertEquals(state.changes.map { "" }, state.changes.map { it.before })
    }

    @Test
    fun createValueRangeHistoryTest() {
        val testName = "createValueRangeHistoryTest"
        val range = Range(3, 5)
        val objectDescription = "object description"

        val prepared = prepareValue(testName, objectDescription, ObjectValueData.RangeValue(range), rangeAspect.idStrict())
        val valueFacts = prepared.valueFacts

        assertEquals(1, valueFacts.size, "exactly one object property event must appear")
        val valueEvent = valueFacts.first().event
        val valuePayload = valueFacts.first().payload
        assertEquals(OBJECT_PROPERTY_VALUE_CLASS, valueEvent.entityClass, "class must be correct")
        assertEquals(EventType.UPDATE, valueEvent.type, "event type must be correct")

        assertEquals(setOf("typeTag", "range"), valuePayload.data.keys, "data keys must be correct")
        assertEquals(emptySet(), valuePayload.removedLinks.keys, "there must be no removed links")
        assertEquals(emptySet(), valuePayload.addedLinks.keys, "added links keys must be correct")

        assertEquals(ScalarTypeTag.RANGE.name, valuePayload.data["typeTag"], "type tag must be correct")
        assertEquals(range.asString(), valuePayload.data["range"], "valueCreateResponse must be correct")

        assertEquals(1, prepared.propertyFacts.size, "no property facts are expected")

        // ровно одно новое состояние
        assertEquals(1, prepared.states.size, "History must contain 1 element about ref book")
        val state = prepared.states[0]
        // проверяем мета-данные
        assertEquals(username, state.event.username)
        assertEquals(prepared.propertyFacts.first().event.timestamp, state.event.timestamp)
        assertEquals(EventType.UPDATE, state.event.type)
        assertEquals(HISTORY_ENTITY_OBJECT, state.event.entityClass)
        assertEquals(false, state.deleted)
        assertEquals(testName, state.info)

        // проверяем объект
        assertEquals(prepared.objectCreateResponse.id, state.fullData.objekt.id)
        assertEquals(testName, state.fullData.objekt.name)
        assertEquals(objectDescription, state.fullData.objekt.description)
        assertEquals(subject.id, state.fullData.objekt.subjectId)
        assertEquals(subject.name, state.fullData.objekt.subjectName)

        // проверяем свойство
        assertNotNull(state.fullData.property)
        val property = state.fullData.property ?: throw IllegalStateException("property is null")
        assertEquals(prepared.propertyId, property.id)
        assertEquals(prepared.propertyRequest.name, property.name)
        assertEquals(PropertyCardinality.ONE.name, property.cardinality)
        assertEquals(rangeAspect.id, property.aspectId)
        assertEquals(rangeAspect.name, property.aspectName)

        // проверяем значение
        assertNotNull(state.fullData.value)
        val propertyValue = state.fullData.value ?: throw IllegalStateException("valueCreateResponse is null")
        assertEquals(prepared.valueId, propertyValue.id)
        assertEquals(ScalarTypeTag.RANGE.name, propertyValue.typeTag)
        assertEquals("3:5", propertyValue.repr)
        assertEquals(null, propertyValue.aspectPropertyId)
        assertEquals(null, propertyValue.aspectPropertyName)
        assertEquals(null, propertyValue.precision)
        assertEquals(null, propertyValue.measureName)

        // проверяем изменения
        assertEquals(3, state.changes.size)
        val byField = state.changes.groupBy { it.fieldName }
        assertEquals(setOf("typeTag", "range", "cardinality").map { prepared.propertyRequest.name + ":" + it }.toSet(), byField.keys)
        assertEquals(ScalarTypeTag.RANGE.name, byField.getValue(prepared.propertyRequest.name + ":typeTag")[0].after)
        assertEquals("3:5", byField.getValue(prepared.propertyRequest.name + ":range")[0].after)
        assertEquals(state.changes.map { "" }, state.changes.map { it.before })
    }

    @Test
    fun createValueIntHistoryTest() {
        val testName = "createValueIntHistoryTest"
        val intValue = 234
        val objectDescription = "object description"

        val prepared = prepareValue(testName, objectDescription, ObjectValueData.IntegerValue(intValue, null), intAspect.idStrict())
        val valueFacts = prepared.valueFacts

        assertEquals(1, valueFacts.size, "exactly one object property event must appear")
        val valueEvent = valueFacts.first().event
        val valuePayload = valueFacts.first().payload
        assertEquals(OBJECT_PROPERTY_VALUE_CLASS, valueEvent.entityClass, "class must be correct")
        assertEquals(EventType.UPDATE, valueEvent.type, "event type must be correct")

        assertEquals(setOf("typeTag", "intValue"), valuePayload.data.keys, "data keys must be correct")
        assertEquals(emptySet(), valuePayload.removedLinks.keys, "there must be no removed links")
        assertEquals(emptySet(), valuePayload.addedLinks.keys, "added links keys must be correct")

        assertEquals(ScalarTypeTag.INTEGER.name, valuePayload.data["typeTag"], "type tag must be correct")
        assertEquals(intValue.toString(), valuePayload.data["intValue"], "valueCreateResponse must be correct")

        assertEquals(1, prepared.propertyFacts.size, "no property facts are expected")

        // ровно одно новое состояние
        assertEquals(1, prepared.states.size, "History must contain 1 element about ref book")
        val state = prepared.states[0]

        // проверяем мета-данные
        assertEquals(username, state.event.username)
        assertEquals(prepared.propertyFacts.first().event.timestamp, state.event.timestamp)
        assertEquals(EventType.UPDATE, state.event.type)
        assertEquals(HISTORY_ENTITY_OBJECT, state.event.entityClass)
        assertEquals(false, state.deleted)
        assertEquals(testName, state.info)

        // проверяем объект
        assertEquals(prepared.objectCreateResponse.id, state.fullData.objekt.id)
        assertEquals(testName, state.fullData.objekt.name)
        assertEquals(objectDescription, state.fullData.objekt.description)
        assertEquals(subject.id, state.fullData.objekt.subjectId)
        assertEquals(subject.name, state.fullData.objekt.subjectName)

        // проверяем свойство
        assertNotNull(state.fullData.property)
        val property = state.fullData.property ?: throw IllegalStateException("property is null")
        assertEquals(prepared.propertyId, property.id)
        assertEquals(prepared.propertyRequest.name, property.name)
        assertEquals(PropertyCardinality.ONE.name, property.cardinality)
        assertEquals(intAspect.id, property.aspectId)
        assertEquals(intAspect.name, property.aspectName)

        // проверяем значение
        assertNotNull(state.fullData.value)
        val propertyValue = state.fullData.value ?: throw IllegalStateException("valueCreateResponse is null")
        assertEquals(prepared.valueId, propertyValue.id)
        assertEquals(ScalarTypeTag.INTEGER.name, propertyValue.typeTag)
        assertEquals(intValue.toString(), propertyValue.repr)
        assertEquals(null, propertyValue.aspectPropertyId)
        assertEquals(null, propertyValue.aspectPropertyName)
        assertEquals(null, propertyValue.precision)
        assertEquals(null, propertyValue.measureName)

        // проверяем изменения
        assertEquals(3, state.changes.size)
        val byField = state.changes.groupBy { it.fieldName }
        assertEquals(setOf("typeTag", "intValue", "cardinality").map { prepared.propertyRequest.name + ":" + it }.toSet(), byField.keys)
        assertEquals(ScalarTypeTag.INTEGER.name, byField.getValue(prepared.propertyRequest.name + ":typeTag")[0].after)
        assertEquals(intValue.toString(), byField.getValue(prepared.propertyRequest.name + ":intValue")[0].after)
        assertEquals(state.changes.map { "" }, state.changes.map { it.before })
    }


    @Test
    @Suppress("MagicNumber")
    fun createValueIntPrecHistoryTest() {
        val testName = "createValueIntPrecHistoryTest"
        val intValue = 234
        val precision = 2
        val objectDescription = "object description"

        val prepared = prepareValue(testName, objectDescription, ObjectValueData.IntegerValue(intValue, precision), intAspect.idStrict())
        val valueFacts = prepared.valueFacts

        assertEquals(1, valueFacts.size, "exactly one object property event must appear")
        val valueEvent = valueFacts.first().event
        val valuePayload = valueFacts.first().payload
        assertEquals(OBJECT_PROPERTY_VALUE_CLASS, valueEvent.entityClass, "class must be correct")
        assertEquals(EventType.UPDATE, valueEvent.type, "event type must be correct")

        assertEquals(setOf("typeTag", "intValue", "precision"), valuePayload.data.keys, "data keys must be correct")
        assertEquals(emptySet(), valuePayload.removedLinks.keys, "there must be no removed links")
        assertEquals(emptySet(), valuePayload.addedLinks.keys, "added links keys must be correct")

        assertEquals(ScalarTypeTag.INTEGER.name, valuePayload.data["typeTag"], "type tag must be correct")
        assertEquals(intValue, valuePayload.data["intValue"]?.toInt(), "valueCreateResponse must be correct")
        assertEquals(precision, valuePayload.data["precision"]?.toInt(), "precision must be correct")

        assertEquals(1, prepared.propertyFacts.size, "no property facts are expected")

        // ровно одно новое состояние
        assertEquals(1, prepared.states.size, "History must contain 1 element about ref book")
        val state = prepared.states[0]

        // проверяем мета-данные
        assertEquals(username, state.event.username)
        assertEquals(prepared.propertyFacts.first().event.timestamp, state.event.timestamp)
        assertEquals(EventType.UPDATE, state.event.type)
        assertEquals(HISTORY_ENTITY_OBJECT, state.event.entityClass)
        assertEquals(false, state.deleted)
        assertEquals(testName, state.info)

        // проверяем объект
        assertEquals(prepared.objectCreateResponse.id, state.fullData.objekt.id)
        assertEquals(testName, state.fullData.objekt.name)
        assertEquals(objectDescription, state.fullData.objekt.description)
        assertEquals(subject.id, state.fullData.objekt.subjectId)
        assertEquals(subject.name, state.fullData.objekt.subjectName)

        // проверяем свойство
        assertNotNull(state.fullData.property)
        val property = state.fullData.property ?: throw IllegalStateException("property is null")
        assertEquals(prepared.propertyId, property.id)
        assertEquals(prepared.propertyRequest.name, property.name)
        assertEquals(PropertyCardinality.ONE.name, property.cardinality)
        assertEquals(intAspect.id, property.aspectId)
        assertEquals(intAspect.name, property.aspectName)

        // проверяем значение
        assertNotNull(state.fullData.value)
        val propertyValue = state.fullData.value ?: throw IllegalStateException("valueCreateResponse is null")
        assertEquals(prepared.valueId, propertyValue.id)
        assertEquals(ScalarTypeTag.INTEGER.name, propertyValue.typeTag)
        assertEquals(intValue.toString(), propertyValue.repr)
        assertEquals(null, propertyValue.aspectPropertyId)
        assertEquals(null, propertyValue.aspectPropertyName)
        assertEquals(precision.toString(), propertyValue.precision)
        assertEquals(null, propertyValue.measureName)

        // проверяем изменения
        assertEquals(4, state.changes.size)
        val byField = state.changes.groupBy { it.fieldName }
        val suffixes = setOf("typeTag", "intValue", "precision", "cardinality")
        assertEquals(suffixes.map { prepared.propertyRequest.name + ":" + it }.toSet(), byField.keys)
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

        val prepared1 = prepareValue(testName, objectDescription, ObjectValueData.StringValue(value1), aspect.idStrict())
        val prepared2 = prepareAnotherValue(prepared1, ObjectValueData.StringValue(value2))

        val valueFacts = prepared2.valueFacts

        assertEquals(1, valueFacts.size, "exactly two valueCreateResponse facts must appear")

        val childEvent = valueFacts.first().event
        val childPayload = valueFacts.first().payload
        assertEquals(OBJECT_PROPERTY_VALUE_CLASS, childEvent.entityClass, "class must be correct")
        assertEquals(EventType.CREATE, childEvent.type, "event type must be correct")

        assertEquals(setOf("typeTag", "strValue", "guid"), childPayload.data.keys, "data keys must be correct")
        assertEquals(emptySet(), childPayload.removedLinks.keys, "there must be no removed links")
        assertEquals(setOf("objectProperty"), childPayload.addedLinks.keys, "added links keys must be correct")

        assertEquals(ScalarTypeTag.STRING.name, childPayload.data["typeTag"], "type tag must be correct")
        assertEquals(value2, childPayload.data["strValue"], "valueCreateResponse must be correct")

        val propertyLinks = childPayload.addedLinks["objectProperty"] ?: fail("unexpected absence of property links")
        assertEquals(1, propertyLinks.size, "only 1 property must be here")
        assertEquals(prepared2.propertyId, propertyLinks.first().toString(), "property id must be correct")

        checkPropertyFacts(prepared2, cardinalityUpdate = PropertyCardinality.INFINITY.name)

        // ровно одно новое состояние
        assertEquals(1, prepared2.states.size, "History must contain 1 element about ref book")
        val state = prepared2.states[0]

        // проверяем мета-данные
        assertEquals(username, state.event.username)
        assertEquals(Math.max(childEvent.timestamp, prepared2.propertyFacts.first().event.timestamp), state.event.timestamp)
        assertEquals(EventType.UPDATE, state.event.type)
        assertEquals(HISTORY_ENTITY_OBJECT, state.event.entityClass)
        assertEquals(false, state.deleted)
        assertEquals(testName, state.info)

        // проверяем объект
        assertEquals(prepared2.objectCreateResponse.id, state.fullData.objekt.id)
        assertEquals(testName, state.fullData.objekt.name)
        assertEquals(objectDescription, state.fullData.objekt.description)
        assertEquals(subject.id, state.fullData.objekt.subjectId)
        assertEquals(subject.name, state.fullData.objekt.subjectName)

        // проверяем свойство
        assertNotNull(state.fullData.property)
        val property = state.fullData.property ?: throw IllegalStateException("property is null")
        assertEquals(prepared2.propertyId, property.id)
        assertEquals(prepared2.propertyRequest.name, property.name)
        assertEquals(PropertyCardinality.INFINITY.name, property.cardinality)
        assertEquals(aspect.id, property.aspectId)
        assertEquals(aspect.name, property.aspectName)

        // проверяем значение
        assertNotNull(state.fullData.value)
        val propertyValue = state.fullData.value ?: throw IllegalStateException("valueCreateResponse is null")
        assertEquals(prepared2.valueId, propertyValue.id)
        assertEquals(ScalarTypeTag.STRING.name, propertyValue.typeTag)
        assertEquals(value2, propertyValue.repr)
        assertEquals(null, propertyValue.aspectPropertyId)
        assertEquals(null, propertyValue.aspectPropertyName)
        assertEquals(null, propertyValue.precision)
        assertEquals(null, propertyValue.measureName)

        // проверяем изменения
        assertEquals(3, state.changes.size)
        val byField = state.changes.groupBy { it.fieldName }
        assertEquals(setOf("typeTag", "strValue", "guid").map { prepared2.propertyRequest.name + ":" + it }.toSet(), byField.keys)
        assertEquals(ScalarTypeTag.STRING.name, byField.getValue(prepared2.propertyRequest.name + ":typeTag")[0].after)
        assertEquals(value2, byField.getValue(prepared2.propertyRequest.name + ":strValue")[0].after)
        assertEquals(state.changes.map { "" }, state.changes.map { it.before })
    }


    @Test
    @Suppress("MagicNumber")
    fun createChildValueHistoryTest() {
        val testName = "createChildValueHistoryTest"
        val value1 = "hello"
        val value2 = "world"
        val objectDescription = "object description"

        val prepared1 = prepareValue(testName, objectDescription, ObjectValueData.StringValue(value1), aspect.idStrict())
        val prepared2 = prepareChildValue(prepared1, complexAspect.properties[0].id, ObjectValueData.StringValue(value2))

        val valueFacts = prepared2.valueFacts

        assertEquals(2, valueFacts.size, "exactly two valueCreateResponse facts must appear")

        val byEntity = valueFacts.groupBy { it.event.entityId }

        assertEquals(setOf(prepared1.valueId, prepared2.valueId), byEntity.keys, "valueCreateResponse facts must be for proper entities")

        val childFact = byEntity[prepared2.valueId]?.firstOrNull() ?: throw IllegalStateException("no fact for child valueCreateResponse")
        val parentFact = byEntity[prepared1.valueId]?.firstOrNull() ?: throw IllegalStateException("no fact for child valueCreateResponse")

        val childEvent = childFact.event
        val childPayload = childFact.payload
        assertEquals(OBJECT_PROPERTY_VALUE_CLASS, childEvent.entityClass, "class must be correct")
        assertEquals(EventType.CREATE, childEvent.type, "event type must be correct")

        assertEquals(setOf("typeTag", "strValue", "guid"), childPayload.data.keys, "data keys must be correct")
        assertEquals(emptySet(), childPayload.removedLinks.keys, "there must be no removed links")
        assertEquals(setOf("objectProperty", "parentValue", "aspectProperty"), childPayload.addedLinks.keys, "added links keys must be correct")

        assertEquals(ScalarTypeTag.STRING.name, childPayload.data["typeTag"], "type tag must be correct")
        assertEquals(value2, childPayload.data["strValue"], "valueCreateResponse must be correct")

        val propertyLinks = childPayload.addedLinks["objectProperty"] ?: fail("unexpected absence of property links")
        assertEquals(1, propertyLinks.size, "only 1 property must be here")
        assertEquals(prepared2.propertyId, propertyLinks.first().toString(), "property id must be correct")

        val parentLinks = childPayload.addedLinks["parentValue"] ?: fail("unexpected absence of parent valueCreateResponse links")
        assertEquals(1, parentLinks.size, "only 1 parent valueCreateResponse link must be here")
        assertEquals(prepared1.valueId, parentLinks.first().toString(), "parent id must be correct")

        val parentEvent = parentFact.event
        val parentPayload = parentFact.payload

        assertEquals(OBJECT_PROPERTY_VALUE_CLASS, parentEvent.entityClass, "class must be correct")
        assertEquals(EventType.UPDATE, parentEvent.type, "event type must be correct")

        assertEquals(emptySet(), parentPayload.data.keys, "data keys must be correct")
        assertEquals(emptySet(), parentPayload.removedLinks.keys, "there must be no removed links")
        assertEquals(setOf("children"), parentPayload.addedLinks.keys, "added links keys must be correct")

        checkPropertyFacts(prepared2, cardinalityUpdate = null)

        // ровно одно новое состояние
        assertEquals(1, prepared2.states.size, "History must contain 1 element about ref book")
        val state = prepared2.states[0]

        // проверяем мета-данные
        assertEquals(username, state.event.username)
        assertEquals(Math.max(childEvent.timestamp, prepared2.propertyFacts.first().event.timestamp), state.event.timestamp)
        assertEquals(EventType.UPDATE, state.event.type)
        assertEquals(HISTORY_ENTITY_OBJECT, state.event.entityClass)
        assertEquals(false, state.deleted)
        assertEquals(testName, state.info)

        // проверяем объект
        assertEquals(prepared2.objectCreateResponse.id, state.fullData.objekt.id)
        assertEquals(testName, state.fullData.objekt.name)
        assertEquals(objectDescription, state.fullData.objekt.description)
        assertEquals(subject.id, state.fullData.objekt.subjectId)
        assertEquals(subject.name, state.fullData.objekt.subjectName)

        // проверяем свойство
        assertNotNull(state.fullData.property)
        val property = state.fullData.property ?: throw IllegalStateException("property is null")
        assertEquals(prepared2.propertyId, property.id)
        assertEquals(prepared2.propertyRequest.name, property.name)
        assertEquals(PropertyCardinality.ONE.name, property.cardinality)
        assertEquals(aspect.id, property.aspectId)
        assertEquals(aspect.name, property.aspectName)

        // проверяем значение
        assertNotNull(state.fullData.value)
        val propertyValue = state.fullData.value ?: throw IllegalStateException("valueCreateResponse is null")
        assertEquals(prepared2.valueId, propertyValue.id)
        assertEquals(ScalarTypeTag.STRING.name, propertyValue.typeTag)
        assertEquals(value2, propertyValue.repr)
        assertEquals(complexAspect.properties[0].id, propertyValue.aspectPropertyId)
        assertEquals(complexAspect.properties[0].name, propertyValue.aspectPropertyName)
        assertEquals(null, propertyValue.precision)
        assertEquals(null, propertyValue.measureName)

        // проверяем изменения
        assertEquals(4, state.changes.size)
        val byField = state.changes.groupBy { it.fieldName }
        assertEquals(setOf("typeTag", "strValue", "aspectProperty", "guid").map { prepared2.propertyRequest.name + ":" + it }.toSet(), byField.keys)
        assertEquals(ScalarTypeTag.STRING.name, byField.getValue(prepared2.propertyRequest.name + ":typeTag")[0].after)
        assertEquals(value2, byField.getValue(prepared2.propertyRequest.name + ":strValue")[0].after)
        assertEquals(state.changes.map { "" }, state.changes.map { it.before })
    }

    @Test
    fun createValueSubjectHistoryTest() {
        val testName = "createValueSubjectHistoryTest"
        val linkValue = ObjectValueData.Link(LinkValueData.Subject(subject.id))
        val objectDescription = "object description"

        val prepared = prepareValue(testName, objectDescription, linkValue, refAspect.idStrict())
        val valueFacts = prepared.valueFacts

        assertEquals(1, valueFacts.size, "exactly one object property event must appear")
        val valueEvent = valueFacts.first().event
        val valuePayload = valueFacts.first().payload
        assertEquals(OBJECT_PROPERTY_VALUE_CLASS, valueEvent.entityClass, "class must be correct")
        assertEquals(EventType.UPDATE, valueEvent.type, "event type must be correct")

        assertEquals(setOf("typeTag"), valuePayload.data.keys, "data keys must be correct")
        assertEquals(emptySet(), valuePayload.removedLinks.keys, "there must be no removed links")
        assertEquals(setOf("refValueSubject"), valuePayload.addedLinks.keys, "added links keys must be correct")

        assertEquals(ScalarTypeTag.SUBJECT.name, valuePayload.data["typeTag"], "type tag must be correct")

        val subjectLinks = valuePayload.addedLinks["refValueSubject"] ?: fail("unexpected absence of subject links")
        assertEquals(1, subjectLinks.size, "only 1 subject must be here")
        assertEquals(subject.id, subjectLinks.first().toString(), "valueCreateResponse must be correct")

        assertEquals(1, prepared.propertyFacts.size, "no property facts are expected")

        // ровно одно новое состояние
        assertEquals(1, prepared.states.size, "History must contain 1 element about ref book")
        val state = prepared.states[0]

        // проверяем мета-данные
        assertEquals(username, state.event.username)
        assertEquals(prepared.propertyFacts.first().event.timestamp, state.event.timestamp)
        assertEquals(EventType.UPDATE, state.event.type)
        assertEquals(HISTORY_ENTITY_OBJECT, state.event.entityClass)
        assertEquals(false, state.deleted)
        assertEquals(testName, state.info)

        // проверяем объект
        assertEquals(prepared.objectCreateResponse.id, state.fullData.objekt.id)
        assertEquals(testName, state.fullData.objekt.name)
        assertEquals(objectDescription, state.fullData.objekt.description)
        assertEquals(subject.id, state.fullData.objekt.subjectId)
        assertEquals(subject.name, state.fullData.objekt.subjectName)

        // проверяем свойство
        assertNotNull(state.fullData.property)
        val property = state.fullData.property ?: throw IllegalStateException("property is null")
        assertEquals(prepared.propertyId, property.id)
        assertEquals(prepared.propertyRequest.name, property.name)
        assertEquals(PropertyCardinality.ONE.name, property.cardinality)
        assertEquals(refAspect.id, property.aspectId)
        assertEquals(refAspect.name, property.aspectName)

        // проверяем значение
        assertNotNull(state.fullData.value)
        val propertyValue = state.fullData.value ?: throw IllegalStateException("valueCreateResponse is null")
        assertEquals(prepared.valueId, propertyValue.id)
        assertEquals(ScalarTypeTag.SUBJECT.name, propertyValue.typeTag)
        assertEquals(subject.name, propertyValue.repr)
        assertEquals(null, propertyValue.aspectPropertyId)
        assertEquals(null, propertyValue.aspectPropertyName)
        assertEquals(null, propertyValue.precision)
        assertEquals(null, propertyValue.measureName)

        // проверяем изменения
        checkValueCreateChanges(
            state.changes,
            mapOf("typeTag" to ScalarTypeTag.SUBJECT.name, "refValueSubject" to subject.name, "cardinality" to PropertyCardinality.ONE.name),
            prepared.propertyRequest.name
        )
    }

    @Test
    fun createValueAspectHistoryTest() {
        val testName = "createValueAspectHistoryTest"
        val linkValue = ObjectValueData.Link(LinkValueData.Aspect(aspect.idStrict()))
        val objectDescription = "object description"

        val prepared = prepareValue(testName, objectDescription, linkValue, refAspect.idStrict())
        val valueFacts = prepared.valueFacts

        assertEquals(1, valueFacts.size, "exactly one object property event must appear")

        val valueEvent = valueFacts.first().event
        val valuePayload = valueFacts.first().payload
        assertEquals(OBJECT_PROPERTY_VALUE_CLASS, valueEvent.entityClass, "class must be correct")
        assertEquals(EventType.UPDATE, valueEvent.type, "event type must be correct")

        assertEquals(setOf("typeTag"), valuePayload.data.keys, "data keys must be correct")
        assertEquals(emptySet(), valuePayload.removedLinks.keys, "there must be no removed links")
        assertEquals(setOf("refValueAspect"), valuePayload.addedLinks.keys, "added links keys must be correct")

        assertEquals(ScalarTypeTag.ASPECT.name, valuePayload.data["typeTag"], "type tag must be correct")

        val aspectLinks = valuePayload.addedLinks["refValueAspect"] ?: fail("unexpected absence of aspect links")
        assertEquals(1, aspectLinks.size)
        assertEquals(linkValue.value.id, aspectLinks.first().toString(), "valueCreateResponse must be correct")

        assertEquals(1, prepared.propertyFacts.size, "no property facts are expected")

        // ровно одно новое состояние
        assertEquals(1, prepared.states.size)
        val state = prepared.states[0]

        // проверяем мета-данные
        assertEquals(username, state.event.username)
        assertEquals(prepared.propertyFacts.first().event.timestamp, state.event.timestamp)
        assertEquals(EventType.UPDATE, state.event.type)
        assertEquals(HISTORY_ENTITY_OBJECT, state.event.entityClass)
        assertEquals(false, state.deleted)
        assertEquals(testName, state.info)

        // проверяем объект
        assertEquals(prepared.objectCreateResponse.id, state.fullData.objekt.id)
        assertEquals(testName, state.fullData.objekt.name)
        assertEquals(objectDescription, state.fullData.objekt.description)
        assertEquals(subject.id, state.fullData.objekt.subjectId)
        assertEquals(subject.name, state.fullData.objekt.subjectName)

        // проверяем свойство
        assertNotNull(state.fullData.property)
        val property = state.fullData.property ?: throw IllegalStateException("property is null")
        assertEquals(prepared.propertyId, property.id)
        assertEquals(prepared.propertyRequest.name, property.name)
        assertEquals(PropertyCardinality.ONE.name, property.cardinality)
        assertEquals(refAspect.id, property.aspectId)
        assertEquals(refAspect.name, property.aspectName)

        // проверяем значение
        assertNotNull(state.fullData.value)
        val propertyValue = state.fullData.value ?: throw IllegalStateException("valueCreateResponse is null")
        assertEquals(prepared.valueId, propertyValue.id)
        assertEquals(ScalarTypeTag.ASPECT.name, propertyValue.typeTag)
        assertEquals(aspect.name, propertyValue.repr)
        assertEquals(null, propertyValue.aspectPropertyId)
        assertEquals(null, propertyValue.aspectPropertyName)
        assertEquals(null, propertyValue.precision)
        assertEquals(null, propertyValue.measureName)

        // проверяем изменения
        checkValueCreateChanges(
            state.changes,
            mapOf("typeTag" to ScalarTypeTag.ASPECT.name, "refValueAspect" to aspect.name, "cardinality" to PropertyCardinality.ONE.name),
            prepared.propertyRequest.name
        )
    }

    @Test
    fun createValueAspectPropertyHistoryTest() {
        val testName = "createValueAspectPropertyHistoryTest"
        val propertyC = propertyOfComplex()
        val linkValue = ObjectValueData.Link(LinkValueData.AspectProperty(propertyC.id))

        val objectDescription = "object description"

        val prepared = prepareValue(testName, objectDescription, linkValue, refAspect.idStrict())
        val valueFacts = prepared.valueFacts

        assertEquals(1, valueFacts.size, "exactly one object property event must appear")

        val valueEvent = valueFacts.first().event
        val valuePayload = valueFacts.first().payload
        assertEquals(OBJECT_PROPERTY_VALUE_CLASS, valueEvent.entityClass, "class must be correct")
        assertEquals(EventType.UPDATE, valueEvent.type, "event type must be correct")

        assertEquals(setOf("typeTag"), valuePayload.data.keys, "data keys must be correct")
        assertEquals(emptySet(), valuePayload.removedLinks.keys, "there must be no removed links")
        assertEquals(setOf("refValueAspectProperty"), valuePayload.addedLinks.keys, "added links keys must be correct")

        assertEquals(ScalarTypeTag.ASPECT_PROPERTY.name, valuePayload.data["typeTag"], "type tag must be correct")

        val aspectPropertyLinks = valuePayload.addedLinks["refValueAspectProperty"] ?: fail("unexpected absence of aspect propetrty links")
        assertEquals(1, aspectPropertyLinks.size)
        assertEquals(linkValue.value.id, aspectPropertyLinks.first().toString(), "valueCreateResponse must be correct")

        assertEquals(1, prepared.propertyFacts.size, "no property facts are expected")

        // ровно одно новое состояние
        assertEquals(1, prepared.states.size, "History must contain 1 element about ref book")
        val state = prepared.states[0]

        // проверяем мета-данные
        assertEquals(username, state.event.username)
        assertEquals(prepared.propertyFacts.first().event.timestamp, state.event.timestamp)
        assertEquals(EventType.UPDATE, state.event.type)
        assertEquals(HISTORY_ENTITY_OBJECT, state.event.entityClass)
        assertEquals(false, state.deleted)
        assertEquals(testName, state.info)

        // проверяем объект
        assertEquals(prepared.objectCreateResponse.id, state.fullData.objekt.id)
        assertEquals(testName, state.fullData.objekt.name)
        assertEquals(objectDescription, state.fullData.objekt.description)
        assertEquals(subject.id, state.fullData.objekt.subjectId)
        assertEquals(subject.name, state.fullData.objekt.subjectName)

        // проверяем свойство
        assertNotNull(state.fullData.property)
        val property = state.fullData.property ?: throw IllegalStateException("property is null")
        assertEquals(prepared.propertyId, property.id)
        assertEquals(prepared.propertyRequest.name, property.name)
        assertEquals(PropertyCardinality.ONE.name, property.cardinality)
        assertEquals(refAspect.id, property.aspectId)
        assertEquals(refAspect.name, property.aspectName)

        // проверяем значение
        assertNotNull(state.fullData.value)
        val propertyValue = state.fullData.value ?: throw IllegalStateException("valueCreateResponse is null")
        assertEquals(prepared.valueId, propertyValue.id)
        assertEquals(ScalarTypeTag.ASPECT_PROPERTY.name, propertyValue.typeTag)
        assertEquals(propertyC.name, propertyValue.repr)
        assertEquals(null, propertyValue.aspectPropertyId)
        assertEquals(null, propertyValue.aspectPropertyName)
        assertEquals(null, propertyValue.precision)
        assertEquals(null, propertyValue.measureName)

        // проверяем изменения
        val propertyName = propertyC.name ?: throw IllegalStateException("name is not defined")
        checkValueCreateChanges(
            state.changes,
            mapOf("typeTag" to ScalarTypeTag.ASPECT_PROPERTY.name, "refValueAspectProperty" to propertyName, "cardinality" to PropertyCardinality.ONE.name),
            prepared.propertyRequest.name
        )
    }


    @Test
    fun createValueObjectHistoryTest() {
        val testName = "createValueObjectHistoryTest"
        val anotherObjectName = "another_obj"
        val objectCreateResponse = objectService.create(
            ObjectCreateRequest(
                name = anotherObjectName,
                description = null,
                subjectId = subject.id
            ), "admin"
        )
        val linkValue = ObjectValueData.Link(LinkValueData.Object(objectCreateResponse.id))
        val objectDescription = "object description"

        val prepared = prepareValue(testName, objectDescription, linkValue, refAspect.idStrict())
        val valueFacts = prepared.valueFacts

        assertEquals(1, valueFacts.size, "exactly one object property event must appear")
        val valueEvent = valueFacts.first().event
        val valuePayload = valueFacts.first().payload
        assertEquals(OBJECT_PROPERTY_VALUE_CLASS, valueEvent.entityClass, "class must be correct")
        assertEquals(EventType.UPDATE, valueEvent.type, "event type must be correct")

        assertEquals(setOf("typeTag"), valuePayload.data.keys, "data keys must be correct")
        assertEquals(emptySet(), valuePayload.removedLinks.keys, "there must be no removed links")
        assertEquals(setOf("refValueObject"), valuePayload.addedLinks.keys, "added links keys must be correct")

        assertEquals(ScalarTypeTag.OBJECT.name, valuePayload.data["typeTag"], "type tag must be correct")

        val objectLinks = valuePayload.addedLinks["refValueObject"] ?: fail("unexpected absence of object links")
        assertEquals(1, objectLinks.size, "only 1 object must be here")
        assertEquals(objectCreateResponse.id, objectLinks.first().toString(), "valueCreateResponse must be correct")

        assertEquals(1, prepared.propertyFacts.size, "no property facts are expected")

        // ровно одно новое состояние
        assertEquals(1, prepared.states.size, "History must contain 1 element about ref book")
        val state = prepared.states[0]

        // проверяем мета-данные
        assertEquals(username, state.event.username)
        assertEquals(prepared.propertyFacts.first().event.timestamp, state.event.timestamp)
        assertEquals(EventType.UPDATE, state.event.type)
        assertEquals(HISTORY_ENTITY_OBJECT, state.event.entityClass)
        assertEquals(false, state.deleted)
        assertEquals(testName, state.info)

        // проверяем объект
        assertEquals(prepared.objectCreateResponse.id, state.fullData.objekt.id)
        assertEquals(testName, state.fullData.objekt.name)
        assertEquals(objectDescription, state.fullData.objekt.description)
        assertEquals(subject.id, state.fullData.objekt.subjectId)
        assertEquals(subject.name, state.fullData.objekt.subjectName)

        // проверяем свойство
        assertNotNull(state.fullData.property)
        val property = state.fullData.property ?: throw IllegalStateException("property is null")
        assertEquals(prepared.propertyId, property.id)
        assertEquals(prepared.propertyRequest.name, property.name)
        assertEquals(PropertyCardinality.ONE.name, property.cardinality)
        assertEquals(refAspect.id, property.aspectId)
        assertEquals(refAspect.name, property.aspectName)

        // проверяем значение
        assertNotNull(state.fullData.value)
        val propertyValue = state.fullData.value ?: throw IllegalStateException("valueCreateResponse is null")
        assertEquals(prepared.valueId, propertyValue.id)
        assertEquals(ScalarTypeTag.OBJECT.name, propertyValue.typeTag)
        assertEquals(anotherObjectName, propertyValue.repr)
        assertEquals(null, propertyValue.aspectPropertyId)
        assertEquals(null, propertyValue.aspectPropertyName)
        assertEquals(null, propertyValue.precision)
        assertEquals(null, propertyValue.measureName)

        // проверяем изменения
        checkValueCreateChanges(
            state.changes,
            mapOf("typeTag" to ScalarTypeTag.OBJECT.name, "refValueObject" to anotherObjectName, "cardinality" to PropertyCardinality.ONE.name),
            prepared.propertyRequest.name
        )
    }

    @Test
    fun createValueObjectPropertyHistoryTest() {
        val testName = "createValueObjectPropertyHistoryTest"
        val anotherObjectName = "another_obj"
        val objectCreateResponse = objectService.create(ObjectCreateRequest(name = anotherObjectName, description = null, subjectId = subject.id), "admin")

        val anotherPropName = "another_prop"
        val propertyCreateResponse = objectService.create(
            PropertyCreateRequest(name = anotherPropName, description = null, aspectId = aspect.idStrict(), objectId = objectCreateResponse.id),
            "admin"
        )


        val linkValue = ObjectValueData.Link(LinkValueData.ObjectProperty(propertyCreateResponse.id))
        val objectDescription = "object description"

        val prepared = prepareValue(testName, objectDescription, linkValue, refAspect.idStrict())
        val valueFacts = prepared.valueFacts

        assertEquals(1, valueFacts.size, "exactly one object property event must appear")

        val valueEvent = valueFacts.first().event
        val valuePayload = valueFacts.first().payload
        assertEquals(OBJECT_PROPERTY_VALUE_CLASS, valueEvent.entityClass, "class must be correct")

        assertEquals(EventType.UPDATE, valueEvent.type, "event type must be correct")
        assertEquals(setOf("typeTag"), valuePayload.data.keys, "data keys must be correct")
        assertEquals(emptySet(), valuePayload.removedLinks.keys, "there must be no removed links")
        assertEquals(setOf("refValueObjectProperty"), valuePayload.addedLinks.keys, "added links keys must be correct")

        assertEquals(ScalarTypeTag.OBJECT_PROPERTY.name, valuePayload.data["typeTag"], "type tag must be correct")

        val propertyLinksRef = valuePayload.addedLinks["refValueObjectProperty"] ?: fail("unexpected absence of object property links")
        assertEquals(1, propertyLinksRef.size)
        assertEquals(linkValue.value.id, propertyLinksRef.first().toString(), "valueCreateResponse must be correct")

        assertEquals(1, prepared.propertyFacts.size, "no property facts are expected")

        // ровно одно новое состояние
        assertEquals(1, prepared.states.size, "History must contain 1 element about ref book")
        val state = prepared.states[0]

        // проверяем мета-данные
        assertEquals(username, state.event.username)
        assertEquals(prepared.propertyFacts.first().event.timestamp, state.event.timestamp)
        assertEquals(EventType.UPDATE, state.event.type)
        assertEquals(HISTORY_ENTITY_OBJECT, state.event.entityClass)
        assertEquals(false, state.deleted)
        assertEquals(testName, state.info)

        // проверяем объект
        assertEquals(prepared.objectCreateResponse.id, state.fullData.objekt.id)
        assertEquals(testName, state.fullData.objekt.name)
        assertEquals(objectDescription, state.fullData.objekt.description)
        assertEquals(subject.id, state.fullData.objekt.subjectId)
        assertEquals(subject.name, state.fullData.objekt.subjectName)

        // проверяем свойство
        assertNotNull(state.fullData.property)
        val property = state.fullData.property ?: throw IllegalStateException("property is null")
        assertEquals(prepared.propertyId, property.id)
        assertEquals(prepared.propertyRequest.name, property.name)
        assertEquals(PropertyCardinality.ONE.name, property.cardinality)
        assertEquals(refAspect.id, property.aspectId)
        assertEquals(refAspect.name, property.aspectName)

        // проверяем значение
        assertNotNull(state.fullData.value)
        val propertyValue = state.fullData.value ?: throw IllegalStateException("valueCreateResponse is null")
        assertEquals(prepared.valueId, propertyValue.id)
        assertEquals(ScalarTypeTag.OBJECT_PROPERTY.name, propertyValue.typeTag)
        assertEquals(anotherPropName, propertyValue.repr)
        assertEquals(null, propertyValue.aspectPropertyId)
        assertEquals(null, propertyValue.aspectPropertyName)
        assertEquals(null, propertyValue.precision)
        assertEquals(null, propertyValue.measureName)

        // проверяем изменения
        checkValueCreateChanges(
            state.changes,
            mapOf("typeTag" to ScalarTypeTag.OBJECT_PROPERTY.name, "refValueObjectProperty" to anotherPropName, "cardinality" to PropertyCardinality.ONE.name),
            prepared.propertyRequest.name
        )
    }

    @Test
    fun createValueObjectPropertyValueHistoryTest() {
        val testName = "createValueObjectPropertyValueHistoryTest"
        val objectCreateResponse = objectService.create(ObjectCreateRequest(name = "another_obj", description = null, subjectId = subject.id), "admin")

        val propertyCreateResponse = objectService.create(
            PropertyCreateRequest(
                name = "another_prop",
                description = null,
                aspectId = aspect.idStrict(), objectId = objectCreateResponse.id
            ), "admin"
        )

        val objectValue = objectService.create(
            ValueCreateRequest(
                value = ObjectValueData.StringValue("valueCreateResponse"),
                description = null,
                objectPropertyId = propertyCreateResponse.id
            ),
            "admin"
        )

        val linkValue = ObjectValueData.Link(LinkValueData.ObjectValue(objectValue.id))

        val objectDescription = "object description"

        val prepared = prepareValue(testName, objectDescription, linkValue, refAspect.idStrict())
        val valueFacts = prepared.valueFacts

        assertEquals(1, valueFacts.size, "exactly one object property event must appear")

        val valueEvent = valueFacts.first().event
        val valuePayload = valueFacts.first().payload
        assertEquals(OBJECT_PROPERTY_VALUE_CLASS, valueEvent.entityClass, "class must be correct")
        assertEquals(EventType.UPDATE, valueEvent.type, "event type must be correct")

        assertEquals(setOf("typeTag"), valuePayload.data.keys, "data keys must be correct")
        assertEquals(emptySet(), valuePayload.removedLinks.keys, "there must be no removed links")
        assertEquals(setOf("refValueObjectValue"), valuePayload.addedLinks.keys, "added links keys must be correct")

        assertEquals(ScalarTypeTag.OBJECT_VALUE.name, valuePayload.data["typeTag"], "type tag must be correct")

        val objectLinks = valuePayload.addedLinks["refValueObjectValue"] ?: fail("unexpected absence of object links")

        assertEquals(1, objectLinks.size, "only 1 object must be here")
        assertEquals(linkValue.value.id, objectLinks.first().toString(), "valueCreateResponse must be correct")

        assertEquals(1, prepared.propertyFacts.size, "no property facts are expected")

        // ровно одно новое состояние
        assertEquals(1, prepared.states.size, "History must contain 1 element about ref book")
        val state = prepared.states[0]

        // проверяем мета-данные
        assertEquals(username, state.event.username)
        assertEquals(prepared.states.first().event.timestamp, state.event.timestamp)
        assertEquals(EventType.UPDATE, state.event.type)
        assertEquals(HISTORY_ENTITY_OBJECT, state.event.entityClass)
        assertEquals(false, state.deleted)
        assertEquals(testName, state.info)

        // проверяем объект
        assertEquals(prepared.objectCreateResponse.id, state.fullData.objekt.id)
        assertEquals(testName, state.fullData.objekt.name)
        assertEquals(objectDescription, state.fullData.objekt.description)
        assertEquals(subject.id, state.fullData.objekt.subjectId)
        assertEquals(subject.name, state.fullData.objekt.subjectName)

        // проверяем свойство
        assertNotNull(state.fullData.property)
        val property = state.fullData.property ?: throw IllegalStateException("property is null")
        assertEquals(prepared.propertyId, property.id)
        assertEquals(prepared.propertyRequest.name, property.name)
        //assertEquals(PropertyCardinality.ONE.name, property.cardinality)
        assertEquals(refAspect.id, property.aspectId)
        assertEquals(refAspect.name, property.aspectName)

        // проверяем значение
        assertNotNull(state.fullData.value)
        val propertyValue = state.fullData.value ?: throw IllegalStateException("valueCreateResponse is null")
        assertEquals(prepared.valueId, propertyValue.id)
        assertEquals(ScalarTypeTag.OBJECT_VALUE.name, propertyValue.typeTag)
        assertEquals(objectValue.id, propertyValue.repr)
        assertEquals(null, propertyValue.aspectPropertyId)
        assertEquals(null, propertyValue.aspectPropertyName)
        assertEquals(null, propertyValue.precision)
        assertEquals(null, propertyValue.measureName)

        // проверяем изменения
        checkValueCreateChanges(
            state.changes,
            mapOf("typeTag" to ScalarTypeTag.OBJECT_VALUE.name, "refValueObjectValue" to objectValue.id, "cardinality" to PropertyCardinality.ONE.name),
            prepared.propertyRequest.name
        )
    }

    @Test
    fun createValueRefBookHistoryTest() {
        val testName = "createValueRefBookHistoryTest"
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
                version = 0, guid = null
            ), "admin"
        )

        val linkValue = ObjectValueData.Link(LinkValueData.DomainElement(rbiId))

        val prepared = prepareValue(testName, objectDescription, linkValue, aspect.idStrict())

        val valueFacts = prepared.valueFacts

        assertEquals(1, valueFacts.size, "exactly one object property event must appear")
        val valueEvent = valueFacts.first().event
        val valuePayload = valueFacts.first().payload
        assertEquals(OBJECT_PROPERTY_VALUE_CLASS, valueEvent.entityClass, "class must be correct")
        assertEquals(EventType.UPDATE, valueEvent.type, "event type must be correct")

        assertEquals(setOf("typeTag"), valuePayload.data.keys, "data keys must be correct")
        assertEquals(emptySet(), valuePayload.removedLinks.keys, "there must be no removed links")
        assertEquals(setOf("refValueDomainElement"), valuePayload.addedLinks.keys, "added links keys must be correct")

        assertEquals(ScalarTypeTag.DOMAIN_ELEMENT.name, valuePayload.data["typeTag"], "type tag must be correct")

        val domainElementLinks =
            valuePayload.addedLinks["refValueDomainElement"] ?: fail("unexpected absence of domain element links")
        assertEquals(1, domainElementLinks.size, "only 1 domain element must be here")
        assertEquals(rbiId, domainElementLinks.first().toString(), "valueCreateResponse must be correct")

        assertEquals(1, prepared.propertyFacts.size, "no property facts are expected")

        // ровно одно новое состояние
        assertEquals(1, prepared.states.size, "History must contain 1 element about ref book")
        val state = prepared.states[0]

        // проверяем мета-данные
        assertEquals(username, state.event.username)
        assertEquals(prepared.propertyFacts.first().event.timestamp, state.event.timestamp)
        assertEquals(EventType.UPDATE, state.event.type)
        assertEquals(HISTORY_ENTITY_OBJECT, state.event.entityClass)
        assertEquals(false, state.deleted)
        assertEquals(testName, state.info)

        // проверяем объект
        assertEquals(prepared.objectCreateResponse.id, state.fullData.objekt.id)
        assertEquals(testName, state.fullData.objekt.name)
        assertEquals(objectDescription, state.fullData.objekt.description)
        assertEquals(subject.id, state.fullData.objekt.subjectId)
        assertEquals(subject.name, state.fullData.objekt.subjectName)

        // проверяем свойство
        assertNotNull(state.fullData.property)
        val property = state.fullData.property ?: throw IllegalStateException("property is null")
        assertEquals(prepared.propertyId, property.id)
        assertEquals(prepared.propertyRequest.name, property.name)
        assertEquals(PropertyCardinality.ONE.name, property.cardinality)
        assertEquals(aspect.id, property.aspectId)
        assertEquals(aspect.name, property.aspectName)

        // проверяем значение
        assertNotNull(state.fullData.value)
        val propertyValue = state.fullData.value ?: throw IllegalStateException("valueCreateResponse is null")
        assertEquals(prepared.valueId, propertyValue.id)
        assertEquals(ScalarTypeTag.DOMAIN_ELEMENT.name, propertyValue.typeTag)
        assertEquals(rbiValue, propertyValue.repr)
        assertEquals(null, propertyValue.aspectPropertyId)
        assertEquals(null, propertyValue.aspectPropertyName)
        assertEquals(null, propertyValue.precision)
        assertEquals(null, propertyValue.measureName)

        // проверяем изменения
        checkValueCreateChanges(
            state.changes,
            mapOf("typeTag" to ScalarTypeTag.DOMAIN_ELEMENT.name, "refValueDomainElement" to rbiValue, "cardinality" to PropertyCardinality.ONE.name),
            prepared.propertyRequest.name
        )
    }

    private fun checkValueCreateChanges(changes: List<FieldDelta>, expected: Map<String, String>, propertyName: String?) {
        fun valueKey(propertyPrefix: String?, tag: String) = "$propertyPrefix:$tag"

        assertEquals(expected.size, changes.size)
        val byField = changes.groupBy { it.fieldName }
        assertEquals(expected.keys.map { valueKey(propertyName, it) }.toSet(), byField.keys)
        expected.forEach { key, value ->
            assertEquals(value, byField.getValue(valueKey(propertyName, key))[0].after)
        }
        assertEquals(changes.map { "" }, changes.map { it.before })
    }

    @Test
    fun createValueWithMeasureHistoryTest() {
        val testName = "createValueWithMeasureHistoryTest"
        val value = "123.23"
        val valueRepr = "1.2323E+5"
        val measure = measureService.findMeasure(Kilometre.name) ?: throw IllegalStateException("Not found measure")
        val objectDescription = "object description"

        val prepared = prepareNewValue(
            objectName = testName, objectDescription = objectDescription, value = ObjectValueData.DecimalValue.single(value),
            aspectId = complexAspect.idStrict(), measureId = measure.name
        )
        val valueFacts = prepared.valueFacts

        assertEquals(1, valueFacts.size, "exactly one object property event must appear")
        val valueEvent = valueFacts.first().event
        val valuePayload = valueFacts.first().payload
        assertEquals(OBJECT_PROPERTY_VALUE_CLASS, valueEvent.entityClass, "class must be correct")
        assertEquals(EventType.CREATE, valueEvent.type, "event type must be correct")

        assertEquals(setOf("typeTag", "decimalValue", "guid"), valuePayload.data.keys, "data keys must be correct")
        assertEquals(emptySet(), valuePayload.removedLinks.keys, "there must be no removed links")
        assertEquals(setOf("objectProperty", "measure"), valuePayload.addedLinks.keys, "added links keys must be correct")

        assertEquals(ScalarTypeTag.DECIMAL.name, valuePayload.data["typeTag"], "type tag must be correct")
        assertEquals(valueRepr, valuePayload.data["decimalValue"], "valueCreateResponse must be correct")

        val propertyLinks = valuePayload.addedLinks["objectProperty"] ?: fail("unexpected absence of property links")
        assertEquals(1, propertyLinks.size, "only 1 property must be here")
        assertEquals(prepared.propertyId, propertyLinks.first().toString(), "property id must be correct")

        val measureLinks = valuePayload.addedLinks["measure"] ?: fail("unexpected absence of measure links")
        assertEquals(1, measureLinks.size, "only 1 measure must be here")
        assertEquals(measure.id, measureLinks.first().toString(), "measure id must be correct")

        checkPropertyFacts(prepared, PropertyCardinality.INFINITY.toString())

        // ровно одно новое состояние
        assertEquals(1, prepared.states.size, "History must contain 1 element about ref book")
        val state = prepared.states[0]

        // проверяем мета-данные
        assertEquals(username, state.event.username)
        assertEquals(Math.max(valueEvent.timestamp, prepared.propertyFacts.first().event.timestamp), state.event.timestamp)
        assertEquals(EventType.UPDATE, state.event.type)
        assertEquals(HISTORY_ENTITY_OBJECT, state.event.entityClass)
        assertEquals(false, state.deleted)
        assertEquals(testName, state.info)

        // проверяем объект
        assertEquals(prepared.objectCreateResponse.id, state.fullData.objekt.id)
        assertEquals(testName, state.fullData.objekt.name)
        assertEquals(objectDescription, state.fullData.objekt.description)
        assertEquals(subject.id, state.fullData.objekt.subjectId)
        assertEquals(subject.name, state.fullData.objekt.subjectName)

        // проверяем свойство
        assertNotNull(state.fullData.property)
        val property = state.fullData.property ?: throw IllegalStateException("property is null")
        assertEquals(prepared.propertyId, property.id)
        assertEquals(prepared.propertyRequest.name, property.name)
        assertEquals(PropertyCardinality.INFINITY.name, property.cardinality)
        assertEquals(complexAspect.id, property.aspectId)
        assertEquals(complexAspect.name, property.aspectName)

        // проверяем значение
        assertNotNull(state.fullData.value)
        val propertyValue = state.fullData.value ?: throw IllegalStateException("valueCreateResponse is null")
        assertEquals(prepared.valueId, propertyValue.id)
        assertEquals(ScalarTypeTag.DECIMAL.name, propertyValue.typeTag)
        assertEquals(valueRepr, propertyValue.repr)
        assertEquals(null, propertyValue.aspectPropertyId)
        assertEquals(null, propertyValue.aspectPropertyName)
        assertEquals(measure.name, propertyValue.measureName)
        assertEquals(null, propertyValue.precision)

        val valueGuid = transaction(db) { db.getVertexById(prepared.valueId)?.toObjectPropertyValueVertex()?.guid ?: throw IllegalStateException() }

        // проверяем изменения
        checkValueCreateChanges(
            state.changes,
            mapOf("typeTag" to ScalarTypeTag.DECIMAL.name, "decimalValue" to valueRepr, "measure" to measure.name, "guid" to valueGuid),
            prepared.propertyRequest.name
        )
    }

    private fun eventsByClass(events: List<HistoryFact>, entityClass: String) =
        events.filter { it.event.entityClass == entityClass }

    private fun objectEvents(events: List<HistoryFact>) = eventsByClass(events, OBJECT_CLASS)

    private fun subjectEvents(events: List<HistoryFact>) = eventsByClass(events, SUBJECT_CLASS)

    private fun propertyEvents(events: List<HistoryFact>) = eventsByClass(events, OBJECT_PROPERTY_CLASS)

    private fun valueEvents(events: List<HistoryFact>) = eventsByClass(events, OBJECT_PROPERTY_VALUE_CLASS)

    private fun createObject(name: String, description: String = "obj descr"): ObjectChangeResponse {
        val request = ObjectCreateRequest(name, description, subject.id)
        return objectService.create(request, "user")
    }
}