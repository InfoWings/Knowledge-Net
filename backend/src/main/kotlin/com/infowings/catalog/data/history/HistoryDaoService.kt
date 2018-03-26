package com.infowings.catalog.data.history

import com.infowings.catalog.storage.OrientDatabase

const val HISTORY_CLASS = "History"

const val historyInsert = "INSERT into ${HISTORY_CLASS} (class, user, rid, name, event, data) VALUES (?, ?, ?, ?, ?, ?) "

class HistoryDaoService(private val db: OrientDatabase) {
    fun saveEvent(event: HistoryEvent) =
            db.command(historyInsert,
                    event.keys.entityClass, event.user, event.keys.entityId, event.keys.entityName,
                    event.payload.event, event.payload.serialize()) {
                it
            }
}