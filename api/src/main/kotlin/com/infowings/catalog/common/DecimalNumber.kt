package com.infowings.catalog.common

import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.pow

actual class DecimalNumber(val value: BigDecimal) {
    actual constructor(value: Double) : this(BigDecimal(value))
    actual constructor(value: Int) : this(BigDecimal(value))
    actual constructor(value: String) : this(BigDecimal(value))

    actual operator fun minus(other: DecimalNumber): DecimalNumber =
        DecimalNumber(value - other.value)

    actual operator fun times(other: DecimalNumber): DecimalNumber =
        DecimalNumber(value * other.value)

    actual operator fun plus(other: DecimalNumber): DecimalNumber =
        DecimalNumber(value + other.value)

    actual operator fun div(other: DecimalNumber): DecimalNumber =
        DecimalNumber(value.divide(other.value, MathContext.DECIMAL64))

    actual fun pow(other: DecimalNumber): DecimalNumber =
        DecimalNumber(value.toDouble().pow(other.value.toDouble()))

    actual fun toPlainString(): String = value.stripTrailingZeros().toPlainString()
    actual override fun toString(): String = value.stripTrailingZeros().toString()
}

actual fun log10(num: DecimalNumber): DecimalNumber =
    DecimalNumber(kotlin.math.log10(num.value.toDouble()))
