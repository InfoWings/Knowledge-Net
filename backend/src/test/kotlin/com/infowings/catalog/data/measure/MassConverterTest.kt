package com.infowings.catalog.data.measure

import com.infowings.catalog.common.*
import org.junit.jupiter.api.Test

class MassConverterTest {

    @Test
    fun gramToKilogramTest() = toKilogramTest(123.0, 0.123, Gram)

    @Test
    fun milligramToKilogramTest() = toKilogramTest(234.0, 0.000234, Milligram)

    @Test
    fun tonToKilogramTest() = toKilogramTest(4.23, 4230.0, Tonne)

    @Test
    fun poundToKilogramTest() = toKilogramTest(45.5, 20.63845, PoundMass)

    private fun toKilogramTest(source: Double, sourceInKilo: Double, measure: Measure<DecimalNumber>) = measureTest(source, sourceInKilo, measure, MassGroup.base)
}