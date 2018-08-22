package com.infowings.catalog.data.measure

import com.infowings.catalog.common.*
import org.junit.jupiter.api.Test

class MoneyConverterTest {

    @Test
    fun pennyToPoundsTest() = measureTest(350.0, 3.50, Penny, UKMoneyGroup.base)

    @Test
    fun usaCentsToDollarTest() = measureTest(567.0, 5.67, CentAmerican, USAMoneyGroup.base)

    @Test
    fun euroCentsToEuroTest() = measureTest(567.0, 5.67, CentEuropean, EuroMoneyGroup.base)
}