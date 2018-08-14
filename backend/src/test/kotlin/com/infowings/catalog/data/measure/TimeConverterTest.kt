package com.infowings.catalog.data.measure

import com.infowings.catalog.common.*
import org.junit.jupiter.api.Test

class TimeConverterTest {

    @Test
    fun minuteToSeconds() = toSecondsMeasureTest(2.0, 120.0, Minute)

    @Test
    fun hourToSeconds() = toSecondsMeasureTest(3.5, 12600.0, Hour)

    @Test
    fun daysToSeconds() = toSecondsMeasureTest(1.53, 132192.0, Day)


    private fun toSecondsMeasureTest(source: Double, sourceInSeconds: Double, measure: Measure<DecimalNumber>) =
            measureTest(source, sourceInSeconds, measure, TimeGroup.base)
}