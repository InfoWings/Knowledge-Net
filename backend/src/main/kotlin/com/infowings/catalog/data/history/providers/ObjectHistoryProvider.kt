package com.infowings.catalog.data.history.providers

import com.infowings.catalog.common.Delta
import com.infowings.catalog.common.EventType
import com.infowings.catalog.common.ObjectHistory
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.history.HistoryFactDto
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.data.history.MutableSnapshot
import com.infowings.catalog.data.history.ObjectHistoryInfo
import com.infowings.catalog.storage.OBJECT_CLASS
import com.infowings.catalog.storage.OBJECT_PROPERTY_CLASS
import com.infowings.catalog.storage.OBJECT_PROPERTY_VALUE_CLASS

const val HISTORY_ENTITY_OBJECT = "Object"

private data class ObjectState(
    val objects: MutableMap<String, ObjectHistoryInfo.Companion.Objekt>,
    val versions: MutableMap<String, Int>,
    val objectIds: MutableMap<String, String>,
    val propertyIds: MutableMap<String, String>,
    val properties: MutableMap<String, ObjectHistoryInfo.Companion.Property>,
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
    private val subjectService: SubjectService
) {
    /* Метод для трансформации дельты для фронта.
     * Если какую-то дельту отдавать не хочется, можно преобразовать в пустой список.
     * Если к существующей дельте хочется добавить новую, можно вернуть ее как часть списка
     * Если надо вернуть как есть, упаковываем в одноэлементный список */
    private fun transformDelta(delta: Delta): List<Delta> = when {
        delta.field == "value" -> listOf(delta.copy(field = "Name"))
        delta.field == "link_subject" -> {
            val after = delta.after
            if (after != null) {
                listOf(
                    delta.copy(
                        field = "Subject",
                        after = subjectService.findById(after)?.name ?: "???"
                    )
                )
            } else {
                listOf(delta)
            }
        }
        else -> listOf(delta)
    }


    private fun objectCreate(createFact: HistoryFactDto, state: ObjectState): ObjectHistory {
        val objectId = createFact.event.entityId.toString()
        val initial = MutableSnapshot(mutableMapOf(), mutableMapOf())
        val subjectId = createFact.payload.addedLinks.getValue("subject")[0].toString()
        val subjectName = subjectService.findById(subjectId)?.name ?: "???"

        initial.apply(createFact.payload)

        val objekt = ObjectHistoryInfo.Companion.Objekt(
            id = objectId, snapshot = initial, subjectName = subjectName
        )

        state.objects[objectId] = objekt
        state.versions[objectId] = createFact.event.version

        val dataDeltas = createFact.payload.data.map { (key, value) -> Delta(key, "", value) }
        val linkDeltas = createFact.payload.addedLinks.map { (key, ids) ->
            Delta(
                "link_$key", "",
                ids.joinToString(separator = ":") { it.toString() }
            )
        }

        return ObjectHistory(
            username = "",
            eventType = EventType.CREATE,
            entityName = "",
            info = objekt.snapshot.data.getValue("name"),
            deleted = false,
            timestamp = createFact.event.timestamp,
            version = createFact.event.version,
            fullData = ObjectHistoryInfo.Companion.BriefState(objekt, null, null).toData(),
            changes = dataDeltas + linkDeltas
        )

    }

    private fun propertyAdd(createFact: HistoryFactDto, state: ObjectState): ObjectHistory {
        val propertyId = createFact.event.entityId.toString()
        val objectId = createFact.payload.addedLinks.getValue("object")[0].toString()
        val objekt = state.objects.getValue(objectId)
        val initial = MutableSnapshot(mutableMapOf(), mutableMapOf())
        val aspectId = createFact.payload.addedLinks.getValue("aspect")[0].toString()
        val aspectName = subjectService.findById(aspectId)?.name ?: "???"

        initial.apply(createFact.payload)

        val newProperty = ObjectHistoryInfo.Companion.Property(
            id = propertyId, snapshot = initial, aspectName = aspectName
        )

        val dataDeltas = createFact.payload.data.map { (key, value) -> Delta(key, "", value) }
        val linkDeltas = createFact.payload.addedLinks.map { (key, ids) ->
            Delta(
                "link_$key", "",
                ids.joinToString(separator = ":") { it.toString() }
            )
        }

        state.objectIds[propertyId] = objectId
        state.properties[propertyId] = newProperty

        return ObjectHistory(
            username = "",
            eventType = EventType.UPDATE,
            entityName = "",
            info = objekt.snapshot.data.getValue("name"),
            deleted = false,
            timestamp = -1,
            version = state.versions.getValue(objectId),
            fullData = ObjectHistoryInfo.Companion.BriefState(objekt, newProperty, null).toData(),
            changes = dataDeltas + linkDeltas
        )
    }

    private fun valueAdd(createFact: HistoryFactDto, state: ObjectState): ObjectHistory {
        val valueId = createFact.event.entityId.toString()
        val propertyId = createFact.payload.addedLinks.getValue("objectProperty")[0].toString()
        val objectId = state.objectIds.getValue(propertyId)
        val objekt = state.objects.getValue(objectId)
        val property = state.properties.getValue(propertyId)
        val (aspectPropertyId, aspectPropertyName) = if (createFact.payload.addedLinks.contains("aspectProperty")) {
            val id = createFact.payload.addedLinks.getValue("aspectProperty").first().toString()
            val name = aspectService.findPropertyById(id)?.name ?: "???"
            Pair(id, name)
        } else Pair(null, null)

        val initial = MutableSnapshot(mutableMapOf(), mutableMapOf())

        initial.apply(createFact.payload)

        val newValue = ObjectHistoryInfo.Companion.Value(
            id = propertyId, snapshot = initial, aspectPropertyName = aspectPropertyName
        )

        val dataDeltas = createFact.payload.data.map { (key, value) -> Delta(key, "", value) }
        val linkDeltas = createFact.payload.addedLinks.map { (key, ids) ->
            Delta(
                "link_$key", "",
                ids.joinToString(separator = ":") { it.toString() }
            )
        }

        state.propertyIds[valueId] = propertyId
        state.values[valueId] = newValue

        return ObjectHistory(
            username = "",
            eventType = EventType.UPDATE,
            entityName = "",
            info = objekt.snapshot.data.getValue("name"),
            deleted = false,
            timestamp = -1,
            version = state.versions.getValue(objectId),
            fullData = ObjectHistoryInfo.Companion.BriefState(objekt, property, newValue).toData(),
            changes = dataDeltas + linkDeltas
        )
    }

    private fun sessionToChange(sessionFacts: List<HistoryFactDto>, state: ObjectState): ObjectHistory {
        val byType = sessionFacts.groupBy { it.event.type }
        val createFacts = byType[EventType.CREATE]

        if (createFacts != null) {
            val createFact = createFacts.first()

            return when (createFact.event.entityClass) {
                OBJECT_CLASS -> {
                    objectCreate(createFact, state)
                }
                OBJECT_PROPERTY_CLASS -> {
                    propertyAdd(createFact, state)
                }
                OBJECT_PROPERTY_VALUE_CLASS -> {
                    valueAdd(createFact, state)
                }
                else -> throw IllegalStateException("Unexpected set of session facts: $sessionFacts")
            }
        } else {
            throw IllegalStateException("Unexpected set of session facts: $sessionFacts")
        }
    }

    private val objectVertices = setOf(OBJECT_CLASS, OBJECT_PROPERTY_CLASS, OBJECT_PROPERTY_VALUE_CLASS)

    fun getAllHistory(): List<ObjectHistory> {
        val allHistory = historyService.allTimeline()

        val objectFacts = allHistory.filter { objectVertices.contains(it.event.entityClass) }

        val factsBySession = objectFacts.groupBy { it.sessionId }

        val historyState = ObjectState()

        return factsBySession.values.map { sessionFacts ->
            val ch = sessionToChange(sessionFacts, historyState)
            val timestamps = sessionFacts.map { it.event.timestamp }
            val sessionTimestamp = timestamps.max() ?: throw IllegalStateException("no facts in session")
            ch.copy(username = sessionFacts.first().event.username,
                timestamp = sessionTimestamp,
                entityName = HISTORY_ENTITY_OBJECT,
                changes = ch.changes.flatMap { transformDelta(it) })
        }.reversed()
    }
}