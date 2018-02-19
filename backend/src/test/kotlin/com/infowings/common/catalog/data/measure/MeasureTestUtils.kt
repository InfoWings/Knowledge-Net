package com.infowings.common.catalog.data.measure

import com.infowings.common.catalog.data.Measure
import com.infowings.common.catalog.utils.DecimalNumber
import org.junit.Assert
import java.math.RoundingMode

internal fun assertEquals(message: String, actual: DecimalNumber, expected: DecimalNumber) {
    val scaledActual = actual.value.setScale(4, RoundingMode.HALF_UP)
    val scaledExpected = expected.value.setScale(4, RoundingMode.HALF_UP)
    if (scaledActual != scaledExpected) {
        Assert.fail("$message, actual:$scaledActual, expected: $scaledExpected")
    }
}

internal fun measureTest(source: Double, sourceInBase: Double, measure: Measure<DecimalNumber>, baseMeasure: Measure<DecimalNumber>) {
    val sourceBigDecimal = DecimalNumber(source)
    val baseValue = measure.toBase(sourceBigDecimal)
    assertEquals("Converting ${measure.symbol} to ${baseMeasure.symbol} fails", baseValue, DecimalNumber(sourceInBase))
    assertEquals("Converting ${baseMeasure.symbol} to ${measure.symbol} fails", sourceBigDecimal, measure.fromBase(baseValue))
}