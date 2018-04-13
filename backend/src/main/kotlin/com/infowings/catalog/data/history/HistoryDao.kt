package com.infowings.catalog.data.history

import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.toVertexOrNull

const val HISTORY_CLASS = "History"
const val selectFromHistory = "SELECT FROM $HISTORY_EVENT_CLASS"

class HistoryDao(private val db: OrientDatabase) {
    fun newHistoryEventVertex() = db.createNewVertex(HISTORY_EVENT_CLASS).toHistoryEventVertex()

    fun newHistoryElementVertex() = db.createNewVertex(HISTORY_ELEMENT_CLASS).toHistoryElementVertex()

    fun newAddLinkVertex() = db.createNewVertex(HISTORY_ADD_LINK_CLASS).toHistoryLinksVertex()

    fun newDropLinkVertex() = db.createNewVertex(HISTORY_DROP_LINK_CLASS).toHistoryLinksVertex()

    fun getAllHistoryEvents() = db.query(selectFromHistory) { rs ->
        rs.mapNotNull { it.toVertexOrNull()?.toHistoryEventVertex() }.toSet()
    }
}