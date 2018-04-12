package com.infowings.catalog.data.subject

import com.infowings.catalog.data.aspect.toAspectVertex
import com.infowings.catalog.data.history.HistoryAware
import com.infowings.catalog.data.history.Snapshot
import com.infowings.catalog.data.history.asStringOrEmpty
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.OVertex
import incomingEdges

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
        set(value) { vertex[ATTR_NAME] = value
        }

    var description: String?
        get() = vertex[ATTR_DESC]
        set(value) {
            vertex[ATTR_DESC] = value
        }

    var deleted: Boolean
        get() = vertex["deleted"] ?: false
        set(value) {
            vertex["deleted"] = value
        }

    fun linkedByAspects() = incomingEdges(ASPECT_SUBJECT_EDGE).map {
        val source = it.from.asVertex()
        val vertex = if (source.isPresent) source.get() else
            throw InternalError("Source of $ASPECT_SUBJECT_EDGE is not vertex")
        val classOpt = vertex.schemaType
        val vertexClass = if (classOpt.isPresent) classOpt.get() else
            throw InternalError("Source of $ASPECT_SUBJECT_EDGE has no type")

        val aspectVertex = if (vertexClass.name == ASPECT_CLASS) {
            vertex.toAspectVertex()
        } else throw InternalError("Source of $ASPECT_SUBJECT_EDGE is not aspect vertex")

        aspectVertex
    }.filterNot {it.deleted}
}