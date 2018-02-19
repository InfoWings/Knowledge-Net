package com.infowings.common.catalog.data.measure

import com.infowings.common.catalog.data.*
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

@RunWith(SpringJUnit4ClassRunner::class)
class MoneyConverterTest {

    @Test
    fun pennyToPoundsTest() = measureTest(350.0, 3.50, Penny, UKMoneyGroup.base)

    @Test
    fun usaCentsToDollarTest() = measureTest(567.0, 5.67, CentAmerican, USAMoneyGroup.base)

    @Test
    fun euroCentsToEuroTest() = measureTest(567.0, 5.67, CentEuropean, EuroMoneyGroup.base)
}