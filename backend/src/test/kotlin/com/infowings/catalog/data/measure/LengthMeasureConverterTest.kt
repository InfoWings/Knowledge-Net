package com.infowings.catalog.data.measure

import com.infowings.catalog.data.*
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import java.math.BigDecimal


@RunWith(SpringJUnit4ClassRunner::class)
class LengthMeasureConverterTest {

    @Test
    fun kilometreToMetreAndViseVersaTest() {
        val kmValue = BigDecimal(120)
        val metreValue = Kilometre.toBase(kmValue)
        assertEquals("Converting km to m fails", metreValue, BigDecimal(120000))
        assertEquals("Converting m to km fails", kmValue, Kilometre.fromBase(metreValue))
    }

    @Test
    fun decimetreToMetreAndViseVersaTest() {
        val decimetreValue = BigDecimal(11)
        val metreValue = Decimetre.toBase(decimetreValue)
        assertEquals("Converting dm to m fails", metreValue, BigDecimal(1.1))
        assertEquals("Converting m to dm fails", decimetreValue, Decimetre.fromBase(metreValue))
    }

    @Test
    fun centimetreToMetreAndViseVersaTest() {
        val centimetreValue = BigDecimal(10)
        val metreValue = Centimetre.toBase(centimetreValue)
        assertEquals("Converting cm to m fails", metreValue, BigDecimal(0.1))
        assertEquals("Converting m to cm fails", centimetreValue, Centimetre.fromBase(metreValue))
    }

    @Test
    fun millimetreToMetreAndViseVersaTest() {
        val millimetreValue = BigDecimal(0.1)
        val metreValue = Millimetre.toBase(millimetreValue)
        assertEquals("Converting millimetre to m fails", metreValue, BigDecimal(0.0001))
        assertEquals("Converting m to millimetre fails", millimetreValue, Millimetre.fromBase(metreValue))
    }

    @Test
    fun micrometreToMetreAndViseVersaTest() {
        val microValue = BigDecimal(669)
        val metreValue = Micrometre.toBase(microValue)
        assertEquals("Converting micrometre to m fails", metreValue, BigDecimal(0.000669))
        assertEquals("Converting m to micrometre fails", microValue, Micrometre.fromBase(metreValue))
    }

    @Test
    fun nanometreToMetreAndViseVersaTest() {
        val nanoValue = BigDecimal(120000)
        val metreValue = Nanometre.toBase(nanoValue)
        assertEquals("Converting nanometre to m fails", metreValue, BigDecimal(0.00012))
        assertEquals("Converting m to nanometre fails", nanoValue, Nanometre.fromBase(metreValue))
    }

    @Test
    fun yardToMetreAndViseVersaTest() {
        val yardValue = BigDecimal(3.456)
        val metreValue = Yard.toBase(yardValue)
        assertEquals("Converting yard to m fails", metreValue, BigDecimal(3.160166))
        assertEquals("Converting m to yard fails", yardValue, Yard.fromBase(metreValue))
    }

    @Test
    fun mileToMetreAndViseVersaTest() {
        val mileValue = BigDecimal(2.71)
        val metreValue = Mile.toBase(mileValue)
        assertEquals("Converting mile to m fails", metreValue, BigDecimal(4361.32224))
        assertEquals("Converting m to mile fails", mileValue, Mile.fromBase(metreValue))
    }

    @Test
    fun inchToMetreAndViseVersaTest() {
        val inchValue = BigDecimal(123)
        val metreValue = Inch.toBase(inchValue)
        assertEquals("Converting inch to m fails", metreValue, BigDecimal(3.12419))
        assertEquals("Converting m to inch fails", inchValue, Inch.fromBase(metreValue))
    }

    @Test
    fun feetToMetreTest() {
        val feetValue = BigDecimal(23.6)
        val metreValue = Feet.toBase(feetValue)
        assertEquals("Converting feet to m fails", metreValue, BigDecimal(7.19328))
        assertEquals("Converting m to feet fails", feetValue, Feet.fromBase(metreValue))
    }
}