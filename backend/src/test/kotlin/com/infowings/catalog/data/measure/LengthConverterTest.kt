package com.infowings.catalog.data.measure

import com.infowings.catalog.common.*
import org.junit.jupiter.api.Test

class LengthConverterTest {

    @Test
    fun kilometreToMetreAndViseVersaTest() = toMetreTest(120.0, 120000.0, Kilometre)

    @Test
    fun decimetreToMetreAndViseVersaTest() = toMetreTest(11.0, 1.1, Decimeter)

    @Test
    fun centimetreToMetreAndViseVersaTest() = toMetreTest(10.0, 0.1, Centimeter)

    @Test
    fun millimetreToMetreAndViseVersaTest() = toMetreTest(0.1, 0.0001, Millimetre)

    @Test
    fun micrometreToMetreAndViseVersaTest() = toMetreTest(669.0, 0.000669, Micrometer)

    @Test
    fun nanometreToMetreAndViseVersaTest() = toMetreTest(120000.0, 0.00012, Nanometer)

    @Test
    fun yardToMetreAndViseVersaTest() = toMetreTest(3.456, 3.160166, Yard)

    @Test
    fun mileToMetreAndViseVersaTest() = toMetreTest(2.71, 4361.32224, Mile)

    @Test
    fun inchToMetreAndViseVersaTest() = toMetreTest(123.0, 3.12419, Inch)

    @Test
    fun feetToMetreTest() = toMetreTest(23.6, 7.19328, Foot)

    private fun toMetreTest(source: Double, sourceInMetre: Double, measure: Measure<DecimalNumber>) = measureTest(source, sourceInMetre, measure, LengthGroup.base)
}