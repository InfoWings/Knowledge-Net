package com.infowings.catalog.data.measure

import com.infowings.common.*
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

@RunWith(SpringJUnit4ClassRunner::class)
class AreaAndVolumeConverterTest {

    @Test
    fun hectareToSquareMetreAndViseVersaTest() = toSquareMetreTest(120.0, 1200000.0, Hectare)

    @Test
    fun squareInchToSquareMetreAndViseVersaTest() = toSquareMetreTest(14.5, 0.00935482, SquareInch)

    @Test
    fun cubicMillimetreToCubicMetreAndVersaTest() = toCubicMetreTest(147331.0, 0.000147331, CubicMillimetre)

    @Test
    fun litreToCubicMetreAndVersaTest() = toCubicMetreTest(13.56, 0.01356, Litre)

    @Test
    fun cubicDecimetreToCubicMetreAndVersaTest() = toCubicMetreTest(669.0, 0.669, CubicDecimetre)

    @Test
    fun cubicCentimetreToCubicMetreAndVersaTest() = toCubicMetreTest(120000.0, 0.12, CubicCentimetre)

    @Test
    fun cubicInchToCubicMetreAndVersaTest() = toCubicMetreTest(300.456, 0.004923591701184, CubicInch)

    @Test
    fun pintToCubicMetreAndVersaTest() = toCubicMetreTest(2.71, 0.001282308, Pint)

    @Test
    fun gallonToMetreAndViseVersaTest() = toCubicMetreTest(123.0, 0.4656056, Gallon)

    private fun toSquareMetreTest(source: Double, sourceInSquareMetre: Double, measure: Measure<DecimalNumber>) =
            measureTest(source, sourceInSquareMetre, measure, AreaGroup.base)

    private fun toCubicMetreTest(source: Double, sourceInSquareMetre: Double, measure: Measure<DecimalNumber>) =
            measureTest(source, sourceInSquareMetre, measure, VolumeGroup.base)
}