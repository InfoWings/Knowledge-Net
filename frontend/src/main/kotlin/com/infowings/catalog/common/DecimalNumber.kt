package com.infowings.catalog.common

actual class DecimalNumber actual constructor(val value: Double) {
    actual constructor(value: Int) : this(value.toDouble())

    actual operator fun minus(other: DecimalNumber): DecimalNumber = DecimalNumber(value - other.value)
    actual operator fun times(other: DecimalNumber): DecimalNumber = DecimalNumber(value * other.value)
    actual operator fun plus(other: DecimalNumber): DecimalNumber = DecimalNumber(value + other.value)
    actual operator fun div(other: DecimalNumber): DecimalNumber = DecimalNumber(value / other.value)
}