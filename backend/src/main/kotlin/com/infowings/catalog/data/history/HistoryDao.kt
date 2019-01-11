package com.infowings.catalog.data.history

import com.infowings.catalog.auth.user.HISTORY_USER_EDGE
import com.infowings.catalog.external.logTime
import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.id
import com.infowings.catalog.storage.toVertexOrNull
import com.infowings.catalog.storage.transaction
import com.orientechnologies.orient.core.id.ORID
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.record.OVertex
import com.orientechnologies.orient.core.sql.executor.OResultInternal

class SqlBuilder() {
    private enum class Query(val ext: String) {
        SELECT("select")
    }

    private var query: Query? = null
    private var source: String? = null
    private var order: String? = null
    private var done: Boolean = false
    private var conditions: List<String> = emptyList()

    fun select(): SqlBuilder {
        if (query != null) throw IllegalStateException("query is already specified: $query")

        query = Query.SELECT

        return this
    }

    fun from(source: String): SqlBuilder {
        if (this.source != null) throw IllegalStateException("source: ${this.source}")

        this.source = source

        return this
    }

    fun sortedBy(field: String): SqlBuilder {
        if (this.order != null) throw IllegalStateException("source: ${this.order}")

        this.order = field

        return this
    }

    fun where(condition: String): SqlBuilder {
        if (this.conditions.isNotEmpty()) throw IllegalStateException("conditions: ${this.order}")

        this.conditions += condition

        return this
    }

    fun build(): String {
        if (done) throw IllegalStateException("can't call build twice")
        val currQuery = query
        if (currQuery == null) throw IllegalStateException("query is not defined")
        val currOrder = order

        val wherePart = if (conditions.isEmpty()) "" else "where ${conditions.joinToString(" and ", "(", ")")}"

        done = true

        return "${currQuery.ext} from $source $wherePart ${currOrder?.let { "order by ${it} "} ?: ""}"
    }
}



const val HISTORY_CLASS = "History"
private val selectFromHistory = SqlBuilder().select().from(HISTORY_EVENT_CLASS).build()
private val selectFromHistoryTS = SqlBuilder().select().from(HISTORY_EVENT_CLASS).sortedBy("timestamp").build()

private val logger = loggerFor<HistoryDao>()

class HistoryDao(private val db: OrientDatabase) {
    fun getVertex(id: String): OVertex? = db.getVertexById(id)

    fun findEvent(id: String) = transaction(db) { getVertex(id)?.toHistoryEventVertex() }

    fun newHistoryEventVertex() = db.createNewVertex(HISTORY_EVENT_CLASS).toHistoryEventVertex()

    fun newHistoryElementVertex() = db.createNewVertex(HISTORY_ELEMENT_CLASS).toHistoryElementVertex()

    fun newAddLinkVertex() = db.createNewVertex(HISTORY_ADD_LINK_CLASS).toHistoryLinksVertex()

    fun newDropLinkVertex() = db.createNewVertex(HISTORY_DROP_LINK_CLASS).toHistoryLinksVertex()

    fun getAllHistoryEvents() = db.query(selectFromHistory) { rs ->
        rs.mapNotNull { it.toVertexOrNull()?.toHistoryEventVertex() }.toList()
    }

    fun getAllHistoryEventsByTime(): List<HistoryEventVertex> = db.query(selectFromHistoryTS) { rs ->
        rs.mapNotNull { it.toVertexOrNull()?.toHistoryEventVertex() }.toList()
    }

    fun timelineForEntity(id: String): List<HistoryEventVertex> = timelineForEntity(ORecordId(id))

    fun timelineForEntity(id: ORID): List<HistoryEventVertex> {
        val q = SqlBuilder().select().from(HISTORY_EVENT_CLASS).where("entityRID == :id").sortedBy("timestamp").build()
        return db.query(q, mapOf("id" to id)) { rs ->
            rs.mapNotNull { it.toVertexOrNull()?.toHistoryEventVertex() }.toList()
        }
    }

    fun getAllHistoryEventsByTime(entityClass: String) = db.query(
        "SELECT FROM $HISTORY_EVENT_CLASS where entityClass = :cls ORDER BY timestamp",
        mapOf("cls" to entityClass)
    ) { rs ->
        rs.mapNotNull { it.toVertexOrNull()?.toHistoryEventVertex() }.toList()
    }

    fun getAllHistoryEventsByTime(entityClasses: List<String>) = db.query(
        "SELECT FROM $HISTORY_EVENT_CLASS where entityClass in :classes ORDER BY timestamp",
        mapOf("classes" to entityClasses)
    ) { rs ->
        rs.mapNotNull { it.toVertexOrNull()?.toHistoryEventVertex() }.toList()
    }

    fun getAllWithoutTimestampDate(): List<String> = db.query(
        "SELECT FROM $HISTORY_EVENT_CLASS where timestampDate is null"
    ) { rs ->
        rs.mapNotNull { it.toVertexOrNull()?.toHistoryEventVertex() }.toList().map { it.id }
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
                }.groupBy { it.first }.mapValues { it.value.map { it.second } }

                val dropped = it.getProperty<List<OResultInternal>>(aliasDropped).map { link ->
                    link.getProperty<String>(aliasKey) to link.getProperty<ORID>(aliasPeer)
                }.groupBy { it.first }.mapValues { it.value.map { it.second } }

                eventId.toString() to Pair(
                    it.getProperty<List<String>>(aliasUser).firstOrNull() ?: "",
                    DiffPayload(data = data, addedLinks = added, removedLinks = dropped)
                )
            }.toMap()
        }
    }
}