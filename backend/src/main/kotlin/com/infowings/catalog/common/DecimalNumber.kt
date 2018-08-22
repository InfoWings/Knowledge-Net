package com.infowings.catalog.common

import java.math.BigDecimal
import kotlin.math.pow

actual class DecimalNumber(val value: BigDecimal) {
    actual constructor(value: Double) : this(BigDecimal(value))
    actual constructor(value: Int) : this(BigDecimal(value))

    actual operator fun minus(other: DecimalNumber): DecimalNumber = DecimalNumber(value - other.value)
    actual operator fun times(other: DecimalNumber): DecimalNumber = DecimalNumber(value * other.value)
    actual operator fun plus(other: DecimalNumber): DecimalNumber = DecimalNumber(value + other.value)
    actual operator fun div(other: DecimalNumber): DecimalNumber = DecimalNumber(value / other.value)
    actual fun pow(other: DecimalNumber): DecimalNumber = DecimalNumber(value.toDouble().pow(other.value.toDouble()))
}

actual fun log10(num: DecimalNumber): DecimalNumber = DecimalNumber(kotlin.math.log10(num.value.toDouble()))