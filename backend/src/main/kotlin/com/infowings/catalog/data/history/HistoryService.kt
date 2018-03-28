package com.infowings.catalog.data.history

import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.session
import com.orientechnologies.orient.core.record.OVertex
import java.sql.Timestamp


class HistoryService(private val db: OrientDatabase,
                     private val historyDaoService: HistoryDaoService) {
    private val logger = loggerFor<HistoryService>()

    fun storeEvent(event: HistoryEvent) {
        logger.info("event: $event")
        val historyEventVertex = historyDaoService.newHistoryEventVertex()

        historyEventVertex.entityClass = event.keys.entityClass
        historyEventVertex.entityId = event.keys.entityId
        historyEventVertex.entityVersion = event.version
        historyEventVertex.timestamp = Timestamp(event.timestamp)
        historyEventVertex.user = event.user

        logger.info("going to save history event ${historyEventVertex.propertyNames}")

        session(database = db) { session ->
            historyEventVertex.save<OVertex>()

            return@session
        }
        logger.info("save")

        historyDaoService.saveEvent(event)
    }
}