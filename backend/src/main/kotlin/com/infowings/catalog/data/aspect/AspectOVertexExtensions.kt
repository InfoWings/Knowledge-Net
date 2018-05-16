package com.infowings.catalog.data.aspect

import com.infowings.catalog.common.*
import com.infowings.catalog.data.Subject
import com.infowings.catalog.data.history.HistoryAware
import com.infowings.catalog.data.history.Snapshot
import com.infowings.catalog.data.history.asStringOrEmpty
import com.infowings.catalog.data.reference.book.ASPECT_REFERENCE_BOOK_EDGE
import com.infowings.catalog.data.reference.book.ReferenceBookVertex
import com.infowings.catalog.data.reference.book.toReferenceBookVertex
import com.infowings.catalog.data.subject.toSubject
import com.infowings.catalog.data.subject.toSubjectVertex
import com.infowings.catalog.data.toSubjectData
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OEdge
import com.orientechnologies.orient.core.record.OVertex
import hasIncomingEdges


fun OVertex.toAspectVertex() = AspectVertex(this)
fun OVertex.toAspectPropertyVertex() = AspectPropertyVertex(this)
fun OVertex.isJustCreated() = this.identity.isNew

/**
 * Kotlin does not provide package-level declarations.
 * These OVertex extensions must be available for whole package and nowhere else without special methods calls.
 * by vertex means simple delegating OVertex calls to property [vertex]
 * */
class AspectVertex(private val vertex: OVertex) : HistoryAware, OVertex by vertex {
    override val entityClass = ASPECT_CLASS

    override fun currentSnapshot(): Snapshot = Snapshot(
        data = mapOf(
            AspectField.NAME.name to asStringOrEmpty(name),
            AspectField.MEASURE.name to asStringOrEmpty(measure),
            AspectField.BASE_TYPE.name to asStringOrEmpty(baseType)
        ),
        links = mapOf(
            AspectField.PROPERTY to properties.map { it.identity }
        )
    )

    fun toAspectData(): AspectData {
        val baseTypeObj = baseType?.let { BaseType.restoreBaseType(it) }
        return AspectData(
            id,
            name,
            measureName,
            baseTypeObj?.let { OpenDomain(it).toString() },
            baseType,
            properties.map { it.toAspectPropertyVertex().toAspectPropertyData() },
            version,
            subject?.toSubjectData(),
            deleted,
            description,
            referenceBookVertex?.name
        )
    }

    val properties: List<OVertex>
        get() = vertex.getVertices(ODirection.OUT, ASPECT_ASPECT_PROPERTY_EDGE).toList()

    val referenceBookVertex: ReferenceBookVertex?
        get() = vertex.getVertices(ODirection.OUT, ASPECT_REFERENCE_BOOK_EDGE)
            .map { it.toReferenceBookVertex() }
            .filterNot { it.deleted }
            .firstOrNull()

    fun dropRefBookEdge() {
        vertex.getEdges(ODirection.OUT, ASPECT_REFERENCE_BOOK_EDGE).forEach { it.delete<OEdge>() }
    }

    var baseType: String?
        get() = measure?.baseType?.name ?: this["baseType"]
        set(value) {
            vertex["baseType"] = value
        }

    var name: String
        get() = vertex[ATTR_NAME]
        set(value) {
            vertex[ATTR_NAME] = value
        }

    val measure: Measure<*>?
        get() = GlobalMeasureMap[measureName]

    var measureName: String?
        get() = vertex["measure"]
        set(value) {
            vertex["measure"] = value
        }

    var deleted: Boolean
        get() = vertex["deleted"] ?: false
        set(value) {
            vertex["deleted"] = value
        }

    val subject: Subject?
        get() {
            val subjects = vertex.getVertices(ODirection.OUT, ASPECT_SUBJECT_EDGE).toList()
            if (subjects.size > 1) {
                throw OnlyOneSubjectForAspectIsAllowed(name)
            }
            return subjects.firstOrNull()?.toSubjectVertex()?.toSubject()
        }

    var description: String?
        get() = vertex[ATTR_DESC]
        set(value) {
            vertex[ATTR_DESC] = value
        }

    fun isLinkedBy() = hasIncomingEdges()

    override fun equals(other: Any?): Boolean {
        return vertex == other
    }

    override fun hashCode(): Int {
        return vertex.hashCode()
    }
}

class OnlyOneSubjectForAspectIsAllowed(name: String) : Exception("Too many subject for aspect '$name'")

class AspectPropertyVertex(private val vertex: OVertex) : HistoryAware, OVertex by vertex {
    override val entityClass = ASPECT_PROPERTY_CLASS

    override fun currentSnapshot(): Snapshot = Snapshot(
        data = mapOf(
            AspectPropertyField.NAME.name to asStringOrEmpty(name),
            AspectPropertyField.ASPECT.name to asStringOrEmpty(aspect),
            AspectPropertyField.CARDINALITY.name to asStringOrEmpty(cardinality)
        ),
        links = emptyMap()
    )

    fun toAspectPropertyData(): AspectPropertyData =
        AspectPropertyData(id, name, aspect, cardinality, version)

    var name: String
        get() = vertex["name"]
        set(value) {
            vertex["name"] = value
        }

    var aspect: String
        get() = vertex["aspectId"]
        set(value) {
            vertex["aspectId"] = value
        }

    var cardinality: String
        get() = vertex["cardinality"]
        set(value) {
            vertex["cardinality"] = value
        }

    var deleted: Boolean
        get() = vertex["deleted"] ?: false
        set(value) {
            vertex["deleted"] = value
        }

    override fun equals(other: Any?): Boolean {
        return vertex == other
    }

    override fun hashCode(): Int {
        return vertex.hashCode()
    }
}