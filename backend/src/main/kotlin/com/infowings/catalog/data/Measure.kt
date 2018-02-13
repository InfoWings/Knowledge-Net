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


//todo: конверсия в СИ
object LengthMeasure : BaseMeasureUnit<BigDecimal, LengthMeasure.Unit>() {
    override val baseType: BaseType
        get() = BaseType.Decimal

    override fun toBase(value: BigDecimal, unit: Unit): BigDecimal = unit.toBase(value)
    override fun fromBase(value: BigDecimal, unit: Unit): BigDecimal = unit.fromBase(value)

    enum class Unit(private val toBaseCoefficient: BigDecimal) {
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

        override fun toString(): String {
            return "LengthMeasure"
        }

        fun toBase(value: BigDecimal): BigDecimal = value * toBaseCoefficient
        fun fromBase(value: BigDecimal): BigDecimal = value / toBaseCoefficient
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
        "LengthMeasure" -> LengthMeasure
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
