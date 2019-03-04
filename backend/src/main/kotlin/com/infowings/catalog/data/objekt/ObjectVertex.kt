package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.GuidAware
import com.infowings.catalog.data.history.HistoryAware
import com.infowings.catalog.data.history.Snapshot
import com.infowings.catalog.data.history.asStringOrEmpty
import com.infowings.catalog.data.subject.SubjectVertex
import com.infowings.catalog.data.subject.toSubjectVertex
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OVertex

fun OVertex.toObjectVertex(): ObjectVertex {
    checkClass(OrientClass.OBJECT)
    return ObjectVertex(this)
}

class ObjectVertex(private val vertex: OVertex) : HistoryAware, GuidAware, DeletableVertex, OVertex by vertex {
    override val entityClass = OBJECT_CLASS

    override fun currentSnapshot(): Snapshot = Snapshot(
        data = mapOf(
            "name" to asStringOrEmpty(name),
            "description" to asStringOrEmpty(description),
            "guid" to asStringOrEmpty(guid)
        ),
        links = mapOf(
            "subject" to listOfNotNull(subject?.identity),
            "properties" to properties.map { it.identity }
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

    override val guid: String = vertex[ATTR_GUID]

    val subject: SubjectVertex?
        get() = vertex.getVertices(ODirection.OUT, OBJECT_SUBJECT_EDGE).firstOrNull()?.toSubjectVertex()

    val properties: List<ObjectPropertyVertex>
        get() = vertex.getVertices(ODirection.IN, OBJECT_OBJECT_PROPERTY_EDGE)
            .map { it.toObjectPropertyVertex() }.filterNot { it.deleted }

    val lastUpdated: Long?
        get() = 1111 // vertex.getVertices(ODirection.IN, OBJECT_SUBJECT_EDGE).map { it.toObjectVertex() }

    fun toObjekt(): Objekt {
        val currentSubject = subject ?: throw IllegalStateException("Object $id has no subject")

        return Objekt(identity, name, description, currentSubject, properties, guid, lastUpdated)
    }
}
