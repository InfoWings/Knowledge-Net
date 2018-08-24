package com.infowings.catalog.common

import kotlin.math.pow

actual class DecimalNumber actual constructor(val value: Double) {
    actual constructor(value: Int) : this(value.toDouble())
    actual constructor(value: String) : this(value.toDouble())

    actual operator fun minus(other: DecimalNumber): DecimalNumber = DecimalNumber(value - other.value)
    actual operator fun times(other: DecimalNumber): DecimalNumber = DecimalNumber(value * other.value)
    actual operator fun plus(other: DecimalNumber): DecimalNumber = DecimalNumber(value + other.value)
    actual operator fun div(other: DecimalNumber): DecimalNumber = DecimalNumber(value / other.value)
    actual fun pow(other: DecimalNumber): DecimalNumber = DecimalNumber(value.pow(other.value))
}

actual fun log10(num: DecimalNumber): DecimalNumber = DecimalNumber(kotlin.math.log10(num.value))