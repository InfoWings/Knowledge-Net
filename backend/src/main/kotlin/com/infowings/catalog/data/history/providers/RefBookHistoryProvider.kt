package com.infowings.catalog.data.history.providers


import com.infowings.catalog.common.*
import com.infowings.catalog.common.history.refbook.RefBookHistoryData
import com.infowings.catalog.data.aspect.AspectDaoService
import com.infowings.catalog.data.aspect.toAspectVertex
import com.infowings.catalog.data.history.HistoryService
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

        val lastSnapshots = mutableMapOf<String, RefBookHistoryData.Companion.Header>()
        val lastVersions = mutableMapOf<String, Int>()

        val res = grb.values.map { sessionFacts ->

            val createFact = sessionFacts.find { it.event.type == EventType.CREATE }

            val users = sessionFacts.map { it.event.username }.toSet()
            if (users.size != 1) throw IllegalStateException("Exactly one user must be specified")

            val ts = sessionFacts.map { it.event.timestamp }

            if (createFact != null) {
                if (sessionFacts.size == 1) {
                    val fact = sessionFacts[0]
                    val refBookId = fact.event.entityId.toString()
                    val aspectId = fact.payload.addedLinks.getValue("aspect")[0].toString()
                    val aspectName = aspectDao.getAspectVertex(aspectId)?.name ?: "???"

                    val header = RefBookHistoryData.Companion.Header(
                        id = refBookId, name = fact.payload.data.getValue("value"),
                        description = fact.payload.data["description"],
                        aspectId = fact.payload.addedLinks.getValue("aspect")[0].toString(),
                        aspectName = aspectName
                    )

                    lastSnapshots[refBookId] = header
                    lastVersions[refBookId] = fact.event.version

                    val q1 = fact.payload.data.map { (key, value) -> Delta(key, "", value) }
                    val q2 = fact.payload.addedLinks.map { (key, ids) ->
                        Delta(
                            "link_$key", "",
                            ids.joinToString(separator = ":") { it.toString() }
                        )
                    }

                    RefBookHistory(
                        username = users.first(),
                        eventType = EventType.CREATE,
                        entityName = HISTORY_ENTITY_REFBOOK,
                        info = header.name,
                        deleted = false,
                        timestamp = fact.event.timestamp,
                        version = fact.event.version,
                        fullData = RefBookHistoryData.Companion.BriefState(header, null),
                        changes = (q1 + q2).flatMap { transformDelta(it) }
                    )
                } else {
                    val itemId = createFact.event.entityId.toString()
                    val rootId = createFact.payload.addedLinks.getValue("root")[0].toString()
                    val header = lastSnapshots.getValue(rootId)

                    val newItem = RefBookHistoryData.Companion.Item(
                        id = itemId, name = createFact.payload.data.getValue("value"),
                        description = createFact.payload.data["description"]
                    )

                    val q1 = createFact.payload.data.map { (key, value) -> Delta(key, "", value) }


                    RefBookHistory(
                        username = users.first(),
                        eventType = EventType.UPDATE,
                        entityName = HISTORY_ENTITY_REFBOOK,
                        info = header.name,
                        deleted = false,
                        timestamp = ts.max() ?: 0,
                        version = lastVersions.getValue(rootId),
                        fullData = RefBookHistoryData.Companion.BriefState(header, newItem),
                        changes = q1
                    )
                }
            } else {
                throw IllegalStateException("Sessions without creations are not supported yet")
            }
        }

        return res.reversed()
    }
}