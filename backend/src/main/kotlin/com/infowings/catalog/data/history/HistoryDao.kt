package com.infowings.catalog.data.history

import com.infowings.catalog.auth.user.HISTORY_USER_EDGE
import com.infowings.catalog.common.HistoryEventData
import com.infowings.catalog.data.aspect.AspectDaoService
import com.infowings.catalog.external.logTime
import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.toVertexOrNull
import com.orientechnologies.orient.core.id.ORID
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.executor.OResultInternal
import javax.validation.Payload

const val HISTORY_CLASS = "History"
const val selectFromHistory = "SELECT FROM $HISTORY_EVENT_CLASS"
const val selectFromHistoryTS = "SELECT FROM $HISTORY_EVENT_CLASS ORDER BY timestamp"

private val logger =   loggerFor<HistoryDao>()

class HistoryDao(private val db: OrientDatabase) {
    fun newHistoryEventVertex() = db.createNewVertex(HISTORY_EVENT_CLASS).toHistoryEventVertex()

    fun newHistoryElementVertex() = db.createNewVertex(HISTORY_ELEMENT_CLASS).toHistoryElementVertex()

    fun newAddLinkVertex() = db.createNewVertex(HISTORY_ADD_LINK_CLASS).toHistoryLinksVertex()

    fun newDropLinkVertex() = db.createNewVertex(HISTORY_DROP_LINK_CLASS).toHistoryLinksVertex()

    fun getAllHistoryEvents() = db.query(selectFromHistory) { rs ->
        rs.mapNotNull { it.toVertexOrNull()?.toHistoryEventVertex() }.toList()
    }

    fun getAllHistoryEventsByTime() = db.query(selectFromHistoryTS) { rs ->
        rs.mapNotNull { it.toVertexOrNull()?.toHistoryEventVertex() }.toList()
    }

    fun getAllHistoryEventsByTime(entityClass: String) = db.query("SELECT FROM $HISTORY_EVENT_CLASS where entityClass = :cls ORDER BY timestamp",
        mapOf("cls" to entityClass)) { rs ->
        rs.mapNotNull { it.toVertexOrNull()?.toHistoryEventVertex() }.toList()
    }

    fun getAllHistoryEventsByTime(entityClasses: List<String>) = db.query("SELECT FROM $HISTORY_EVENT_CLASS where entityClass in :classes ORDER BY timestamp",
        mapOf("classes" to entityClasses)) { rs ->
        rs.mapNotNull { it.toVertexOrNull()?.toHistoryEventVertex() }.toList()
    }

    fun getPayloadsAndUsers(ids: List<ORID>): Map<String, Pair<String, DiffPayload>> = logTime(logger, "facts extraction at dao level") {
            val aliasKey = "key"
            val aliasValue = "value"
            val aliasPeer = "peerId"
            val aliasId = "id"
            val aliasFields = "fields"
            val aliasAdded = "addedLinks"
            val aliasDropped = "droppedLinks"
            val aliasUser = "user"

            db.query(
                "select" +
                        " @rid as $aliasId," +
                        " in('$HISTORY_USER_EDGE').username as $aliasUser," +
                        " out('$HISTORY_ELEMENT_EDGE'):{$aliasKey, $aliasValue} as $aliasFields," +
                        " out('$HISTORY_ADD_LINK_EDGE'):{$aliasKey, $aliasPeer} as $aliasAdded, " +
                        " out('$HISTORY_DROP_LINK_EDGE'):{$aliasKey, $aliasPeer} as $aliasDropped " +
                        "  from  :ids ", mapOf("ids" to ids)
            ) { rs ->
                rs.mapNotNull {
                    it.toVertexOrNull()
                    val eventId = it.getProperty<ORID>(aliasId)

                    val data: Map<String, String> = it.getProperty<List<OResultInternal>>(aliasFields).map {
                        it.getProperty<String>(aliasKey) to it.getProperty<String>(aliasValue)
                    }.toMap()

                    val added = it.getProperty<List<OResultInternal>>(aliasAdded).map { link ->
                        link.getProperty<String>(aliasKey) to link.getProperty<ORID>(aliasPeer)
                    }.groupBy { it.first } .mapValues { it.value.map { it.second } }

                    val dropped = it.getProperty<List<OResultInternal>>(aliasDropped).map { link ->
                        link.getProperty<String>(aliasKey) to link.getProperty<ORID>(aliasPeer)
                    }.groupBy { it.first } .mapValues { it.value.map { it.second } }

                    eventId.toString() to Pair(it.getProperty<List<String>>(aliasUser).firstOrNull() ?: "", DiffPayload(data = data, addedLinks = added, removedLinks = dropped))
                }.toMap()
            }
    }
}