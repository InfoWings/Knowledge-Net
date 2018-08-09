package com.infowings.catalog.data.measure

import com.infowings.catalog.common.Fahrenheit
import com.infowings.catalog.common.TemperatureGroup
import org.junit.jupiter.api.Test

class TemperatureConverterTest {

    @Test
    fun fahrenheitToCelsius() = measureTest(23.0, -5.0, Fahrenheit, TemperatureGroup.base)
}