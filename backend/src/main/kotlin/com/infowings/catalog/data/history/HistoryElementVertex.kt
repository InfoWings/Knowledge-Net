package com.infowings.catalog.data.history

import com.infowings.catalog.storage.get
import com.infowings.catalog.storage.set
import com.orientechnologies.orient.core.id.ORID
import com.orientechnologies.orient.core.record.OVertex

fun OVertex.toHistoryElementVertex() = HistoryElementVertex(this)
const val HISTORY_ELEMENT_CLASS = "HistoryElement"

class HistoryElementVertex(private val vertex: OVertex) : OVertex by vertex {
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

    var stringValue: String
        get() = vertex["value"]
        set(value) {
            vertex["value"] = value
        }
}