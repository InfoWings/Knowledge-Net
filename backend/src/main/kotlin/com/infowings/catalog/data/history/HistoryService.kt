package com.infowings.catalog.data.history

import com.infowings.catalog.storage.OrientDatabase
import com.orientechnologies.orient.core.id.ORID
import java.sql.Timestamp


class HistoryService(private val db: OrientDatabase, private val historyDaoService: HistoryDaoService) {

    fun storeFact(fact: HistoryFact) {
        val historyEventVertex = fact.newHistoryEventVertex()

        val elementVertices = fact.payload.data.map {
            val elementVertex = historyDaoService.newHistoryElementVertex()
            elementVertex.addEdge(historyEventVertex)
            elementVertex.key = it.key
            elementVertex.stringValue = it.value

            elementVertex
        }

        val addLinkVertices = historyEventVertex.linksVertices(fact.payload.addedLinks) {
            historyDaoService.newAddLinkVertex()
        }
        val dropLinkVertices = historyEventVertex.linksVertices(fact.payload.removedLinks) {
            historyDaoService.newDropLinkVertex()
        }

        db.saveAll(listOf(historyEventVertex) + elementVertices + addLinkVertices + dropLinkVertices)
    }

    private fun HistoryFact.newHistoryEventVertex(): HistoryEventVertex {
        val historyEventVertex = historyDaoService.newHistoryEventVertex()

        historyEventVertex.entityClass = event.entityClass
        historyEventVertex.entityId = event.entityId
        historyEventVertex.entityVersion = event.version
        historyEventVertex.timestamp = Timestamp(event.timestamp)
        historyEventVertex.user = event.user

        return historyEventVertex
    }

    private fun HistoryEventVertex.linksVertices(
        linksPayload: Map<String, List<ORID>>,
        vertexProducer: () -> HistoryLinksVertex): List<HistoryLinksVertex> {

        return linksPayload.flatMap {
            val key = it.key
            it.value.map {
                val v = vertexProducer()
                v.addEdge(this)
                v.key = key
                v.peerId = it
                v
            }
        }
    }

}