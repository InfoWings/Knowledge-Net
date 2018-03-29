package com.infowings.catalog.data.history

import com.infowings.catalog.storage.OrientDatabase
import com.orientechnologies.orient.core.id.ORID
import java.sql.Timestamp


class HistoryService(private val db: OrientDatabase, private val historyDaoService: HistoryDaoService) {
    fun storeFact(fact: HistoryFact) {
        val historyEventVertex = historyDaoService.newHistoryEventVertex()

        historyEventVertex.entityClass = fact.event.entityClass
        historyEventVertex.entityId = fact.event.entityId
        historyEventVertex.entityVersion = fact.event.version
        historyEventVertex.timestamp = Timestamp(fact.event.timestamp)
        historyEventVertex.user = fact.event.user

        val elementVertices = fact.payload.data.map {
            val elementVertex = historyDaoService.newHistoryElementVertex()
            elementVertex.eventId = historyEventVertex.identity
            elementVertex.key = it.key
            elementVertex.stringValue = it.value

            elementVertex
        }

        fun linksVertices(linksPayload: Map<String, List<ORID>>, vertexProducer: () -> HistoryLinksVertex) =
            linksPayload.flatMap {
                val key = it.key
                it.value.map {
                    val elementVertex = vertexProducer()
                    elementVertex.eventId = historyEventVertex.identity
                    elementVertex.key = key
                    elementVertex.peerId = it
                    elementVertex
                }
            }

        val addLinkVertices = linksVertices(fact.payload.addedLinks, {
            historyDaoService.newAddLinkVertex()
        })
        val dropLinkVertices = linksVertices(fact.payload.removedLinks, {
            historyDaoService.newDropLinkVertex()
        })

        db.saveAll(listOf(historyEventVertex) + elementVertices + addLinkVertices + dropLinkVertices)
    }
}