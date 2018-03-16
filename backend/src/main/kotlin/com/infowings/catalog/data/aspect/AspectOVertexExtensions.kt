package com.infowings.catalog.data.aspect

import com.infowings.catalog.common.*
import com.infowings.catalog.storage.ASPECT_ASPECTPROPERTY_EDGE
import com.infowings.catalog.storage.get
import com.infowings.catalog.storage.id
import com.infowings.catalog.storage.set
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OVertex


fun OVertex.toAspectVertex() = AspectVertex(this)

fun OVertex.toAspectPropertyVertex() = AspectPropertyVertex(this)

class AspectVertex(val vertex: OVertex) : OVertex by vertex {

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
}

class AspectPropertyVertex(val vertex: OVertex) : OVertex by vertex {

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
}
