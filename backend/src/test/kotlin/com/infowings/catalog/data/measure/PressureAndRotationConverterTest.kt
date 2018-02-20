package com.infowings.catalog.data.measure

import com.infowings.common.*
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

@RunWith(SpringJUnit4ClassRunner::class)
class PressureAndRotationConverterTest {

    @Test
    fun atmosphereToPascalTest() = measureTest(45.0, 4559625.0, Atmosphere, PressureGroup.base)

    @Test
    fun kilogramPerSquareMetreToPascalTest() = measureTest(23.5, 230.4563, KilogramPerSquareMetre, PressureGroup.base)

    @Test
    fun revolutionsPerSecondToRevolutionsPerMinute() = measureTest(3.0, 180.0, RevolutionsPerSecond, RotationFrequencyGroup.base)
}