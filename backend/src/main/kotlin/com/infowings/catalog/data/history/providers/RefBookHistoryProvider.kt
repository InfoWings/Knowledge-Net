package com.infowings.catalog.data.history.providers


import com.infowings.catalog.common.Delta
import com.infowings.catalog.common.EventType
import com.infowings.catalog.common.RefBookHistory
import com.infowings.catalog.data.aspect.AspectDaoService
import com.infowings.catalog.data.aspect.toAspectVertex
import com.infowings.catalog.data.history.HistoryFactDto
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.data.history.MutableSnapshot
import com.infowings.catalog.data.history.RefBookHistoryInfo
import com.infowings.catalog.data.reference.book.REFERENCE_BOOK_ITEM_VERTEX

const val HISTORY_ENTITY_REFBOOK = "Reference Book"

private data class RefBookState(
    val headers: MutableMap<String, RefBookHistoryInfo.Companion.Header>,
    val versions: MutableMap<String, Int>,
    val rootIds: MutableMap<String, String>,
    val items: MutableMap<String, RefBookHistoryInfo.Companion.Item>
) {
    constructor() : this(
        headers = mutableMapOf<String, RefBookHistoryInfo.Companion.Header>(),
        versions = mutableMapOf<String, Int>(), rootIds = mutableMapOf<String, String>(),
        items = mutableMapOf<String, RefBookHistoryInfo.Companion.Item>()
    )
}

