package com.infowings.catalog.data.measure

import com.infowings.catalog.data.*
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import java.math.BigDecimal

@RunWith(SpringJUnit4ClassRunner::class)
class SpeedMeasureConverterTest {
    @Test
    fun kilometrePerSecondToMetrePerSecAndViseVersaTest() {
        val kmPerSec = BigDecimal(14.3)
        val metrePerSec = KilometrePerSecond.toBase(kmPerSec)
        assertEquals("Converting k/s to m/s fails", metrePerSec, BigDecimal(14300))
        assertEquals("Converting m/s to k/s fails", kmPerSec, KilometrePerSecond.fromBase(metrePerSec))
    }

    @Test
    fun milePerHourToMetrePerSecAndViseVersaTest() {
        val milePerHour = BigDecimal(2.71)
        val metrePerSec = MilePerHour.toBase(milePerHour)
        assertEquals("Converting mile/h to m/s fails", metrePerSec, BigDecimal(1.2114784))
        assertEquals("Converting m/s to mile/h fails", milePerHour, MilePerHour.fromBase(metrePerSec))
    }

    @Test
    fun inchPerSecondToMetrePerSecAndViseVersaTest() {
        val inchPerSecond = BigDecimal(3.24)
        val metrePerSec = InchPerSecond.toBase(inchPerSecond)
        assertEquals("Converting inch/s to m/s fails", metrePerSec, BigDecimal(0.98755))
        assertEquals("Converting m/s to inch/s fails", inchPerSecond, InchPerSecond.fromBase(metrePerSec))
    }

    @Test
    fun kilometrePerHourToMetrePerSecAndViseVersaTest() {
        val kmPerHour = BigDecimal(123)
        val metrePerSec = KilometrePerHour.toBase(kmPerHour)
        assertEquals("Converting km/h to m/s fails", metrePerSec, BigDecimal(34.16669))
        assertEquals("Converting m/s to km/h fails", kmPerHour, KilometrePerHour.fromBase(metrePerSec))
    }

    @Test
    fun knotToMetrePerSecAndViseVersaTest() {
        val knot = BigDecimal(13)
        val metrePerSec = Knot.toBase(knot)
        assertEquals("Converting knot to m/s fails", metrePerSec, BigDecimal(6.68777))
        assertEquals("Converting m/s to knot fails", knot, Knot.fromBase(metrePerSec))
    }
}