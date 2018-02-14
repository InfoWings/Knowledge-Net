package com.infowings.catalog.data.measure

import com.infowings.catalog.data.SpeedMeasure
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import java.math.BigDecimal

@RunWith(SpringJUnit4ClassRunner::class)
class SpeedMeasureConverterTest {
    @Test
    fun kilometrePerSecondToMetrePerSecAndViseVersaTest() {
        val kmPerSec = BigDecimal(14.3)
        val metrePerSec = SpeedMeasure.toBase(kmPerSec, SpeedMeasure.Unit.KilometrePerSecond)
        assertEquals(metrePerSec, BigDecimal(14000), EPS)
        assertEquals(kmPerSec, SpeedMeasure.fromBase(metrePerSec, SpeedMeasure.Unit.KilometrePerSecond), EPS)
    }

    @Test
    fun milePerHourToMetrePerSecAndViseVersaTest() {
        val kmPerSec = BigDecimal(2.71)
        val metrePerSec = SpeedMeasure.toBase(kmPerSec, SpeedMeasure.Unit.MilePerHour)
        assertEquals(metrePerSec, BigDecimal(0.89408), EPS)
        assertEquals(kmPerSec, SpeedMeasure.fromBase(metrePerSec, SpeedMeasure.Unit.MilePerHour), EPS)
    }

    @Test
    fun inchPerSecondToMetrePerSecAndViseVersaTest() {
        val kmPerSec = BigDecimal(3.24)
        val metrePerSec = SpeedMeasure.toBase(kmPerSec, SpeedMeasure.Unit.InchPerSecond)
        assertEquals(metrePerSec, BigDecimal(0.9876), EPS)
        assertEquals(kmPerSec, SpeedMeasure.fromBase(metrePerSec, SpeedMeasure.Unit.InchPerSecond), EPS)
    }

    @Test
    fun kilometrePerHourToMetrePerSecAndViseVersaTest() {
        val kmPerSec = BigDecimal(123)
        val metrePerSec = SpeedMeasure.toBase(kmPerSec, SpeedMeasure.Unit.KilometrePerHour)
        assertEquals(metrePerSec, BigDecimal(34.1667), EPS)
        assertEquals(kmPerSec, SpeedMeasure.fromBase(metrePerSec, SpeedMeasure.Unit.KilometrePerHour), EPS)
    }

    @Test
    fun knotToMetrePerSecAndViseVersaTest() {
        val kmPerSec = BigDecimal(13)
        val metrePerSec = SpeedMeasure.toBase(kmPerSec, SpeedMeasure.Unit.Knot)
        assertEquals(metrePerSec, BigDecimal(6.68778), EPS)
        assertEquals(kmPerSec, SpeedMeasure.fromBase(metrePerSec, SpeedMeasure.Unit.Knot), EPS)
    }
}