package com.infowings.catalog.data.aspect

import com.infowings.catalog.common.*
import com.infowings.catalog.data.Subject
import com.infowings.catalog.data.toSubject
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OVertex
import hasIncomingEdges


fun OVertex.toAspectVertex() = AspectVertex(this)
fun OVertex.toAspectPropertyVertex() = AspectPropertyVertex(this)

/**
 * Kotlin does not provide package-level declarations.
 * These OVertex extensions must be available for whole package and nowhere else without special methods calls.
 * by vertex means simple delegating OVertex calls to property [vertex]
 * */
class AspectVertex(private val vertex: OVertex) : OVertex by vertex {

    fun toAspectData(): AspectData {
        val baseTypeObj = baseType?.let { BaseType.restoreBaseType(it) }
        return AspectData(
                id,
                name,
                measureName,
                baseTypeObj?.let { OpenDomain(it).toString() },
                baseType,
                properties.map { it.toAspectPropertyVertex().toAspectPropertyData() },
                version)
    }

    val properties: List<OVertex>
        get() = vertex.getVertices(ODirection.OUT, ASPECT_ASPECTPROPERTY_EDGE).toList()

    var baseType: String?
        get() = measure?.baseType?.name ?: this["baseType"]
        set(value) {
            vertex["baseType"] = value
        }

    var name: String
        get() = vertex["name"]
        set(value) {
            vertex["name"] = value
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
        get() = vertex.getVertices(ODirection.OUT, ASPECT_SUBJECT_EDGE).toList().firstOrNull()?.toSubject()

    fun isLinkedBy() = hasIncomingEdges()

    override fun equals(other: Any?): Boolean {
        return vertex == other
    }

    override fun hashCode(): Int {
        return vertex.hashCode()
    }
}

class AspectPropertyVertex(private val vertex: OVertex) : OVertex by vertex {

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
