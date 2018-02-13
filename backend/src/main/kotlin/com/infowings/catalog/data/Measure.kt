package com.infowings.catalog.data

import java.math.BigDecimal


/**
 * Единицы измерения - физическая величина см (https://en.wikipedia.org/wiki/List_of_physical_quantities) или относительное значение]
 * хранить надо нормализованные значения(для физических величин СИ) для упрощения поиска
 */

/**
 * T - тип данных для хранения значение
 * U - тип данных для единиц измерения
 */
sealed class BaseMeasureUnit<T, in U> {

    abstract val linkedTypes: Set<BaseMeasureUnit<*, *>>

    abstract val baseType: BaseType
    /**
     * приводим значение к базовой форме (для физических величин - единицы СИ)
     */
    abstract fun toBase(value: T, unit: U): T

    /**
     * конвертируем значение из базовой формы измерения в указанную (например из мм в метры)
     */
    abstract fun fromBase(value: T, unit: U): T

}


object LengthMeasure : BaseMeasureUnit<BigDecimal, LengthMeasure.LengthUnit>() {
    override val linkedTypes: Set<BaseMeasureUnit<*, *>> by lazy { setOf(SpeedMeasure) }
    override val baseType: BaseType = BaseType.Decimal
    override fun toBase(value: BigDecimal, unit: LengthUnit): BigDecimal = unit.toBase(value)
    override fun fromBase(value: BigDecimal, unit: LengthUnit): BigDecimal = unit.fromBase(value)

    enum class LengthUnit(private val toBaseCoefficient: BigDecimal) {
        Kilometre(BigDecimal(1000)),
        Metre(BigDecimal.ONE),
        Decimetre(BigDecimal(0.1)),
        Centimetre(BigDecimal(0.01)),
        Millimetre(BigDecimal(0.001)),
        Micrometre(BigDecimal(0.000001)),
        Nanometre(BigDecimal(0.000000001)),
        Yard(BigDecimal(0.9144)),
        Mile(BigDecimal(1609.34)),
        Inch(BigDecimal(0.0253999368683));

        fun toBase(value: BigDecimal): BigDecimal = value * toBaseCoefficient
        fun fromBase(value: BigDecimal): BigDecimal = value / toBaseCoefficient
    }

    override fun toString(): String {
        return "LengthMeasure"
    }
}

object SpeedMeasure : BaseMeasureUnit<BigDecimal, SpeedMeasure.SpeedUnit>() {
    override val linkedTypes: Set<BaseMeasureUnit<*, *>> by lazy { setOf(LengthMeasure) }
    override val baseType: BaseType = BaseType.Decimal
    override fun toBase(value: BigDecimal, unit: SpeedUnit): BigDecimal = unit.toBase(value)
    override fun fromBase(value: BigDecimal, unit: SpeedUnit): BigDecimal = unit.fromBase(value)

    enum class SpeedUnit(private val toBaseCoefficient: BigDecimal) {
        KilometrePerSecond(BigDecimal(1000)),
        MilePerHour(BigDecimal(0.44704)),
        InchPerSecond(BigDecimal(0.3048)),
        MetrePerSecond(BigDecimal.ONE),
        KilometrePerHour(BigDecimal(0.277778)),
        Knot(BigDecimal(0.514444));

        fun toBase(value: BigDecimal): BigDecimal = value * toBaseCoefficient
        fun fromBase(value: BigDecimal): BigDecimal = value / toBaseCoefficient
    }

    override fun toString(): String {
        return "SpeedMeasure"
    }
}
//object MassMeasure : BaseMeasureUnit<BigDecimal, MassMeasure.Unit>() {
//    override val baseType: BaseType
//        get() = BaseType.Decimal
//
//    override fun normalize(value: BigDecimal, unit: MassMeasure.Unit): BigDecimal = BigDecimal.ZERO
//    override fun restore(value: BigDecimal, unit: MassMeasure.Unit): BigDecimal = BigDecimal.ZERO
//
//    enum class Unit {
//        Milligram,
//        Microgram,
//        Nanogram,
//        Picogram,
//        Kilogram
//    }
//}


fun restoreMeasureUnit(measureUnit: String?): BaseMeasureUnit<*, *>? =
    when (measureUnit) {
        LengthMeasure.toString() -> LengthMeasure
        SpeedMeasure.toString() -> SpeedMeasure
    //"MassMeasure" -> MassMeasure
        "" -> null
        null -> null
        else -> throw IllegalStateException("Wrong measure unit $measureUnit")
    }

//todo: скопипастить сверху
//object Volume : BaseUnit()

//object Temperature : BaseUnit()
//object Time : BaseUnit()
//object ElectricCurrent : BaseUnit()
//
//todo: min 0, max 1
//object Ratio : BaseUnit()
