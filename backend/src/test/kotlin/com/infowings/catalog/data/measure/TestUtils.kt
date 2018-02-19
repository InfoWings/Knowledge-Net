package com.infowings.catalog.data.measure

import org.junit.Assert
import java.math.BigDecimal
import java.math.RoundingMode

fun assertEquals(message: String, expected: BigDecimal, actual: BigDecimal) {
    val scaledActual = actual.setScale(4, RoundingMode.HALF_UP)
    val scaledExpected = expected.setScale(4, RoundingMode.HALF_UP)
    if (scaledActual != scaledExpected) {
        Assert.fail("$message, actual:$scaledActual, expected: $scaledExpected")
    }
}