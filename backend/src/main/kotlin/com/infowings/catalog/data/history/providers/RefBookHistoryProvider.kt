package com.infowings.catalog.data.history.providers


import com.infowings.catalog.common.Delta
import com.infowings.catalog.common.EventType
import com.infowings.catalog.common.RefBookHistory
import com.infowings.catalog.data.aspect.AspectDaoService
import com.infowings.catalog.data.aspect.toAspectVertex
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.data.history.MutableSnapshot
import com.infowings.catalog.data.history.RefBookHistoryInfo
import com.infowings.catalog.data.reference.book.REFERENCE_BOOK_ITEM_VERTEX

const val HISTORY_ENTITY_REFBOOK = "Reference Book"


class RefBookHistoryProvider(
    private val historyService: HistoryService,
    private val aspectDao: AspectDaoService
) {
    private fun transformDelta(delta: Delta): List<Delta> {
        return if (delta.field == "value")
            listOf(delta.copy(field = "Name"))
        else if (delta.field == "link_aspect") {
            val after = delta.after
            if (after != null) {
                listOf(
                    delta,
                    delta.copy(
                        field = "Aspect",
                        after = aspectDao.getVertex(after)?.toAspectVertex()?.name ?: "???"
                    )
                )
            } else {
                listOf(delta)
            }


        } else listOf(delta)
    }

    fun getAllHistory(): List<RefBookHistory> {

        val allHistory = historyService.allTimeline()

        val rbFacts = allHistory.filter { it.event.entityClass == REFERENCE_BOOK_ITEM_VERTEX }

        val grb = rbFacts.groupBy { it.sessionId }

        val lastHeaders = mutableMapOf<String, RefBookHistoryInfo.Companion.Header>()
        val lastVersions = mutableMapOf<String, Int>()
        val lastRoots = mutableMapOf<String, String>()
        val lastItems = mutableMapOf<String, RefBookHistoryInfo.Companion.Item>()

        val res = grb.values.map { sessionFacts ->

            val createFact = sessionFacts.find { it.event.type == EventType.CREATE }

            val users = sessionFacts.map { it.event.username }.toSet()
            if (users.size != 1) throw IllegalStateException("Exactly one user must be specified")

            val ts = sessionFacts.map { it.event.timestamp }

            if (createFact != null) {
                if (sessionFacts.size == 1) {
                    val fact = sessionFacts[0]
                    val refBookId = fact.event.entityId.toString()
                    val initial = MutableSnapshot(mutableMapOf(), mutableMapOf())
                    val aspectId = fact.payload.addedLinks.getValue("aspect")[0].toString()
                    val aspectName = aspectDao.getAspectVertex(aspectId)?.name ?: "???"

                    initial.apply(fact.payload)

                    val header = RefBookHistoryInfo.Companion.Header(
                        id = refBookId, snapshot = initial,
                        aspectName = aspectName
                    )

                    lastHeaders[refBookId] = header
                    lastVersions[refBookId] = fact.event.version

                    val dataDeltas = fact.payload.data.map { (key, value) -> Delta(key, "", value) }
                    val linkDeltas = fact.payload.addedLinks.map { (key, ids) ->
                        Delta(
                            "link_$key", "",
                            ids.joinToString(separator = ":") { it.toString() }
                        )
                    }

                    RefBookHistory(
                        username = users.first(),
                        eventType = EventType.CREATE,
                        entityName = HISTORY_ENTITY_REFBOOK,
                        info = header.snapshot.data.getValue("value"),
                        deleted = false,
                        timestamp = fact.event.timestamp,
                        version = fact.event.version,
                        fullData = RefBookHistoryInfo.Companion.BriefState(header, null).toData(),
                        changes = (dataDeltas + linkDeltas).flatMap { transformDelta(it) }
                    )
                } else {
                    val itemId = createFact.event.entityId.toString()
                    val rootId = createFact.payload.addedLinks.getValue("root")[0].toString()
                    val header = lastHeaders.getValue(rootId)
                    val initial = MutableSnapshot(mutableMapOf(), mutableMapOf())

                    initial.apply(createFact.payload)

                    val newItem = RefBookHistoryInfo.Companion.Item(
                        id = itemId, snapshot = initial
                    )

                    val dataDeltas = createFact.payload.data.map { (key, value) -> Delta(key, "", value) }

                    lastRoots[itemId] = rootId
                    lastItems[itemId] = newItem

                    RefBookHistory(
                        username = users.first(),
                        eventType = EventType.UPDATE,
                        entityName = HISTORY_ENTITY_REFBOOK,
                        info = header.snapshot.data.getValue("value"),
                        deleted = false,
                        timestamp = ts.max() ?: 0,
                        version = lastVersions.getValue(rootId),
                        fullData = RefBookHistoryInfo.Companion.BriefState(header, newItem).toData(),
                        changes = dataDeltas
                    )
                }
            } else if (sessionFacts.size == 1 && sessionFacts.first().event.type == EventType.UPDATE) {
                val fact = sessionFacts.first()
                val itemId = fact.event.entityId.toString()
                val rootId = lastRoots.getValue(itemId)
                val header = lastHeaders.getValue(rootId)

                val item = lastItems.getValue(itemId)

                val prev = item.snapshot.toSnapshot()

                item.snapshot.apply(fact.payload)

                val dataDeltas = fact.payload.data.map { (key, value) -> Delta(key, prev.data[key], value) }

                RefBookHistory(
                    username = users.first(),
                    eventType = EventType.UPDATE,
                    entityName = HISTORY_ENTITY_REFBOOK,
                    info = header.snapshot.data.getValue("value"),
                    deleted = false,
                    timestamp = fact.event.timestamp,
                    version = lastVersions.getValue(rootId),
                    fullData = RefBookHistoryInfo.Companion.BriefState(header, item).toData(),
                    changes = dataDeltas
                )

            } else {

                throw IllegalStateException("Unexpected set of session facts: $sessionFacts")
            }
        }

        return res.reversed()
    }
}