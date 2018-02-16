package com.infowings.catalog.data

import java.math.BigDecimal

class Measure<T>(val name: String, val symbol: String, val toBase: (T) -> T, val fromBase: (T) -> T)

class MeasureGroup<T>(val name: String, val measureList: List<Measure<T>>, val base: Measure<T>) {
    val elementGroupMap = measureList.map { it.name to base }.toMap()
}

/** Length group */
val Kilometre = Measure<BigDecimal>("kilometre", "km", { it * BigDecimal(1000) }, { it / BigDecimal(1000) })
val Metre = Measure<BigDecimal>("Metre", "m", { it }, { it })
val Decimetre = Measure<BigDecimal>("Decimetre", "dm", { it * BigDecimal(0.1) }, { it / BigDecimal(0.1) })
val Centimetre = Measure<BigDecimal>("Centimetre", "cm", { it * BigDecimal(0.01) }, { it / BigDecimal(0.01) })
val Millimetre = Measure<BigDecimal>("Millimetre", "millimetre", { it * BigDecimal(0.001) }, { it / BigDecimal(0.001) })
val Micrometre = Measure<BigDecimal>("Micrometre", "micrometre", { it * BigDecimal(0.000001) }, { it / BigDecimal(0.000001) })
val Nanometre = Measure<BigDecimal>("Nanometre", "nanometre", { it * BigDecimal(0.000000001) }, { it / BigDecimal(0.000000001) })
val Yard = Measure<BigDecimal>("Yard", "yard", { it * BigDecimal(0.9144) }, { it / BigDecimal(0.9144) })
val Inch = Measure<BigDecimal>("Inch", "inch", { it * BigDecimal(1609.344) }, { it / BigDecimal(1609.344) })
val Mile = Measure<BigDecimal>("Mile", "mile", { it * BigDecimal(0.0253999368683) }, { it / BigDecimal(0.0253999368683) })

val LengthGroup = MeasureGroup("Length", listOf(Kilometre, Metre, Decimetre, Centimetre, Millimetre, Micrometre, Nanometre, Yard, Inch, Mile), Metre)

/** Speed group */
val KilometrePerSecond = Measure<BigDecimal>("KilometrePerSecond", "km/s", { it * BigDecimal(1000) }, { it / BigDecimal(1000) })
val MilePerHour = Measure<BigDecimal>("MilePerHour", "mile/s", { it * BigDecimal(0.44704) }, { it / BigDecimal(0.44704) })
val InchPerSecond = Measure<BigDecimal>("InchPerSecond", "inch/s", { it * BigDecimal(0.3048) }, { it / BigDecimal(0.3048) })
val MetrePerSecond = Measure<BigDecimal>("MetrePerSecond", "m/s", { it }, { it })
val KilometrePerHour = Measure<BigDecimal>("KilometrePerHour", "km/h", { it * BigDecimal(0.277778) }, { it / BigDecimal(0.277778) })
val Knot = Measure<BigDecimal>("Knot", "knot", { it * BigDecimal(0.514444) }, { it / BigDecimal(0.514444) })

val SpeedGroup = MeasureGroup("Speed", listOf(KilometrePerSecond, MilePerHour, InchPerSecond, MetrePerSecond, KilometrePerHour, Knot), MetrePerSecond)


/** Global */
val MeasureGroupMap = setOf<MeasureGroup<*>>(LengthGroup, SpeedGroup).map { it.name to it }.toMap()
val GlobalMeasureMap = MeasureGroupMap.values.flatMap { it.measureList.map { it.name to it } }.toMap()
