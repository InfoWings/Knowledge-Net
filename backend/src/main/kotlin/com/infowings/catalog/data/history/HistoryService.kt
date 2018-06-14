package com.infowings.catalog.data.history

import com.infowings.catalog.auth.user.HISTORY_USER_EDGE
import com.infowings.catalog.auth.user.UserVertex
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.sessionStore
import com.infowings.catalog.storage.transaction
import com.orientechnologies.orient.core.id.ORID
import java.time.Instant
import java.util.*

class HistoryService(
    private val db: OrientDatabase,
    private val historyDao: HistoryDao
) {

    fun getAll(): Set<HistoryFactDto> = transaction(db) {
        return@transaction historyDao.getAllHistoryEvents()
            .map { it.toFact() }
            .toSet()
    }

    fun allTimeline(): List<HistoryFactDto> = transaction(db) {
        return@transaction historyDao.getAllHistoryEventsByTime()
            .map { it.toFact() }
    }

    fun storeFact(fact: HistoryFact): HistoryEventVertex = transaction(db) {
        val historyEventVertex = fact.newHistoryEventVertex()

        val elementVertices = fact.payload.data.map {
            return@map historyDao.newHistoryElementVertex().apply {
                key = it.key
                stringValue = it.value
            }
        }
        elementVertices.forEach { historyEventVertex.addEdge(it, HISTORY_ELEMENT_EDGE) }

        val addLinkVertices = linksVertices(fact.payload.addedLinks, { historyDao.newAddLinkVertex() })

        addLinkVertices.forEach { historyEventVertex.addEdge(it, HISTORY_ADD_LINK_EDGE) }

        val dropLinkVertices = linksVertices(fact.payload.removedLinks, { historyDao.newDropLinkVertex() })
        dropLinkVertices.forEach { historyEventVertex.addEdge(it, HISTORY_DROP_LINK_EDGE) }

        fact.userVertex.addEdge(historyEventVertex, HISTORY_USER_EDGE)

        fact.subject.addEdge(historyEventVertex, HISTORY_EDGE)

        db.saveAll(listOf(historyEventVertex) + elementVertices + addLinkVertices + dropLinkVertices)

        return@transaction historyEventVertex
    }

    fun <T> trackUpdate(entity: HistoryAware, context: HistoryContext, action: () -> T): T {
        val before = entity.currentSnapshot()

        val result = action()

        storeFact(entity.toUpdateFact(context, before))

        return result
    }

    fun <T> trackUpdates(entities: List<HistoryAware>, context: HistoryContext, action: () -> T): T {
        fun worker(current: Int): T = when {
            current == entities.size -> action()
            current < entities.size -> trackUpdate(entities[current], context, { worker(current + 1) })
            else -> throw IllegalStateException("incorrect index: $current")
        }

        return worker(0)
    }

    private fun HistoryFact.newHistoryEventVertex(): HistoryEventVertex =
        historyDao.newHistoryEventVertex().apply {
            entityClass = event.entityClass
            entityRID = event.entityId
            entityVersion = event.version
            timestamp = Instant.ofEpochMilli(event.timestamp)
            eventType = event.type.name
            sessionId = sessionStore.get().uuid
        }

    private fun linksVertices(
        linksPayload: Map<String, List<ORID>>,
        linksVertex: () -> HistoryLinksVertex
    ): List<HistoryLinksVertex> =
        linksPayload.flatMap { (linkKey, peerIds) ->
            peerIds.map { id ->
                linksVertex().apply {
                    key = linkKey
                    peerId = id
                }
            }
        }
}

// We can't use HistoryFact because vertex HistoryAware possible not exist
// TODO:redesign data structures to easily detach backend specific elements (like OVertex descendants) from ones reasonable for frontend
data class HistoryFactDto(val event: HistoryEvent, val sessionId: UUID, var payload: DiffPayload)

data class HistoryContext(val userVertex: UserVertex)
