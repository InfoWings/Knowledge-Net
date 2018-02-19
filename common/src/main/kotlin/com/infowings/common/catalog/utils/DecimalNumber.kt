package com.infowings.common.catalog.utils

expect class DecimalNumber(value: Double) {
    constructor(value: Int)

    operator fun minus(other: DecimalNumber): DecimalNumber
    operator fun times(other: DecimalNumber): DecimalNumber
    operator fun plus(other: DecimalNumber): DecimalNumber
    operator fun div(other: DecimalNumber): DecimalNumber
}
