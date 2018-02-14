package com.infowings.catalog.data.measure

import com.infowings.catalog.data.LengthMeasure
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import java.math.BigDecimal


@RunWith(SpringJUnit4ClassRunner::class)
class LengthMeasureConverterTest {

    @Test
    fun kilometreToMetreAndViseVersaTest() {
        val kmValue = BigDecimal(120)
        val metreValue = LengthMeasure.toBase(kmValue, LengthMeasure.Unit.Kilometre)
        assertEquals("Converting km to m fails", metreValue, BigDecimal(120000), EPS)
        assertEquals("Converting m to km fails",
                kmValue,
                LengthMeasure.fromBase(metreValue, LengthMeasure.Unit.Kilometre),
                EPS)
    }

    @Test
    fun decimetreToMetreAndViseVersaTest() {
        val decimetreValue = BigDecimal(11)
        val metreValue = LengthMeasure.toBase(decimetreValue, LengthMeasure.Unit.Decimetre)
        assertEquals("Converting dm to m fails", metreValue, BigDecimal(1.1), EPS)
        assertEquals("Converting m to dm fails",
                decimetreValue,
                LengthMeasure.fromBase(metreValue, LengthMeasure.Unit.Decimetre),
                EPS)
    }

    @Test
    fun centimetreToMetreAndViseVersaTest() {
        val centimetreValue = BigDecimal(10)
        val metreValue = LengthMeasure.toBase(centimetreValue, LengthMeasure.Unit.Centimetre)
        assertEquals("Converting cm to m fails", metreValue, BigDecimal(0.1), EPS)
        assertEquals("Converting m to cm fails",
                centimetreValue,
                LengthMeasure.fromBase(metreValue, LengthMeasure.Unit.Centimetre),
                EPS)
    }

    @Test
    fun millimetreToMetreAndViseVersaTest() {
        val millimetreValue = BigDecimal(0.1)
        val metreValue = LengthMeasure.toBase(millimetreValue, LengthMeasure.Unit.Millimetre)
        assertEquals("Converting millimetre to m fails", metreValue, BigDecimal(0.0001), EPS)
        assertEquals("Converting m to millimetre fails",
                millimetreValue,
                LengthMeasure.fromBase(metreValue, LengthMeasure.Unit.Millimetre),
                EPS)
    }

    @Test
    fun micrometreToMetreAndViseVersaTest() {
        val microValue = BigDecimal(669)
        val metreValue = LengthMeasure.toBase(microValue, LengthMeasure.Unit.Micrometre)
        assertEquals("Converting micrometre to m fails", metreValue, BigDecimal(0.000669), EPS)
        assertEquals("Converting m to micrometre fails",
                microValue,
                LengthMeasure.fromBase(metreValue, LengthMeasure.Unit.Micrometre),
                EPS)
    }

    @Test
    fun nanometreToMetreAndViseVersaTest() {
        val nanoValue = BigDecimal(120000)
        val metreValue = LengthMeasure.toBase(nanoValue, LengthMeasure.Unit.Nanometre)
        assertEquals("Converting nanometre to m fails", metreValue, BigDecimal(0.00012), EPS)
        assertEquals("Converting m to nanometre fails",
                nanoValue,
                LengthMeasure.fromBase(metreValue, LengthMeasure.Unit.Nanometre),
                EPS)
    }

    @Test
    fun yardToMetreAndViseVersaTest() {
        val yardValue = BigDecimal(3.456)
        val metreValue = LengthMeasure.toBase(yardValue, LengthMeasure.Unit.Yard)
        assertEquals("Converting yard to m fails", metreValue, BigDecimal(3.160166), EPS)
        assertEquals("Converting m to yard fails",
                yardValue,
                LengthMeasure.fromBase(metreValue, LengthMeasure.Unit.Yard),
                EPS)
    }

    @Test
    fun mileToMetreAndViseVersaTest() {
        val kmValue = BigDecimal(2.71)
        val metreValue = LengthMeasure.toBase(kmValue, LengthMeasure.Unit.Mile)
        assertEquals("Converting mile to m fails", metreValue, BigDecimal(4361.32224), EPS)
        assertEquals("Converting m to mile fails",
                kmValue,
                LengthMeasure.fromBase(metreValue, LengthMeasure.Unit.Mile),
                EPS)
    }

    @Test
    fun inchToMetreAndViseVersaTest() {
        val kmValue = BigDecimal(123)
        val metreValue = LengthMeasure.toBase(kmValue, LengthMeasure.Unit.Inch)
        assertEquals("Converting inch to m fails", metreValue, BigDecimal(3.1242), EPS)
        assertEquals("Converting m to inch fails",
                kmValue,
                LengthMeasure.fromBase(metreValue, LengthMeasure.Unit.Inch),
                EPS)
    }
}