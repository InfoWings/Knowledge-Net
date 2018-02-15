package com.infowings.catalog.data

import java.math.BigDecimal
import java.util.stream.Collectors

class Measure<T>(val name: String, val symbol: String, val toBase: (T) -> T, val fromBase: (T) -> T)

class MeasureGroup<T>(val name: String, val measureList: List<Measure<T>>, val base: Measure<T>) {
    val elemGroupMap = measureList.stream().collect(Collectors.toMap({ e: Measure<T> -> e }, { e -> base }))
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
//        KilometrePerSecond(BigDecimal(1000)),
//        MilePerHour(BigDecimal(0.44704)),
//        InchPerSecond(BigDecimal(0.3048)),
//        MetrePerSecond(BigDecimal.ONE),
//        KilometrePerHour(BigDecimal(0.277778)),
//        Knot(BigDecimal(0.514444));

val KilometrePerSecond = Measure<BigDecimal>("KilometrePerSecond", "km/s", { it * BigDecimal(1000) }, { it / BigDecimal(1000) })
val MilePerHour = Measure<BigDecimal>("MilePerHour", "mile/s", { it * BigDecimal(0.44704) }, { it / BigDecimal(0.44704) })
val InchPerSecond = Measure<BigDecimal>("InchPerSecond", "inch/s", { it * BigDecimal(0.3048) }, { it / BigDecimal(0.3048) })
val MetrePerSecond = Measure<BigDecimal>("MetrePerSecond", "m/s", { it }, { it })
val KilometrePerHour = Measure<BigDecimal>("KilometrePerHour", "km/h", { it * BigDecimal(0.277778) }, { it / BigDecimal(0.277778) })
val Knot = Measure<BigDecimal>("Knot", "knot", { it * BigDecimal(0.514444) }, { it / BigDecimal(0.514444) })

val SpeedGroup = MeasureGroup("Speed", listOf(KilometrePerSecond, MilePerHour, InchPerSecond, MetrePerSecond, KilometrePerHour, Knot), MetrePerSecond)

///**
// * Единицы измерения - физическая величина см (https://en.wikipedia.org/wiki/List_of_physical_quantities) или относительное значение]
// * хранить надо нормализованные значения(для физических величин СИ) для упрощения поиска
// */
//
///**
// * T - тип данных для хранения значение
// * U - тип данных для единиц измерения
// */
//sealed class BaseMeasureUnit<T, in U> {
//
//    abstract val linkedTypes: Set<BaseMeasureUnit<*, *>>
//
//    abstract val baseType: BaseType
//    /**
//     * приводим значение к базовой форме (для физических величин - единицы СИ)
//     */
//    abstract fun toBase(value: T, unit: U): T
//
//    /**
//     * конвертируем значение из базовой формы измерения в указанную (например из мм в метры)
//     */
//    abstract fun fromBase(value: T, unit: U): T
//
//    abstract fun name(): String
//}
//
//
//object LengthMeasure : BaseMeasureUnit<BigDecimal, LengthMeasure.Unit>() {
//    override val linkedTypes: Set<BaseMeasureUnit<*, *>> by lazy { setOf(SpeedMeasure) }
//    override val baseType: BaseType = BaseType.Decimal
//    override fun toBase(value: BigDecimal, unit: Unit): BigDecimal = unit.toBase(value)
//    override fun fromBase(value: BigDecimal, unit: Unit): BigDecimal = unit.fromBase(value)
//    val recommendedScaleFun = { x: BigDecimal -> x.setScale(5, RoundingMode.HALF_EVEN) }
//
//    enum class Unit(private val toBaseCoefficient: BigDecimal) {
//        Kilometre(BigDecimal(1000)),
//        Metre(BigDecimal.ONE),
//        Decimetre(BigDecimal(0.1)),
//        Centimetre(BigDecimal(0.01)),
//        Millimetre(BigDecimal(0.001)),
//        Micrometre(BigDecimal(0.000001)),
//        Nanometre(BigDecimal(0.000000001)),
//        Yard(BigDecimal(0.9144)),
//        Mile(BigDecimal(1609.344)),
//        Inch(BigDecimal(0.0253999368683));
//
//        fun toBase(value: BigDecimal): BigDecimal = value * toBaseCoefficient
//        fun fromBase(value: BigDecimal): BigDecimal = value / toBaseCoefficient
//    }
//
//    override fun name(): String {
//        return "LengthMeasure"
//    }
//}
//
//object SpeedMeasure : BaseMeasureUnit<BigDecimal, SpeedMeasure.Unit>() {
//    override val linkedTypes: Set<BaseMeasureUnit<*, *>> by lazy { setOf(LengthMeasure) }
//    override val baseType: BaseType = BaseType.Decimal
//    override fun toBase(value: BigDecimal, unit: Unit): BigDecimal = unit.toBase(value)
//    override fun fromBase(value: BigDecimal, unit: Unit): BigDecimal = unit.fromBase(value)
//    val recommendedScaleFun = { x: BigDecimal -> x.setScale(5, RoundingMode.HALF_EVEN) }
//
//    enum class Unit(private val toBaseCoefficient: BigDecimal) {
//        KilometrePerSecond(BigDecimal(1000)),
//        MilePerHour(BigDecimal(0.44704)),
//        InchPerSecond(BigDecimal(0.3048)),
//        MetrePerSecond(BigDecimal.ONE),
//        KilometrePerHour(BigDecimal(0.277778)),
//        Knot(BigDecimal(0.514444));
//
//        fun toBase(value: BigDecimal): BigDecimal = value * toBaseCoefficient
//        fun fromBase(value: BigDecimal): BigDecimal = value / toBaseCoefficient
//    }
//
//    override fun name(): String {
//        return "SpeedMeasure"
//    }
//}
////object MassMeasure : BaseMeasureUnit<BigDecimal, MassMeasure.Unit>() {
////    override val baseType: BaseType
////        get() = BaseType.Decimal
////
////    override fun normalize(value: BigDecimal, unit: MassMeasure.Unit): BigDecimal = BigDecimal.ZERO
////    override fun restore(value: BigDecimal, unit: MassMeasure.Unit): BigDecimal = BigDecimal.ZERO
////
////    enum class Unit {
////        Milligram,
////        Microgram,
////        Nanogram,
////        Picogram,
////        Kilogram
////    }
////}
//
//
//fun restoreMeasureUnit(measureUnit: String?): BaseMeasureUnit<*, *>? =
//    when (measureUnit) {
//        LengthMeasure.name() -> LengthMeasure
//        SpeedMeasure.name() -> SpeedMeasure
//    //"MassMeasure" -> MassMeasure
//        "" -> null
//        null -> null
//        else -> throw IllegalStateException("Wrong measure unit $measureUnit")
//    }
//
////todo: скопипастить сверху
////object Volume : BaseUnit()
//
////object Temperature : BaseUnit()
////object Time : BaseUnit()
////object ElectricCurrent : BaseUnit()
////
////todo: min 0, max 1
////object Ratio : BaseUnit()
