package com.infowings.catalog.data.history

import com.infowings.catalog.storage.get
import com.infowings.catalog.storage.set
import com.orientechnologies.orient.core.id.ORID
import com.orientechnologies.orient.core.record.OVertex

fun OVertex.toHistoryLinksVertex() = HistoryLinksVertex(this)
const val HISTORY_ADD_LINK_CLASS = "HistoryAddLink"
const val HISTORY_DROP_LINK_CLASS = "HistoryDropLink"

class HistoryLinksVertex(private val vertex: OVertex) : OVertex by vertex {
    override fun equals(other: Any?): Boolean {
        return vertex == other
    }

    override fun hashCode(): Int {
        return vertex.hashCode()
    }

    var eventId: ORID
        get() = vertex["eventId"]
        set(value) {
            vertex["eventId"] = value
        }

    var key: String
        get() = vertex["key"]
        set(value) {
            vertex["key"] = value
        }

    var peerId: String
        get() = vertex["peerId"]
        set(value) {
            vertex["peerId"] = value
        }
}