package com.infowings.common

import java.math.BigDecimal

actual class DecimalNumber(val value: BigDecimal) {
    actual constructor(value: Double) : this(BigDecimal(value))
    actual constructor(value: Int) : this(BigDecimal(value))

    actual operator fun minus(other: DecimalNumber): DecimalNumber = DecimalNumber(value - other.value)
    actual operator fun times(other: DecimalNumber): DecimalNumber = DecimalNumber(value * other.value)
    actual operator fun plus(other: DecimalNumber): DecimalNumber = DecimalNumber(value + other.value)
    actual operator fun div(other: DecimalNumber): DecimalNumber = DecimalNumber(value / other.value)
}