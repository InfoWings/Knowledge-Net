package com.infowings.catalog.data.measure

import com.infowings.catalog.data.Fahrenheit
import com.infowings.catalog.data.TemperatureGroup
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

@RunWith(SpringJUnit4ClassRunner::class)
class TemperatureConverterTest {

    @Test
    fun fahrenheitToCelsius() = measureTest(23.0, -5.0, Fahrenheit, TemperatureGroup.base)
}