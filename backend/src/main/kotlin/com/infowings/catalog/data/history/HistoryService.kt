package com.infowings.catalog.data.history

import com.infowings.catalog.auth.user.HISTORY_USER_EDGE
import com.infowings.catalog.auth.user.UserVertex
import com.infowings.catalog.common.EventType
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.transaction
import com.orientechnologies.orient.core.id.ORID
import com.orientechnologies.orient.core.record.ODirection
import java.time.Instant

class HistoryService(
    private val db: OrientDatabase,
    private val historyDao: HistoryDao
) {

    fun getAll(): Set<HistoryFact> = transaction(db) {
        return@transaction historyDao.getAllHistoryEvents()
            .map {
                val event = HistoryEvent(
                    it.userVertex.username,
                    it.timestamp.toEpochMilli(),
                    it.entityVersion,
                    EventType.valueOf(it.eventType),
                    it.entityRID,
                    it.entityClass,
                    it.sessionId
                )

                val data = it.getVertices(ODirection.OUT, HISTORY_ELEMENT_EDGE)
                    .map { it.toHistoryElementVertex() }
                    .map { it.key to it.stringValue }
                    .toMap()

                val addedLinks = it.getVertices(ODirection.OUT, HISTORY_ADD_LINK_EDGE)
                    .map { it.toHistoryLinksVertex() }
                    .groupBy { it.key }
                    .map { it.key to it.value.map { it.peerId } }
                    .toMap()

                val removedLinks = it.getVertices(ODirection.OUT, HISTORY_DROP_LINK_EDGE)
                    .map { it.toHistoryLinksVertex() }
                    .groupBy { it.key }
                    .map { it.key to it.value.map { it.peerId } }
                    .toMap()

                val payload = DiffPayload(data, addedLinks, removedLinks)

                return@map HistoryFact(event, payload)
            }
            .toSet()
    }

    fun storeFact(fact: HistoryFactWrite): HistoryEventVertex = transaction(db) {
        val historyEventVertex = fact.newHistoryEventVertex()

        val elementVertices = fact.payload.data.map {
            return@map historyDao.newHistoryElementVertex().apply {
                key = it.key
                stringValue = it.value
            }
        }
        elementVertices.forEach { historyEventVertex.addEdge(it, HISTORY_ELEMENT_EDGE) }

        val addLinkVertices = linksVertices(fact.payload.addedLinks, historyDao.newAddLinkVertex())
        addLinkVertices.forEach { historyEventVertex.addEdge(it, HISTORY_ADD_LINK_EDGE) }

        val dropLinkVertices = linksVertices(fact.payload.removedLinks, historyDao.newDropLinkVertex())
        dropLinkVertices.forEach { historyEventVertex.addEdge(it, HISTORY_DROP_LINK_EDGE) }

        fact.event.userVertex.addEdge(historyEventVertex, HISTORY_USER_EDGE)

        fact.event.entityVertex.addEdge(historyEventVertex, HISTORY_EDGE)

        db.saveAll(listOf(historyEventVertex) + elementVertices + addLinkVertices + dropLinkVertices)

        return@transaction historyEventVertex
    }

    private fun HistoryFactWrite.newHistoryEventVertex(): HistoryEventVertex =
        historyDao.newHistoryEventVertex().apply {
            entityClass = event.entityClass
            entityRID = event.entityVertex.identity
            entityVersion = event.version
            timestamp = Instant.ofEpochMilli(event.timestamp)
            eventType = event.type.name
            sessionId = event.sessionId
        }

    private fun linksVertices(
        linksPayload: Map<String, List<ORID>>,
        linksVertex: HistoryLinksVertex
    ): List<HistoryLinksVertex> =
        linksPayload.flatMap { (linkKey, peerIds) ->
            peerIds.map { id ->
                linksVertex.apply {
                    key = linkKey
                    peerId = id
                }
            }
        }
}

data class HistoryContext(val userVertex: UserVertex)
