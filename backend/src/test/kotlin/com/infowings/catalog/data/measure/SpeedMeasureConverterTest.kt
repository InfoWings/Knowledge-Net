package com.infowings.catalog.data.measure

import com.infowings.catalog.data.*
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import java.math.BigDecimal

@RunWith(SpringJUnit4ClassRunner::class)
class SpeedMeasureConverterTest {
    @Test
    fun kilometrePerSecondToMetrePerSecAndViseVersaTest() = toMetrePerSecondTest(14.3, 14300.0, KilometrePerSecond)

    @Test
    fun milePerHourToMetrePerSecAndViseVersaTest() = toMetrePerSecondTest(2.71, 1.2114784, MilePerHour)

    @Test
    fun inchPerSecondToMetrePerSecAndViseVersaTest() = toMetrePerSecondTest(3.24, 0.98755, InchPerSecond)

    @Test
    fun kilometrePerHourToMetrePerSecAndViseVersaTest() = toMetrePerSecondTest(123.0, 34.16669, KilometrePerHour)

    @Test
    fun knotToMetrePerSecAndViseVersaTest() = toMetrePerSecondTest(13.0, 6.68777, Knot)

    private fun toMetrePerSecondTest(source: Double, sourceInMetrePerSecond: Double, measure: Measure<BigDecimal>) {
        val sourceBigDecimal = BigDecimal(source)
        val metrePerSecondValue = measure.toBase(sourceBigDecimal)
        assertEquals("Converting ${measure.symbol} to m/s fails", metrePerSecondValue, BigDecimal(sourceInMetrePerSecond))
        assertEquals("Converting m/s to ${measure.symbol} fails", sourceBigDecimal, measure.fromBase(metrePerSecondValue))
    }
}