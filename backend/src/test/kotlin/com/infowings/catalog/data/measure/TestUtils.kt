//package com.infowings.catalog.data.measure
//
//import com.infowings.catalog.data.BaseMeasureUnit
//import com.infowings.catalog.data.LengthMeasure
//import com.infowings.catalog.data.SpeedMeasure
//import org.junit.Assert
//import java.math.BigDecimal
//import java.math.RoundingMode
//
//inline fun <reified T: BaseMeasureUnit<*, *>> assertEquals(message: String, expected: BigDecimal, actual: BigDecimal) {
//    val scaleFun = when (T::class) {
//        LengthMeasure::class -> LengthMeasure.recommendedScaleFun
//        SpeedMeasure::class -> SpeedMeasure.recommendedScaleFun
//        else -> {x -> x}
//    }
//    val scaledActual = scaleFun(actual)
//    val scaledExpected = scaleFun(expected)
//    if (scaleFun(scaledActual - scaleFun(actual)).signum() != 0) {
//        Assert.fail("$message, actual:$scaledActual, expected: $scaledExpected")
//    }
//}