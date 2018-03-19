package com.infowings.catalog.data.aspect

import com.infowings.catalog.common.*
import com.infowings.catalog.storage.ASPECT_ASPECTPROPERTY_EDGE
import com.infowings.catalog.storage.get
import com.infowings.catalog.storage.id
import com.infowings.catalog.storage.set
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OVertex

/**
 * Public OVertex Extensions.
 */
fun OVertex.toAspectData(): AspectData {
    val baseTypeObj = baseType?.let { BaseType.restoreBaseType(it) }
    return AspectData(
            id,
            name,
            measureName,
            baseTypeObj?.let { OpenDomain(it).toString() },
            baseType,
            properties.map { it.toAspectPropertyData() },
            version)
}

fun OVertex.toAspectPropertyData(): AspectPropertyData =
        AspectPropertyData(id, name, aspect, cardinality, version)

/**
 * Internal OVertex Extensions.
 */
internal val OVertex.properties: List<OVertex>
    get() = getVertices(ODirection.OUT, ASPECT_ASPECTPROPERTY_EDGE).toList()
internal var OVertex.baseType: String?
    get() = measure?.baseType?.name ?: this["baseType"]
    set(value) {
        this["baseType"] = value
    }
internal var OVertex.name: String
    get() = this["name"]
    set(value) {
        this["name"] = value
    }
internal var OVertex.aspect: String
    get() = this["aspectId"]
    set(value) {
        this["aspectId"] = value
    }
internal var OVertex.cardinality: String
    get() = this["cardinality"]
    set(value) {
        this["cardinality"] = value
    }
internal val OVertex.measure: Measure<*>?
    get() = GlobalMeasureMap[measureName]
internal var OVertex.measureName: String?
    get() = this["measure"]
    set(value) {
        this["measure"] = value
    }