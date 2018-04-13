package com.infowings.catalog.data.history

import com.infowings.catalog.storage.get
import com.infowings.catalog.storage.set
import com.orientechnologies.orient.core.id.ORID
import com.orientechnologies.orient.core.record.OVertex
import com.orientechnologies.orient.core.record.impl.OVertexDocument
import java.time.Instant

const val HISTORY_EVENT_CLASS = "HistoryEvent"


fun OVertex.toHistoryEventVertex() = HistoryEventVertex(this)

class HistoryEventVertex(private val vertex: OVertex) : OVertex by vertex {
    override fun equals(other: Any?): Boolean {
        return vertex == other
    }

    override fun hashCode(): Int {
        return vertex.hashCode()
    }

    var entityRID: ORID
        get() = (vertex.getProperty("entityRID") as OVertexDocument).identity.identity
        set(value) {
            vertex["entityRID"] = value
        }

    var entityClass: String
        get() = vertex["entityClass"]
        set(value) {
            vertex["entityClass"] = value
        }

    var user: String
        get() = vertex["user"]
        set(value) {
            vertex["user"] = value
        }

    var timestamp: Instant
        get() = vertex["timestamp"]
        set(value) {
            vertex["timestamp"] = value
        }

    var entityVersion: Int
        get() = vertex["entityVersion"]
        set(value) {
            vertex["entityVersion"] = value
        }

    var eventType: String
        get() = vertex["eventType"]
        set(value) {
            vertex["eventType"] = value
        }
}