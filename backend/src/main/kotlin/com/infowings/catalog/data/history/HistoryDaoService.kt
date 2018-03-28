package com.infowings.catalog.data.history

import com.infowings.catalog.data.aspect.HISTORY_EVENT_CLASS
import com.infowings.catalog.data.aspect.toAspectVertex
import com.infowings.catalog.data.aspect.toHistoryEventVertex
import com.infowings.catalog.storage.ASPECT_CLASS
import com.infowings.catalog.storage.OrientDatabase
import java.sql.Timestamp

const val HISTORY_CLASS = "History"

const val historyInsert = "INSERT into ${HISTORY_CLASS} (class, user, entityRID, name, event, data, timestamp, version)" +
        " VALUES (?, ?, ?, ?, ?, ?, ?, ?) "

class HistoryDaoService(private val db: OrientDatabase) {
    fun newHistoryEventVertex() = db.createNewVertex(HISTORY_EVENT_CLASS).toHistoryEventVertex()

    fun saveEvent(event: HistoryEvent) =
            db.command(historyInsert,
                    event.keys.entityClass, event.user, event.keys.entityId,
                    event.keys.entityName, event.payload.event, event.payload.serialize(),
                    Timestamp(event.timestamp), event.version) {
                it
            }
}