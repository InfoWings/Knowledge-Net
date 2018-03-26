package com.infowings.catalog.data.history

import com.infowings.catalog.storage.OrientDatabase

const val HISTORY_CLASS = "History"

const val historyInsert = "INSERT into ${HISTORY_CLASS} (cls, user, rid, event, data) VALUES (?, ?, ?, ?, ?) "

class HistoryDaoService(private val db: OrientDatabase) {
    fun saveEvent(event: HistoryEvent) =
            db.command(historyInsert,
                    event.cls, event.user, event.rid, event.event, event.data.toString()) {
                it
            }
}