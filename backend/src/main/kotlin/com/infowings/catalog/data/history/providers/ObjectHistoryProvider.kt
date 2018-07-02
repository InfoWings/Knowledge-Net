package com.infowings.catalog.data.history.providers

import com.infowings.catalog.common.EventType
import com.infowings.catalog.common.FieldDelta
import com.infowings.catalog.common.HistoryEventData
import com.infowings.catalog.common.ObjectHistory
import com.infowings.catalog.common.history.objekt.ObjectHistoryData
import com.infowings.catalog.data.MeasureService
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectDaoService
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.history.HistoryFact
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.data.history.MutableSnapshot
import com.infowings.catalog.data.history.ObjectHistoryInfo
import com.infowings.catalog.data.reference.book.ReferenceBookService
import com.infowings.catalog.external.logTime
import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.OBJECT_CLASS
import com.infowings.catalog.storage.OBJECT_PROPERTY_CLASS
import com.infowings.catalog.storage.OBJECT_PROPERTY_VALUE_CLASS
import com.infowings.catalog.storage.id

const val HISTORY_ENTITY_OBJECT = "Object"

private data class ObjectState(
    // известные объекты по id
    val objects: MutableMap<String, ObjectHistoryInfo.Companion.Objekt>,
    // версии объектов по id
    val versions: MutableMap<String, Int>,
    // id объектов по id пропертей
    val objectIds: MutableMap<String, String>,
    // id пропертей по id значений
    val propertyIds: MutableMap<String, String>,
    // известные проперти по id
    val properties: MutableMap<String, ObjectHistoryInfo.Companion.Property>,
    // известные значения по id
    val values: MutableMap<String, ObjectHistoryInfo.Companion.Value>
) {
    constructor() : this(
        objects = mutableMapOf<String, ObjectHistoryInfo.Companion.Objekt>(),
        versions = mutableMapOf<String, Int>(),
        objectIds = mutableMapOf<String, String>(),
        propertyIds = mutableMapOf<String, String>(),
        properties = mutableMapOf<String, ObjectHistoryInfo.Companion.Property>(),
        values = mutableMapOf<String, ObjectHistoryInfo.Companion.Value>()
    )
}

