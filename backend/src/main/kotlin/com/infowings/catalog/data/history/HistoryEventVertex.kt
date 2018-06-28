package com.infowings.catalog.data.history

import com.infowings.catalog.auth.user.HISTORY_USER_EDGE
import com.infowings.catalog.auth.user.UserVertex
import com.infowings.catalog.auth.user.toUserVertex
import com.infowings.catalog.common.EventType
import com.infowings.catalog.common.HistoryEventData
import com.infowings.catalog.storage.get
import com.infowings.catalog.storage.set
import com.orientechnologies.orient.core.id.ORID
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OVertex
import com.orientechnologies.orient.core.record.impl.OVertexDocument
import java.time.Instant
import java.util.*

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
        get() {
            val property = vertex.getProperty<Any>("entityRID")
            return if (property is OVertexDocument)
                property.identity
            else
                property as ORID
        }
        set(value) {
            vertex["entityRID"] = value
        }

    var entityClass: String
        get() = vertex["entityClass"]
        set(value) {
            vertex["entityClass"] = value
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

    val userVertex: UserVertex
        get() = getVertices(ODirection.IN, HISTORY_USER_EDGE)
            .map { it.toUserVertex() }
            .first()

    var sessionId: UUID
        get() = UUID.fromString(vertex["sessionUUID"])
        set(value) {
            vertex["sessionUUID"] = value.toString()
        }

    private fun toEvent() = HistoryEventData(
        userVertex.username,
        timestamp.toEpochMilli(),
        entityVersion,
        EventType.valueOf(eventType),
        entityRID.toString(),
        entityClass,
        sessionId.toString()
    )

    private fun dataMap() = getVertices(ODirection.OUT, HISTORY_ELEMENT_EDGE).map { vertex ->
        val heVertex = vertex.toHistoryElementVertex()
        heVertex.key to heVertex.stringValue
    }.toMap()

    private fun addedLinks(): Map<String, List<ORID>> {
        return getVertices(ODirection.OUT, HISTORY_ADD_LINK_EDGE)
            .map { vertex -> vertex.toHistoryLinksVertex() }
            .groupBy { linksVertex -> linksVertex.key }
            .mapValues { (_, peers) -> peers.map { it.peerId } }
    }

    private fun removedLinks() = getVertices(ODirection.OUT, HISTORY_DROP_LINK_EDGE)
        .map { vertex -> vertex.toHistoryLinksVertex() }
        .groupBy { linksVertex -> linksVertex.key }
        .mapValues { (_, peers) -> peers.map { it.peerId } }


    fun toFact(): HistoryFact {
        val event = toEvent()
        val data = dataMap()
        val addedLinks = addedLinks()
        val removedLinks = removedLinks()
        val payload = DiffPayload(data, addedLinks, removedLinks)

        return HistoryFact(event, payload)
    }
}