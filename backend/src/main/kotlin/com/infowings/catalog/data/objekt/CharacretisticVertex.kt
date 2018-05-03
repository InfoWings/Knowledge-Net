package com.infowings.catalog.data.objekt

import com.infowings.catalog.common.CharacteristicType
import com.infowings.catalog.data.MEASURE_VERTEX
import com.infowings.catalog.data.aspect.AspectPropertyVertex
import com.infowings.catalog.data.aspect.AspectVertex
import com.infowings.catalog.data.aspect.toAspectPropertyVertex
import com.infowings.catalog.data.aspect.toAspectVertex
import com.infowings.catalog.storage.ASPECT_CLASS
import com.infowings.catalog.storage.ASPECT_PROPERTY_CLASS
import com.infowings.catalog.storage.id
import com.orientechnologies.orient.core.record.OVertex

/**
 * Вспомогательная сущность, позволяющая сохранять список тройки <Aspect, AspectProperty, Measure>
 *     Пользователю ее не должно быть видно
 */

fun OVertex.toCharacteristicVertex(): CharacteristicVertex {
    val clazz = if (schemaType.isPresent) schemaType.get().name
    else throw SchemelessCharacteristicVertexTypeException(id)

    return when (clazz) {
        ASPECT_CLASS -> CharacteristicAspectVertex(this)
        ASPECT_PROPERTY_CLASS -> CharacteristicAspectPropertyVertex(this)
        MEASURE_VERTEX -> CharacteristicMeasureVertex(this)
        else -> throw IncorrectCharacteristicVertexTypeException(id, clazz)
    }
}

abstract class CharacteristicVertex(private val vertex: OVertex) : OVertex by vertex {
    open fun isAspect(): Boolean = false
    open fun isAspectProperty(): Boolean = false
    open fun isMeasure(): Boolean = false

    fun toAspectVertex(): AspectVertex = if (isAspect()) vertex.toAspectVertex() else
        throw NotAspectException(id)

    fun toAspectPropertyVertex(): AspectPropertyVertex = if (isAspectProperty()) vertex.toAspectPropertyVertex()
    else throw NotAspectPropertyException(id)

    fun toMeasureVertex(): OVertex = if (isMeasure()) vertex
    else throw NotMeasureException(id)

    fun type(): CharacteristicType {
        val clazz = if (schemaType.isPresent) schemaType.get().name else throw InternalError()
        return when (clazz) {
            ASPECT_CLASS -> CharacteristicType.ASPECT
            ASPECT_PROPERTY_CLASS -> CharacteristicType.ASPECT_PROPERTY
            MEASURE_VERTEX -> CharacteristicType.MEASURE
            else -> throw IllegalArgumentException()
        }
    }
}

data class CharacteristicAspectVertex(private val vertex: OVertex) : CharacteristicVertex(vertex) {
    override fun isAspect() = true
}

data class CharacteristicAspectPropertyVertex(val vertex: OVertex) : CharacteristicVertex(vertex) {
    override fun isAspectProperty() = true
}

data class CharacteristicMeasureVertex(val vertex: OVertex) : CharacteristicVertex(vertex) {
    override fun isMeasure() = true
}

class IncorrectCharacteristicVertexTypeException(id: String, type: String) :
    Exception("Incorrect type $type of characteristic vertex $id")

class SchemelessCharacteristicVertexTypeException(id: String) :
    Exception("Characteristic vertex $id is schemeless")

class NotAspectException(id: String) : Exception("It is not aspect. id: $id")
class NotAspectPropertyException(id: String) : Exception("It is not aspect property. id: $id")
class NotMeasureException(id: String) : Exception("It is not measure. id: $id")