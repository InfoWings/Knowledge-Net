package com.infowings.catalog.common


interface ScalarValue {
    fun encode(): String
}

data class Integer(val value: Int) : ScalarValue {
    override fun encode(): String = "INT: $value"
}

fun decode(repr: String): ScalarValue =
    if (repr.startsWith("INT: ")) Integer(repr.drop(5).toInt()) else throw IllegalStateException()

data class Range(val left: Int, val right: Int)

enum class CharacteristicType {
    ASPECT, ASPECT_PROPERTY, MEASURE
}

data class ObjectPropertyValueData(
    val id: String?,
    val value: ScalarValue?,
    val range: Range?,
    val precision: Int?,
    val objectPropertyId: String,
    val characteristics: List<CharacteristicData>
)

data class CharacteristicData (val id: String, val type: CharacteristicType)