package com.infowings.catalog.data.measure

import org.junit.Assert
import java.math.BigDecimal

const val EPS = 0.0001

fun assertEquals(message: String, expected: BigDecimal, actual: BigDecimal, delta: Double) {
    if ((expected - actual) * (expected - actual) > BigDecimal(delta * delta)) {
        Assert.fail("$message, actual:$actual, expected: $expected")
    }
}