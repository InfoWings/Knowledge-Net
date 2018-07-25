package com.infowings.catalog.data.history

import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.id.ORID
import com.orientechnologies.orient.core.record.OVertex
import com.orientechnologies.orient.core.record.impl.OVertexDocument

fun OVertex.toHistoryLinksVertex(): HistoryLinksVertex {
    checkClassAny(listOf(OrientClass.HISTORY_ADD_LINK, OrientClass.HISTORY_REMOVE_LINK))
    return HistoryLinksVertex(this)
}

const val HISTORY_ADD_LINK_CLASS = "HistoryAddLink"
const val HISTORY_ADD_LINK_EDGE = "HistoryAddLinkEdge"
const val HISTORY_DROP_LINK_CLASS = "HistoryDropLink"
const val HISTORY_DROP_LINK_EDGE = "HistoryDropLinkEdge"


class HistoryLinksVertex(private val vertex: OVertex) : OVertex by vertex {
    override fun equals(other: Any?): Boolean {
        return vertex == other
    }

    override fun hashCode(): Int {
        return vertex.hashCode()
    }

    var eventId: ORID
        get() = vertex.getProperty("eventId")
        set(value) {
            vertex["eventId"] = value
        }

    var key: String
        get() = vertex["key"]
        set(value) {
            vertex["key"] = value
        }

    var peerId: ORID
        get() {
            val property = vertex.getProperty<Any>("peerId")
            return if (property is OVertexDocument)
                property.identity
            else
                property as ORID
        }
        set(value) {
            vertex["peerId"] = value
        }
}