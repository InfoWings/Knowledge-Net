package com.infowings.catalog.data.objekt

import com.infowings.catalog.data.aspect.AspectPropertyVertex
import com.infowings.catalog.data.aspect.AspectVertex
import com.infowings.catalog.data.aspect.toAspectPropertyVertex
import com.infowings.catalog.data.aspect.toAspectVertex
import com.infowings.catalog.data.history.HistoryAware
import com.infowings.catalog.data.history.Snapshot
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OVertex

fun OVertex.toCharacteristicVertex() = CharacteristicVertex(this)

/**
 * Вспомогательная сущность, позволяющая сохранять список тройки <Aspect, AspectProperty, Measure>
 *     Пользователю ее не должно быть видно
 */
class CharacteristicVertex(private val vertex: OVertex) : HistoryAware, OVertex by vertex {
    override val entityClass = CHARACTERISTIC_VALUE_CLASS

    override fun currentSnapshot(): Snapshot = Snapshot(
        data = emptyMap(),
        links = emptyMap()
    )

    val aspect: AspectVertex?
        get() = vertex.getVertices(ODirection.OUT, CHARACTERISTIC_ASPECT_EDGE).firstOrNull()?.toAspectVertex()

    private val aspectProperty: AspectPropertyVertex?
        get() = vertex.getVertices(ODirection.OUT, CHARACTERISTIC_ASPECT_PROPERTY_EDGE).firstOrNull()?.toAspectPropertyVertex()

    val measure: OVertex?
        get() = vertex.getVertices(ODirection.OUT, CHARACTERISTIC_MEASURE_EDGE).firstOrNull()

    fun toCharacteristic(): Characteristic {
        val currentProperty = aspectProperty
        val currentAspect = aspect
        val currentMeasure = measure

        if (currentProperty == null) {
            throw ObjectCharacteristicWithoutPropertyException(this)
        }
        if (currentAspect == null) {
            throw ObjectCharacteristicWithoutAspectException(this)
        }
        if (currentMeasure == null) {
            throw ObjectCharacteristicWithoutMeasureException(this)
        }

        return Characteristic(currentAspect, currentProperty, currentMeasure)
    }
}

class ObjectCharacteristicWithoutAspectException(vertex: CharacteristicVertex) :
    ObjectValueException("Aspect vertex not linked for ${vertex.id} ")
class ObjectCharacteristicWithoutPropertyException(vertex: CharacteristicVertex) :
    ObjectValueException("Aspect property vertex not linked for ${vertex.id} ")
class ObjectCharacteristicWithoutMeasureException(vertex: CharacteristicVertex) :
    ObjectValueException("Measure vertex not linked for ${vertex.id} ")