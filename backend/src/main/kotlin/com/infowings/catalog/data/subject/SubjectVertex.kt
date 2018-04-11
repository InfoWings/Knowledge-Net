package com.infowings.catalog.data.subject

import com.infowings.catalog.data.history.HistoryAware
import com.infowings.catalog.data.history.Snapshot
import com.infowings.catalog.data.history.asStringOrEmpty
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.OVertex

fun OVertex.toSubjectVertex() = SubjectVertex(this)

class SubjectVertex(private val vertex: OVertex) : HistoryAware, OVertex by vertex {
    override val entityClass = SUBJECT_CLASS

    override fun currentSnapshot(): Snapshot = Snapshot(
        data = mapOf(
            "value" to asStringOrEmpty(name),
            "description" to asStringOrEmpty(description)
        ),
        links = emptyMap()
    )

    var name: String
        get() = vertex[ATTR_NAME]
        set(value) {
            vertex[ATTR_NAME] = value
        }

    var description: String?
        get() = vertex[ATTR_DESC]
        set(value) {
            vertex[ATTR_DESC] = value
        }
}