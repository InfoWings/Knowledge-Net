package com.infowings.catalog.data

import java.math.BigDecimal

class Measure<T>(val name: String, val symbol: String, val toBase: (T) -> T, val fromBase: (T) -> T, val baseType: BaseType)

class MeasureGroup<T>(val name: String, val measureList: List<Measure<T>>, val base: Measure<T>) {
    val elementGroupMap = measureList.map { it.name to base }.toMap()
}
private fun <T> identity(): (T) -> T = { it }


/** Length group */
val Kilometre = Measure<BigDecimal>("Kilometre", "km", { it * BigDecimal(1000) }, { it / BigDecimal(1000) }, BaseType.Decimal)
val Metre = Measure<BigDecimal>("Metre", "m", identity(), identity(), BaseType.Decimal)
val Decimetre = Measure<BigDecimal>("Decimetre", "dm", { it * BigDecimal(0.1) }, { it / BigDecimal(0.1) }, BaseType.Decimal)
val Centimetre = Measure<BigDecimal>("Centimetre", "cm", { it * BigDecimal(0.01) }, { it / BigDecimal(0.01) }, BaseType.Decimal)
val Millimetre = Measure<BigDecimal>("Millimetre", "millimetre", { it * BigDecimal(0.001) }, { it / BigDecimal(0.001) }, BaseType.Decimal)
val Micrometre = Measure<BigDecimal>("Micrometre", "micrometre", { it * BigDecimal(0.000001) }, { it / BigDecimal(0.000001) }, BaseType.Decimal)
val Nanometre = Measure<BigDecimal>("Nanometre", "nanometre", { it * BigDecimal(0.000000001) }, { it / BigDecimal(0.000000001) }, BaseType.Decimal)
val Yard = Measure<BigDecimal>("Yard", "yard", { it * BigDecimal(0.9144) }, { it / BigDecimal(0.9144) }, BaseType.Decimal)
val Inch = Measure<BigDecimal>("Inch", "inch", { it * BigDecimal(1609.344) }, { it / BigDecimal(1609.344) }, BaseType.Decimal)
val Mile = Measure<BigDecimal>("Mile", "mile", { it * BigDecimal(0.0253999368683) }, { it / BigDecimal(0.0253999368683) }, BaseType.Decimal)

val LengthGroup = MeasureGroup("Length", listOf(Kilometre, Metre, Decimetre, Centimetre, Millimetre, Micrometre, Nanometre, Yard, Inch, Mile), Metre)

/** Speed group */

val KilometrePerSecond = Measure<BigDecimal>("Kilometres per Second", "km/s", { it * BigDecimal(1000) }, { it / BigDecimal(1000) }, BaseType.Decimal)
val MilePerHour = Measure<BigDecimal>("Miles per Hour", "mile/h", { it * BigDecimal(0.44704) }, { it / BigDecimal(0.44704) }, BaseType.Decimal)
val InchPerSecond = Measure<BigDecimal>("Inches per Second", "inch/s", { it * BigDecimal(0.3048) }, { it / BigDecimal(0.3048) }, BaseType.Decimal)
val MetrePerSecond = Measure<BigDecimal>("Metres per Second", "m/s", identity(), identity(), BaseType.Decimal)

val KilometrePerHour = Measure<BigDecimal>("Kilometres per Hour", "km/h", { it * BigDecimal(0.277778) }, { it / BigDecimal(0.277778) }, BaseType.Decimal)
val Knot = Measure<BigDecimal>("Knot", "knot", { it * BigDecimal(0.514444) }, { it / BigDecimal(0.514444) }, BaseType.Decimal)

val SpeedGroup = MeasureGroup("Speed", listOf(KilometrePerSecond, MilePerHour, InchPerSecond, MetrePerSecond, KilometrePerHour, Knot), MetrePerSecond)


/** Global */
val MeasureGroupMap = setOf<MeasureGroup<*>>(LengthGroup, SpeedGroup).map { it.name to it }.toMap()
val GlobalMeasureMap: Map<String, Measure<*>> = MeasureGroupMap.values.flatMap { it.measureList.map { it.name to it } }.toMap()
