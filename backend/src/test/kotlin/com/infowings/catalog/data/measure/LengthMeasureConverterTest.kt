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
        assertEquals(metreValue, BigDecimal(120000), EPS)
        assertEquals(kmValue, LengthMeasure.fromBase(metreValue, LengthMeasure.Unit.Kilometre), EPS)
    }

    @Test
    fun decimetreToMetreAndViseVersaTest() {
        val decimetreValue = BigDecimal(11)
        val metreValue = LengthMeasure.toBase(decimetreValue, LengthMeasure.Unit.Decimetre)
        assertEquals(metreValue, BigDecimal(1.1), EPS)
        assertEquals(decimetreValue, LengthMeasure.fromBase(metreValue, LengthMeasure.Unit.Decimetre), EPS)
    }

    @Test
    fun centimetreToMetreAndViseVersaTest() {
        val centimetreValue = BigDecimal(10)
        val metreValue = LengthMeasure.toBase(centimetreValue, LengthMeasure.Unit.Centimetre)
        assertEquals(metreValue, BigDecimal(0.01), EPS)
        assertEquals(centimetreValue, LengthMeasure.fromBase(metreValue, LengthMeasure.Unit.Centimetre), EPS)
    }

    @Test
    fun millimetreToMetreAndViseVersaTest() {
        val millimetreValue = BigDecimal(0.1)
        val metreValue = LengthMeasure.toBase(millimetreValue, LengthMeasure.Unit.Millimetre)
        assertEquals(metreValue, BigDecimal(0.0001), EPS)
        assertEquals(millimetreValue, LengthMeasure.fromBase(metreValue, LengthMeasure.Unit.Millimetre), EPS)
    }

    @Test
    fun micrometreToMetreAndViseVersaTest() {
        val microValue = BigDecimal(669)
        val metreValue = LengthMeasure.toBase(microValue, LengthMeasure.Unit.Micrometre)
        assertEquals(metreValue, BigDecimal(0.000669), EPS)
        assertEquals(microValue, LengthMeasure.fromBase(metreValue, LengthMeasure.Unit.Micrometre), EPS)
    }

    @Test
    fun nanometreToMetreAndViseVersaTest() {
        val nanoValue = BigDecimal(120000)
        val metreValue = LengthMeasure.toBase(nanoValue, LengthMeasure.Unit.Nanometre)
        assertEquals(metreValue, BigDecimal(0.00012), EPS)
        assertEquals(nanoValue, LengthMeasure.fromBase(metreValue, LengthMeasure.Unit.Nanometre), EPS)
    }

    @Test
    fun yardToMetreAndViseVersaTest() {
        val yardValue = BigDecimal(3.456)
        val metreValue = LengthMeasure.toBase(yardValue, LengthMeasure.Unit.Yard)
        assertEquals(metreValue, BigDecimal(3.160166), EPS)
        assertEquals(yardValue, LengthMeasure.fromBase(metreValue, LengthMeasure.Unit.Yard), EPS)
    }

    @Test
    fun mileToMetreAndViseVersaTest() {
        val kmValue = BigDecimal(2.71)
        val metreValue = LengthMeasure.toBase(kmValue, LengthMeasure.Unit.Mile)
        assertEquals(metreValue, BigDecimal(4361.322), EPS)
        assertEquals(kmValue, LengthMeasure.fromBase(metreValue, LengthMeasure.Unit.Mile), EPS)
    }

    @Test
    fun inchToMetreAndViseVersaTest() {
        val kmValue = BigDecimal(123)
        val metreValue = LengthMeasure.toBase(kmValue, LengthMeasure.Unit.Inch)
        assertEquals(metreValue, BigDecimal(3.1242), EPS)
        assertEquals(kmValue, LengthMeasure.fromBase(metreValue, LengthMeasure.Unit.Inch), EPS)
    }
}