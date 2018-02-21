package com.infowings.catalog.data.measure

import com.infowings.catalog.common.*
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

@RunWith(SpringJUnit4ClassRunner::class)
class ElectricityIndicatorsConverterTest {

    @Test
    fun kilowattToWattTest() = toWattTest(153.0, 153000.0, Kilowatt)

    @Test
    fun horsepowerToWattTest() = toWattTest(153.0, 114092.08042, Horsepower)

    @Test
    fun voltAmpereToWattTest() = toWattTest(153.0, 153.0, VoltAmpere)

    @Test
    fun millivoltToVoltTest() = toVoltTest(45.1, 0.0451, Millivolt)

    @Test
    fun kilovoltToVoltTest() = toVoltTest(0.45, 450.0, Kilovolt)

    @Test
    fun kilovarToVarTest() = measureTest(55.3, 55300.0, Kilovar, ReactivePowerGroup.base)

    @Test
    fun kilojouleToJouleTest() = toJouleTest(0.45, 450.0, Kilojoule)

    @Test
    fun wattHourToJouleTest() = toJouleTest(16.23, 58428.0, WattHour)

    @Test
    fun kilowattHourToJouleTest() = toJouleTest(13.0, 46800000.0, KilowattHour)

    @Test
    fun milliampereToAmpereTest() = measureTest(23.0, 0.023, MilliAmpere, ElectricCurrentGroup.base)

    @Test
    fun milliOmToOmTest() = measureTest(23.0, 0.023, MilliOm, ElectricCurrentGroup.base)


    private fun toWattTest(source: Double, sourceInWatt: Double, measure: Measure<DecimalNumber>) =
            measureTest(source, sourceInWatt, measure, PowerEnergyGroup.base)

    private fun toVoltTest(source: Double, sourceInVolt: Double, measure: Measure<DecimalNumber>) = measureTest(source, sourceInVolt, measure, VoltageGroup.base)

    private fun toJouleTest(source: Double, sourceInJoule: Double, measure: Measure<DecimalNumber>) =
            measureTest(source, sourceInJoule, measure, WorkEnergyGroup.base)
}