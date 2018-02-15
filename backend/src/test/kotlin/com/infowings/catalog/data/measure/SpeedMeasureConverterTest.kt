//package com.infowings.catalog.data.measure
//
//import com.infowings.catalog.data.LengthMeasure
//import com.infowings.catalog.data.SpeedMeasure
//import org.junit.Test
//import org.junit.runner.RunWith
//import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
//import java.math.BigDecimal
//
//@RunWith(SpringJUnit4ClassRunner::class)
//class SpeedMeasureConverterTest {
//    @Test
//    fun kilometrePerSecondToMetrePerSecAndViseVersaTest() {
//        val kmPerSec = BigDecimal(14.3)
//        val metrePerSec = SpeedMeasure.toBase(kmPerSec, SpeedMeasure.Unit.KilometrePerSecond)
//        assertEquals<SpeedMeasure>("Converting k/s to m/s fails", metrePerSec, BigDecimal(14300))
//        assertEquals<SpeedMeasure>("Converting m/s to k/s fails",
//                kmPerSec,
//                SpeedMeasure.fromBase(metrePerSec, SpeedMeasure.Unit.KilometrePerSecond))
//    }
//
//    @Test
//    fun milePerHourToMetrePerSecAndViseVersaTest() {
//        val milePerHour = BigDecimal(2.71)
//        val metrePerSec = SpeedMeasure.toBase(milePerHour, SpeedMeasure.Unit.MilePerHour)
//        assertEquals<SpeedMeasure>("Converting mile/h to m/s fails", metrePerSec, BigDecimal(1.2114784))
//        assertEquals<SpeedMeasure>("Converting m/s to mile/h fails",
//                milePerHour,
//                SpeedMeasure.fromBase(metrePerSec, SpeedMeasure.Unit.MilePerHour))
//    }
//
//    @Test
//    fun inchPerSecondToMetrePerSecAndViseVersaTest() {
//        val inchPerSecond = BigDecimal(3.24)
//        val metrePerSec = SpeedMeasure.toBase(inchPerSecond, SpeedMeasure.Unit.InchPerSecond)
//        assertEquals<SpeedMeasure>("Converting inch/s to m/s fails", metrePerSec, BigDecimal(0.98755))
//        assertEquals<SpeedMeasure>("Converting m/s to inch/s fails",
//                inchPerSecond,
//                SpeedMeasure.fromBase(metrePerSec, SpeedMeasure.Unit.InchPerSecond))
//    }
//
//    @Test
//    fun kilometrePerHourToMetrePerSecAndViseVersaTest() {
//        val kmPerHour = BigDecimal(123)
//        val metrePerSec = SpeedMeasure.toBase(kmPerHour, SpeedMeasure.Unit.KilometrePerHour)
//        assertEquals<SpeedMeasure>("Converting km/h to m/s fails", metrePerSec, BigDecimal(34.16669))
//        assertEquals<SpeedMeasure>("Converting m/s to km/h fails",
//                kmPerHour,
//                SpeedMeasure.fromBase(metrePerSec, SpeedMeasure.Unit.KilometrePerHour))
//    }
//
//    @Test
//    fun knotToMetrePerSecAndViseVersaTest() {
//        val knot = BigDecimal(13)
//        val metrePerSec = SpeedMeasure.toBase(knot, SpeedMeasure.Unit.Knot)
//        assertEquals<SpeedMeasure>("Converting knot to m/s fails", metrePerSec, BigDecimal(6.68777))
//        assertEquals<SpeedMeasure>("Converting m/s to knot fails",
//                knot,
//                SpeedMeasure.fromBase(metrePerSec, SpeedMeasure.Unit.Knot))
//    }
//}