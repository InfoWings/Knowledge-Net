package com.infowings.catalog.data.history

import com.infowings.catalog.storage.OrientDatabase


class HistoryService(private val db: OrientDatabase,
                     private val historyDaoService: HistoryDaoService) {
    fun storeEvent(event: HistoryEvent) {
        historyDaoService.saveEvent(event)
    }
}