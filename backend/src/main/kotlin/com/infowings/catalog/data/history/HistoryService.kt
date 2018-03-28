package com.infowings.catalog.data.history

import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.session
import com.orientechnologies.orient.core.record.OVertex
import java.sql.Timestamp


class HistoryService(private val db: OrientDatabase,
                     private val historyDaoService: HistoryDaoService) {
    private val logger = loggerFor<HistoryService>()

    fun storeEvent(fact: HistoryFact) {
        logger.info("fact: $fact")
        val historyEventVertex = historyDaoService.newHistoryEventVertex()

        historyEventVertex.entityClass = fact.event.entityClass
        historyEventVertex.entityId = fact.event.entityId
        historyEventVertex.entityVersion = fact.event.version
        historyEventVertex.timestamp = Timestamp(fact.event.timestamp)
        historyEventVertex.user = fact.event.user

        logger.info("going to save history event ${historyEventVertex.propertyNames}")

        val elementVertices = fact.payload.data.map {
            val elementVertex = historyDaoService.newHistoryElementVertex()
            elementVertex.eventId = historyEventVertex.identity
            elementVertex.key = it.key
            elementVertex.stringValue = it.value

            elementVertex
        }

        fun linksVertices(linksPayload: Map<String, List<String>>,
                          vertexProducer: () -> HistoryLinksVertex) = linksPayload.flatMap {
            val key = it.key
            it.value.map {
                val elementVertex = vertexProducer()
                elementVertex.eventId = historyEventVertex.identity
                elementVertex.key = key
                elementVertex.peerId = it

                elementVertex
            }
        }

        val addLinkVertices = linksVertices(fact.payload.addedLinks, {historyDaoService.newAddLinkVertex()})
        val dropLinkVertices = linksVertices(fact.payload.removedLinks, {historyDaoService.newDropLinkVertex()})

        session(database = db) { session ->
            historyEventVertex.save<OVertex>()
            for (e in elementVertices + addLinkVertices + dropLinkVertices) {
                e.save<OVertex>()
            }
        }
        logger.info("save")
    }
}