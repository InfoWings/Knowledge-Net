package com.infowings.catalog.data.history

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

private val logger = loggerFor<HistoryDao>()

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

    fun getPayloads(ids: List<ORID>): Map<String, DiffPayload> = logTime(logger, "facts extraction at dao level") {
            val aliasPropIds = "propertyIds"
            val aliasSubjects = "subjectIds"
            val aliasRefBookNames = "refBookNames"
            val aliasId = "id"
            val aliasFields = "fields"
            val aliasAdded = "addedLinks"
            val aliasDropped = "droppedLinks"
            val aliasPropertiesTime = "propTime"
            val aliasVersion = "version"

            db.query(
                "select" +
                        " @rid as $aliasId," +
                        " out('$HISTORY_ELEMENT_EDGE'):{key, value} as $aliasFields," +
                        " out('$HISTORY_ADD_LINK_EDGE'):{key, peerId} as $aliasAdded, " +
                        " out('$HISTORY_DROP_LINK_EDGE'):{key, peerId} as $aliasDropped " +
                        "  from  :ids ", mapOf("ids" to ids)
            ) { rs ->
                rs.mapNotNull {
                    it.toVertexOrNull()
                    val eventId = it.getProperty<ORID>(aliasId)
                    val addedLinks = it.getProperty<List<OResultInternal>>(aliasAdded)
                    val droppedLinks = it.getProperty<List<OResultInternal>>(aliasAdded)

                    val data: Map<String, String> = it.getProperty<List<OResultInternal>>(aliasFields).map {
                        it.getProperty<String>("key") to it.getProperty<String>("value")
                    }.toMap()

                    val added = it.getProperty<List<OResultInternal>>(aliasAdded).map { link ->
                        logger.info("added: ${link.propertyNames}")
                        link.propertyNames.forEach {
                            val v = link.getProperty<Any>(it)
                            logger.info("prop name: $it")
                            logger.info("prop value: $v")
                            logger.info("prop value class: ${v?.javaClass}")
                        }
                        "it.getProperty<String>(\"key\")" to "it.getProperty<String>(\"value\")"
                    }.toMap()

                    eventId.toString() to DiffPayload(data = data, addedLinks = emptyMap(), removedLinks = emptyMap())
                }.toMap()
            }
    }
}