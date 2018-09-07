package com.infowings.catalog.data.subject

import com.infowings.catalog.data.aspect.toAspectVertex
import com.infowings.catalog.data.guid.toGuidVertex
import com.infowings.catalog.data.history.HistoryAware
import com.infowings.catalog.data.history.Snapshot
import com.infowings.catalog.data.history.asStringOrEmpty
import com.infowings.catalog.data.objekt.ObjectVertex
import com.infowings.catalog.data.objekt.toObjectVertex
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OVertex
import incomingEdges

fun OVertex.toSubjectVertex(): SubjectVertex {
    checkClass(OrientClass.SUBJECT)
    return SubjectVertex(this)
}

class SubjectVertex(private val vertex: OVertex) : HistoryAware, OVertex by vertex {
    override val entityClass = SUBJECT_CLASS

    override fun currentSnapshot(): Snapshot = Snapshot(
        data = mapOf(
            "name" to asStringOrEmpty(name),
            "description" to asStringOrEmpty(description),
            "guid" to asStringOrEmpty(guid)
        ),
        links = mapOf(
            "objects" to objects.map { it.identity }
        )
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

    var deleted: Boolean
        get() = vertex["deleted"] ?: false
        set(value) {
            vertex["deleted"] = value
        }

    val objects: List<ObjectVertex>
        get() = vertex.getVertices(ODirection.IN, OBJECT_SUBJECT_EDGE).map { it.toObjectVertex() }

    val guid: String?
        get() = getVertices(ODirection.OUT, OrientEdge.GUID_OF_SUBJECT.extName).firstOrNull()?.toGuidVertex()?.guid

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
    }.filterNot { it.deleted }
}