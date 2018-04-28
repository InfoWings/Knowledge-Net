package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.Range
import com.infowings.catalog.common.ScalarValue
import com.infowings.catalog.common.decode
import com.infowings.catalog.data.history.HistoryAware
import com.infowings.catalog.data.history.Snapshot
import com.infowings.catalog.data.history.asStringOrEmpty
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OVertex

fun OVertex.toObjectPropertyValueVertex() = ObjectPropertyValueVertex(this)

class ObjectPropertyValueVertex(private val vertex: OVertex) : HistoryAware, OVertex by vertex {
    override val entityClass = OBJECT_PROPERTY_VALUE_CLASS

    override fun currentSnapshot(): Snapshot = Snapshot(
        data = mapOf(
            "value" to asStringOrEmpty(value),
            "range" to asStringOrEmpty(range),
            "precision" to asStringOrEmpty(precision)
        ),
        links = emptyMap()
    )

    var value: ScalarValue?
        get() {
            val s: String = vertex["value"]
            return s.let {decode(it)}
        }
        set(value) {
            if (value != null) {
                vertex["value"] = value.encode()
            } else {
                vertex.removeProperty("value")
            }
        }

    var range: Range?
        get() {
            val s: String = vertex["range"]
            val parts = s.split(":").map {it.toInt()}
            return Range(parts[0], parts[1])
        }
        set(value) {
            if (value != null) {
                vertex["range"] = "${value.left}:${value.right}"
            } else {
                vertex.removeProperty("range")
            }
        }

    var precision: Int?
        get() = vertex["precision"]
        set(v) {
            if (v != null) {
                vertex["precision"] = v
            } else {
                vertex.removeProperty("precision")
            }
        }

    val objectProperty: ObjectPropertyVertex?
        get() = vertex.getVertices(ODirection.OUT, OBJECT_VALUE_OBJECT_PROPERTY_EDGE).firstOrNull()?.toObjectPropertyVertex()

    val characteristics: List<CharacteristicVertex>
        get() = vertex.getVertices(ODirection.OUT, OBJECT_VALUE_CHARACTERISTIC_EDGE).map {
            it.toCharacteristicVertex()
        }

    fun toObjectPropertyValue(): ObjectPropertyValue {
        val currentProperty = objectProperty ?: throw ObjectValueWithoutPropertyException(this)

        return ObjectPropertyValue(identity, value, range, precision, currentProperty, characteristics)
    }
}

abstract class ObjectValueException(message: String) : Exception(message)
class ObjectValueWithoutPropertyException(vertex: ObjectPropertyValueVertex) :
    ObjectValueException("Object property vertex not linked for ${vertex.id} ")