class RefBookHistoryProvider(
    private val historyService: HistoryService,
    private val aspectDao: AspectDaoService
) {
    /* Метод для трансформации дельты для фронта.
     * Если какую-то дельту отдавать не хочется, можно преобразовать в пустой список.
     * Если к существующей дельте хочется добавить новую, можно вернуть ее как часть списка
     * Если надо вернуть как есть, упаковываем в одноэлементный список */
    private fun transformDelta(delta: Delta): List<Delta> = when {
        delta.field == "value" -> listOf(delta.copy(field = "Name"))
        delta.field == "link_aspect" -> {
            val after = delta.after
            if (after != null) {
                listOf(
                    delta.copy(
                        field = "Aspect",
                        after = aspectDao.getVertex(after)?.toAspectVertex()?.name ?: "???"
                    )
                )
            } else {
                listOf(delta)
            }
        }
        else -> listOf(delta)
    }

    private fun refBookCreation(createFact: HistoryFactDto, state: RefBookState): RefBookHistory {
        val refBookId = createFact.event.entityId.toString()
        val initial = MutableSnapshot(mutableMapOf(), mutableMapOf())
        val aspectId = createFact.payload.addedLinks.getValue("aspect")[0].toString()
        val aspectName = aspectDao.getAspectVertex(aspectId)?.name ?: "???"

        initial.apply(createFact.payload)

        val header = RefBookHistoryInfo.Companion.Header(
            id = refBookId, snapshot = initial,
            aspectName = aspectName
        )

        state.headers[refBookId] = header
        state.versions[refBookId] = createFact.event.version

        val dataDeltas = createFact.payload.data.map { (key, value) -> Delta(key, "", value) }
        val linkDeltas = createFact.payload.addedLinks.map { (key, ids) ->
            Delta(
                "link_$key", "",
                ids.joinToString(separator = ":") { it.toString() }
            )
        }

        return RefBookHistory(
            username = "",
            eventType = EventType.CREATE,
            entityName = HISTORY_ENTITY_REFBOOK,
            info = header.snapshot.data.getValue("value"),
            deleted = false,
            timestamp = createFact.event.timestamp,
            version = createFact.event.version,
            fullData = RefBookHistoryInfo.Companion.BriefState(header, null).toData(),
            changes = (dataDeltas + linkDeltas).flatMap { transformDelta(it) }
        )

    }

    private fun refBookItemInsertion(createFact: HistoryFactDto, state: RefBookState): RefBookHistory {
        val itemId = createFact.event.entityId.toString()
        val rootId = createFact.payload.addedLinks.getValue("root")[0].toString()
        val header = state.headers.getValue(rootId)
        val initial = MutableSnapshot(mutableMapOf(), mutableMapOf())

        initial.apply(createFact.payload)

        val newItem = RefBookHistoryInfo.Companion.Item(
            id = itemId, snapshot = initial
        )

        val dataDeltas = createFact.payload.data.map { (key, value) -> Delta(key, "", value) }

        state.rootIds[itemId] = rootId
        state.items[itemId] = newItem

        return RefBookHistory(
            username = "",
            eventType = EventType.UPDATE,
            entityName = HISTORY_ENTITY_REFBOOK,
            info = header.snapshot.data.getValue("value"),
            deleted = false,
            timestamp = -1,
            version = state.versions.getValue(rootId),
            fullData = RefBookHistoryInfo.Companion.BriefState(header, newItem).toData(),
            changes = dataDeltas
        )
    }

    private fun refBookItemEdit(updateFact: HistoryFactDto, state: RefBookState): RefBookHistory {
        val itemId = updateFact.event.entityId.toString()
        val rootId = state.rootIds.getValue(itemId)
        val header = state.headers.getValue(rootId)

        val item = state.items.getValue(itemId)

        val prev = item.snapshot.toSnapshot()

        item.snapshot.apply(updateFact.payload)

        val dataDeltas = updateFact.payload.data.map { (key, value) -> Delta(key, prev.data[key], value) }

        return RefBookHistory(
            username = "",
            eventType = EventType.UPDATE,
            entityName = HISTORY_ENTITY_REFBOOK,
            info = header.snapshot.data.getValue("value"),
            deleted = false,
            timestamp = -1,
            version = state.versions.getValue(rootId),
            fullData = RefBookHistoryInfo.Companion.BriefState(header, item).toData(),
            changes = dataDeltas
        )
    }

    private fun refBookRootEdit(updateFact: HistoryFactDto, state: RefBookState): RefBookHistory {
        val itemId = updateFact.event.entityId.toString()
        val header = state.headers.getValue(itemId)
        val prev = header.snapshot.toSnapshot()

        header.snapshot.apply(updateFact.payload)

        val dataDeltas = updateFact.payload.data.map { (key, value) -> Delta(key, prev.data[key], value) }

        return RefBookHistory(
            username = "",
            eventType = EventType.UPDATE,
            entityName = HISTORY_ENTITY_REFBOOK,
            info = header.snapshot.data.getValue("value"),
            deleted = false,
            timestamp = -1,
            version = state.versions.getValue(itemId),
            fullData = RefBookHistoryInfo.Companion.BriefState(header, null).toData(),
            changes = dataDeltas
        )
    }

    private fun sessionToChange(sessionFacts: List<HistoryFactDto>, state: RefBookState): RefBookHistory {
        val byType = sessionFacts.groupBy { it.event.type }
        val createFacts = byType[EventType.CREATE]
        val updateFacts = byType[EventType.UPDATE]

        return when {
            createFacts != null && createFacts.size == 1 -> {
                val createFact = createFacts.first()
                if (sessionFacts.size == 1) {
                    refBookCreation(createFact, state)
                } else {
                    refBookItemInsertion(createFact, state)
                }
            }
            updateFacts != null && sessionFacts.size == 1 -> {
                val updateFact = updateFacts.first()
                val itemId = updateFact.event.entityId.toString()

                if (!state.rootIds.contains(itemId)) {
                    refBookRootEdit(updateFact, state)
                } else {
                    refBookItemEdit(updateFact, state)
                }
            }
            else -> throw IllegalStateException("Unexpected set of session facts: $sessionFacts")
        }
    }

    fun getAllHistory(): List<RefBookHistory> {
        val allHistory = historyService.allTimeline()

        val rbFacts = allHistory.filter { it.event.entityClass == REFERENCE_BOOK_ITEM_VERTEX }

        val factsBySession = rbFacts.groupBy { it.sessionId }

        val historyState = RefBookState()

        return factsBySession.values.map { sessionFacts ->
            val ch = sessionToChange(sessionFacts, historyState)
            val timestamps = sessionFacts.map { it.event.timestamp }
            val sessionTimestamp = timestamps.max() ?: throw IllegalStateException("no facts in session")
            ch.copy(username = sessionFacts.first().event.username,
                timestamp = sessionTimestamp,
                changes = ch.changes.flatMap { transformDelta(it) })
        }.reversed()
    }
}