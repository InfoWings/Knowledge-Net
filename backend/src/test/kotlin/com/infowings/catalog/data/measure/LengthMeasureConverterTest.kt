//package com.infowings.catalog.data.measure
//
//import com.infowings.catalog.data.LengthMeasure
//import org.junit.Test
//import org.junit.runner.RunWith
//import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
//import java.math.BigDecimal
//
//
//@RunWith(SpringJUnit4ClassRunner::class)
//class LengthMeasureConverterTest {
//
//    @Test
//    fun kilometreToMetreAndViseVersaTest() {
//        val kmValue = BigDecimal(120)
//        val metreValue = LengthMeasure.toBase(kmValue, LengthMeasure.Unit.Kilometre)
//        assertEquals<LengthMeasure>("Converting km to m fails", metreValue, BigDecimal(120000))
//        assertEquals<LengthMeasure>("Converting m to km fails",
//                kmValue,
//                LengthMeasure.fromBase(metreValue, LengthMeasure.Unit.Kilometre))
//    }
//
//    @Test
//    fun decimetreToMetreAndViseVersaTest() {
//        val decimetreValue = BigDecimal(11)
//        val metreValue = LengthMeasure.toBase(decimetreValue, LengthMeasure.Unit.Decimetre)
//        assertEquals<LengthMeasure>("Converting dm to m fails", metreValue, BigDecimal(1.1))
//        assertEquals<LengthMeasure>("Converting m to dm fails",
//                decimetreValue,
//                LengthMeasure.fromBase(metreValue, LengthMeasure.Unit.Decimetre))
//    }
//
//    @Test
//    fun centimetreToMetreAndViseVersaTest() {
//        val centimetreValue = BigDecimal(10)
//        val metreValue = LengthMeasure.toBase(centimetreValue, LengthMeasure.Unit.Centimetre)
//        assertEquals<LengthMeasure>("Converting cm to m fails", metreValue, BigDecimal(0.1))
//        assertEquals<LengthMeasure>("Converting m to cm fails",
//                centimetreValue,
//                LengthMeasure.fromBase(metreValue, LengthMeasure.Unit.Centimetre))
//    }
//
//    @Test
//    fun millimetreToMetreAndViseVersaTest() {
//        val millimetreValue = BigDecimal(0.1)
//        val metreValue = LengthMeasure.toBase(millimetreValue, LengthMeasure.Unit.Millimetre)
//        assertEquals<LengthMeasure>("Converting millimetre to m fails", metreValue, BigDecimal(0.0001))
//        assertEquals<LengthMeasure>("Converting m to millimetre fails",
//                millimetreValue,
//                LengthMeasure.fromBase(metreValue, LengthMeasure.Unit.Millimetre))
//    }
//
//    @Test
//    fun micrometreToMetreAndViseVersaTest() {
//        val microValue = BigDecimal(669)
//        val metreValue = LengthMeasure.toBase(microValue, LengthMeasure.Unit.Micrometre)
//        assertEquals<LengthMeasure>("Converting micrometre to m fails", metreValue, BigDecimal(0.000669))
//        assertEquals<LengthMeasure>("Converting m to micrometre fails",
//                microValue,
//                LengthMeasure.fromBase(metreValue, LengthMeasure.Unit.Micrometre))
//    }
//
//    @Test
//    fun nanometreToMetreAndViseVersaTest() {
//        val nanoValue = BigDecimal(120000)
//        val metreValue = LengthMeasure.toBase(nanoValue, LengthMeasure.Unit.Nanometre)
//        assertEquals<LengthMeasure>("Converting nanometre to m fails", metreValue, BigDecimal(0.00012))
//        assertEquals<LengthMeasure>("Converting m to nanometre fails",
//                nanoValue,
//                LengthMeasure.fromBase(metreValue, LengthMeasure.Unit.Nanometre))
//    }
//
//    @Test
//    fun yardToMetreAndViseVersaTest() {
//        val yardValue = BigDecimal(3.456)
//        val metreValue = LengthMeasure.toBase(yardValue, LengthMeasure.Unit.Yard)
//        assertEquals<LengthMeasure>("Converting yard to m fails", metreValue, BigDecimal(3.160166))
//        assertEquals<LengthMeasure>("Converting m to yard fails",
//                yardValue,
//                LengthMeasure.fromBase(metreValue, LengthMeasure.Unit.Yard))
//    }
//
//    @Test
//    fun mileToMetreAndViseVersaTest() {
//        val mileValue = BigDecimal(2.71)
//        val metreValue = LengthMeasure.toBase(mileValue, LengthMeasure.Unit.Mile)
//        assertEquals<LengthMeasure>("Converting mile to m fails", metreValue, BigDecimal(4361.32224))
//        assertEquals<LengthMeasure>("Converting m to mile fails",
//                mileValue,
//                LengthMeasure.fromBase(metreValue, LengthMeasure.Unit.Mile))
//    }
//
//    @Test
//    fun inchToMetreAndViseVersaTest() {
//        val inchValue = BigDecimal(123)
//        val metreValue = LengthMeasure.toBase(inchValue, LengthMeasure.Unit.Inch)
//        assertEquals<LengthMeasure>("Converting inch to m fails", metreValue, BigDecimal(3.12419))
//        assertEquals<LengthMeasure>("Converting m to inch fails",
//                inchValue,
//                LengthMeasure.fromBase(metreValue, LengthMeasure.Unit.Inch))
//    }
//}