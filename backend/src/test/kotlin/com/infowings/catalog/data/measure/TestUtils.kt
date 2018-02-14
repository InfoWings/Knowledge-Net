package com.infowings.catalog.data.measure

import java.math.BigDecimal

const val EPS = 0.0001

fun assertEquals(expected: BigDecimal, actual: BigDecimal, delta: Double): Boolean {
    return (expected - actual) * (expected - actual) < BigDecimal(delta * delta)
}