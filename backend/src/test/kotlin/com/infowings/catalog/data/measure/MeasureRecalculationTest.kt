package com.infowings.catalog.data.measure

import com.infowings.catalog.common.*
import com.infowings.catalog.data.objekt.ObjectService
import com.infowings.catalog.data.objekt.RecalculationException
import io.kotlintest.Matcher
import io.kotlintest.Result
import io.kotlintest.should
import io.kotlintest.shouldThrow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.math.BigDecimal

@ExtendWith(SpringExtension::class)
@SpringBootTest
class MeasureRecalculationTest {

    @Autowired
    lateinit var objectService: ObjectService

    val beRelativelyEqualTo = relativelyEqualWithDelta(allowedRelativeDelta)

    @Test
    fun `Recalculation from metre to yard with right parameters results in correct result`() {
        val metreValue = "10"
        val expectedYardValue = "10.936133"

        val resultYardValue = objectService.recalculateValue(Metre.name, Yard.name, DecimalNumber(metreValue))
        resultYardValue should beRelativelyEqualTo(expectedYardValue)
    }

    @Test
    fun `Recalculation from yard to metre with right parameters results in correct result`() {
        val yardValue = "10"
        val expectedMetreValue = "9.144"

        val resultMetreValue = objectService.recalculateValue(Yard.name, Metre.name, DecimalNumber(yardValue))
        resultMetreValue should beRelativelyEqualTo(expectedMetreValue)
    }

    @Test
    fun `Recalculation from celsius to fahrenheit with right parameters results in correct result`() {
        val celsiusValue = "37.7"
        val expectedFahrenheitValue = "99.86"

        val resultFahrenheitValue = objectService.recalculateValue(Celsius.name, Fahrenheit.name, DecimalNumber(celsiusValue))
        resultFahrenheitValue should beRelativelyEqualTo(expectedFahrenheitValue)
    }

    @Test
    fun `Recalculation from fahrenheit to celsius with right parameters results in correct result`() {
        val fahrenheitValue = "88.42"
        val expectedCelsiusValue = "31.344444"

        val resultCelsiusValue = objectService.recalculateValue(Fahrenheit.name, Celsius.name, DecimalNumber(fahrenheitValue))
        resultCelsiusValue should beRelativelyEqualTo(expectedCelsiusValue)
    }

    @Test
    fun `Recalculation from yard to inch with right parameters results in correct result`() {
        val yardValue = "50.8"
        val expectedInchValue = "1828.8"

        val resultInchValue = objectService.recalculateValue(Yard.name, Inch.name, DecimalNumber(yardValue))
        resultInchValue should beRelativelyEqualTo(expectedInchValue)
    }

    @Test
    fun `Recalculation from inch to yard with right parameters results in incorrect result`() {
        val inchValue = "50.8"
        val expectedYardValue = "1.411111"

        val resultYardValue = objectService.recalculateValue(Inch.name, Yard.name, DecimalNumber(inchValue))
        resultYardValue should beRelativelyEqualTo(expectedYardValue)
    }

    @Test
    fun `Recalculation from celsius to yard should result in thrown exception`() {
        val celsiusValue = "36.6"

        shouldThrow<RecalculationException> {
            objectService.recalculateValue(Celsius.name, Yard.name, DecimalNumber(celsiusValue))
        }
    }

}

const val allowedRelativeDelta = "0.0001"

class DecimalNumberRelativelyEqualMatcher(relativeDelta: BigDecimal, private val targetValue: BigDecimal) : Matcher<DecimalNumber> {

    private val relativeDeltaAbs: BigDecimal = relativeDelta.abs()

    constructor(relativeDelta: String, targetValue: String) : this(BigDecimal(relativeDelta), BigDecimal(targetValue))

    override fun test(value: DecimalNumber): Result {
        val bigDecimalValue = value.value
        val absoluteDelta = bigDecimalValue.abs().multiply(relativeDeltaAbs)
        val upperBound = bigDecimalValue.plus(absoluteDelta)
        val lowerBound = bigDecimalValue.minus(absoluteDelta)
        return Result(
            targetValue in lowerBound..upperBound,
            "$targetValue is not in allowed bounds [$lowerBound .. $upperBound] of $value",
            "$targetValue is too close to $value"
        )
    }

}

fun relativelyEqualWithDelta(relativeDelta: String) = { targetValue: String ->
    DecimalNumberRelativelyEqualMatcher(relativeDelta, targetValue)
}