class ObjectHistoryProvider(
    private val historyService: HistoryService,
    private val aspectService: AspectService,
    private val aspectDao: AspectDaoService,
    private val subjectService: SubjectService,
    private val refBookService: ReferenceBookService,
    private val measureService: MeasureService
) {
    /* Метод для трансформации дельты для фронта.
     * Если какую-то дельту отдавать не хочется, можно преобразовать в пустой список.
     * Если к существующей дельте хочется добавить новую, можно вернуть ее как часть списка
     * Если надо вернуть как есть, упаковываем в одноэлементный список */
    private fun transformDelta(delta: FieldDelta, state: ObjectHistoryData.Companion.BriefState): List<FieldDelta> {

        fun substAfter(source: FieldDelta, name: String, value: String?): FieldDelta {
            /* Вспомогательная функция, подставляющая новые значения вместо fieldName/after в тех случаях,
             * когда в исходной дельте after - не null (для создания/редактирования) */
            val after = source.after
            return if (after != null) {
                source.copy(fieldName = name, after = value ?: "???")
            } else {
                source
            }
        }

        val fieldSuffix = delta.fieldName.dropPrefix("link_")

        val linksProcessed = if (fieldSuffix != null) {
            if (idReprExtractors.containsKey(fieldSuffix)) {
                listOf(substAfter(delta, fieldSuffix, idReprExtractors.getValue(fieldSuffix)(state)))
            } else {
                emptyList()
            }
        } else {
            listOf(delta)
        }

        return if (state.value == null) linksProcessed else linksProcessed.map {
            it.copy(fieldName = (state.property?.name ?: "<UNDEFINED>") + ":" + it.fieldName)
        }
    }


    private fun objectCreate(createFact: HistoryFact, state: ObjectState): ObjectHistory {
        val objectId = createFact.event.entityId
        val initial = MutableSnapshot(mutableMapOf(), mutableMapOf())
        val subjectId = createFact.payload.addedLinks.getValue("subject")[0].toString()
        val subjectName = subjectService.findById(subjectId)?.name ?: "???"

        initial.apply(createFact.payload)

        val objekt = ObjectHistoryInfo.Companion.Objekt(
            id = objectId, snapshot = initial, subjectName = subjectName
        )

        state.objects[objectId] = objekt
        state.versions[objectId] = createFact.event.version

        val dataDeltas = createFact.payload.data.map { (key, value) -> FieldDelta(key, "", value) }
        val linkDeltas = createFact.payload.addedLinks.map { (key, ids) ->
            FieldDelta("link_$key", "", ids.joinToString(separator = ":") { it.toString() })
        }

        val event = HistoryEventData(
            username = "", type = EventType.CREATE, entityClass = "", timestamp = createFact.event.timestamp,
            version = createFact.event.version, entityId = createFact.event.entityId, sessionId = ""
        )

        return ObjectHistory(
            event = event,
            info = objekt.snapshot.data.getValue("name"),
            deleted = false,
            fullData = ObjectHistoryInfo.Companion.BriefState(objekt, null, null).toData(),
            changes = dataDeltas + linkDeltas
        )
    }

    private fun propertyAdd(createFact: HistoryFact, state: ObjectState, aspectNameById: Map<String, String>): ObjectHistory {
        val propertyId = createFact.event.entityId
        val objectId = createFact.payload.addedLinks.getValue("object")[0].toString()
        val objekt = state.objects.getValue(objectId)
        val initial = MutableSnapshot(mutableMapOf(), mutableMapOf())
        val aspectId = createFact.payload.addedLinks.getValue("aspect")[0].toString()
        val aspectName = aspectNameById[aspectId] ?: "???"

        initial.apply(createFact.payload)

        val newProperty = ObjectHistoryInfo.Companion.Property(
            id = propertyId, snapshot = initial, aspectName = aspectName
        )

        val dataDeltas = createFact.payload.data.map { (key, value) -> FieldDelta(key, "", value) }
        val linkDeltas = createFact.payload.addedLinks.filter { it.key != "object" }.map { (key, ids) ->
            FieldDelta(
                "link_$key", "",
                ids.joinToString(separator = ":") { it.toString() }
            )
        }

        state.objectIds[propertyId] = objectId
        state.properties[propertyId] = newProperty

        val event = HistoryEventData(
            username = "", type = EventType.UPDATE, entityClass = "", timestamp = -1,
            version = state.versions.getValue(objectId), entityId = createFact.event.entityId, sessionId = ""
        )

        return ObjectHistory(
            event = event,
            info = objekt.snapshot.data.getValue("name"),
            deleted = false,
            fullData = ObjectHistoryInfo.Companion.BriefState(objekt, newProperty, null).toData(),
            changes = dataDeltas + linkDeltas
        )
    }

    private fun valueAdd(createFact: HistoryFact, state: ObjectState): ObjectHistory {
        val valueId = createFact.event.entityId
        val propertyId = createFact.payload.addedLinks.getValue("objectProperty")[0].toString()
        val objectId = state.objectIds.getValue(propertyId)
        val objekt = state.objects.getValue(objectId)
        val property = state.properties.getValue(propertyId)

        fun nameById(key: String, getter: (String) -> String?): String? =
            createFact.payload.addedLinks[key]?.first()?.toString()?.let { getter(it) ?: "???" }

        val aspectPropertyName = nameById("aspectProperty") {
            aspectService.findPropertyById(it)?.name
        }
        val subjectName = nameById("refValueSubject") {
            subjectService.findById(it)?.name
        }
        val objectName = nameById("refValueObject") {
            state.objects[it]?.snapshot?.data?.get("name")
        }
        val domainElement = nameById("refValueDomainElement") {
            refBookService.itemName(it)
        }
        val measureName = nameById("measure") {
            measureService.name(it)
        }

        val initial = MutableSnapshot(mutableMapOf(), mutableMapOf())

        initial.apply(createFact.payload)

        val newValue = ObjectHistoryInfo.Companion.Value(
            id = valueId, snapshot = initial,
            subjectName = subjectName,
            objectName = objectName,
            domainElement = domainElement,
            measureName = measureName,
            aspectPropertyName = aspectPropertyName
        )

        val dataDeltas = createFact.payload.data.map { (key, value) -> FieldDelta(key, "", value) }
        val linkDeltas = createFact.payload.addedLinks.filter { it.key != "objectProperty" }.map { (key, ids) ->
            FieldDelta(
                "link_$key", "",
                ids.joinToString(separator = ":") { it.toString() }
            )
        }

        state.propertyIds[valueId] = propertyId
        state.values[valueId] = newValue

        val event = HistoryEventData(
            username = "", type = EventType.UPDATE, entityClass = "", timestamp = -1,
            version = state.versions.getValue(objectId), entityId = createFact.event.entityId, sessionId = ""
        )

        return ObjectHistory(
            event = event,
            info = objekt.snapshot.data.getValue("name"),
            deleted = false,
            fullData = ObjectHistoryInfo.Companion.BriefState(objekt, property, newValue).toData(),
            changes = dataDeltas + linkDeltas
        )
    }

    private fun placeHolder(fact: HistoryFact) =
        ObjectHistory(event = fact.event, info = "<UNSUPPORTED>", deleted = false,
            fullData = ObjectHistoryData.Companion.BriefState(
                ObjectHistoryData.Companion.Objekt(id = "<UNSUPPORTED>", name = "<UNSUPPORTED>", description = null, subjectId = "", subjectName = ""), null, null), changes = emptyList())

    private fun sessionToChange(sessionFacts: List<HistoryFact>, state: ObjectState, aspectNameById: Map<String, String>): ObjectHistory {
        val byType = sessionFacts.groupBy { it.event.type }
        val createFacts = byType[EventType.CREATE]

        return if (createFacts != null) {
            val createFact = createFacts.first()

            when (createFact.event.entityClass) {
                OBJECT_CLASS -> {
                    objectCreate(createFact, state)
                }
                OBJECT_PROPERTY_CLASS -> {
                    propertyAdd(createFact, state, aspectNameById)
                }
                OBJECT_PROPERTY_VALUE_CLASS -> {
                    valueAdd(createFact, state)
                }
                else ->
                    placeHolder(createFact)
            }
        } else {
            val fact = sessionFacts.first()

            placeHolder(fact)
        }
    }

    private val objectVertices = setOf(OBJECT_CLASS, OBJECT_PROPERTY_CLASS, OBJECT_PROPERTY_VALUE_CLASS)

    fun getAllHistory(): List<ObjectHistory> {
        val objectFacts = logTime(com.infowings.catalog.data.history.providers.logger, "extracting new timeline for object history") {
            historyService.allTimeline(objectVertices.toList())
        }

        val factsByEntity = objectFacts.groupBy { it.event.entityClass }
        val propertyFacts = factsByEntity[OBJECT_PROPERTY_CLASS].orEmpty()

        val aspectLinks = propertyFacts.map { it.payload.mentionedLinks("aspect") }.flatten().toSet()
        logger.info("aspect links: $aspectLinks")

        val aspectNames = aspectDao.findAspectsByIds(aspectLinks.toList()).groupBy{it.id}.mapValues{ it.value.first().name }
        logger.info("aspect names: $aspectNames")

        val factsBySession = objectFacts.groupBy { it.event.sessionId }

        val historyState = ObjectState()

        return factsBySession.map { (sessionId, sessionFacts) ->
            println("session facts: ")
            val ch: ObjectHistory = sessionToChange(sessionFacts, historyState, aspectNames)
            val timestamps = sessionFacts.map { it.event.timestamp }
            val sessionTimestamp = timestamps.max() ?: throw IllegalStateException("no facts in session")
            val newEvent = ch.event.copy(
                username = sessionFacts.first().event.username,
                timestamp = sessionTimestamp,
                entityClass = HISTORY_ENTITY_OBJECT, sessionId = sessionId
            )
            ch.copy(event = newEvent,
                changes = ch.changes.flatMap { transformDelta(it, ch.fullData) })
        }.reversed()
    }
}

private val idReprExtractors: Map<String, (ObjectHistoryData.Companion.BriefState) -> String?> = mapOf(
    "subject" to { currentState -> currentState.objekt.subjectName },
    "refValueSubject" to { currentState -> currentState.value?.repr },
    "refValueObject" to { currentState -> currentState.value?.repr },
    "refValueDomainElement" to { currentState -> currentState.value?.repr },
    "aspect" to { currentState -> currentState.property?.aspectName },
    "aspectProperty" to { currentState -> currentState.value?.aspectPropertyName },
    "measure" to { currentState -> currentState.value?.measureName }
)

private fun String.dropPrefix(p: String): String? {
    return if (startsWith(p)) drop(p.length) else null
}

private val logger = loggerFor<ObjectHistoryProvider>()
