package com.infowings.catalog.data.history

import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.toVertexOrNull

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

class HistoryDao(private val db: OrientDatabase) {
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

    fun timelineForEntity(id: String): List<HistoryEventVertex> {
        val q = SqlBuilder().select().from(HISTORY_EVENT_CLASS).where("@rid == :id").sortedBy("timestamp").build()
        return db.query(q, mapOf("id" to id)) { rs ->
            rs.mapNotNull { it.toVertexOrNull()?.toHistoryEventVertex() }.toList()
        }
    }

}