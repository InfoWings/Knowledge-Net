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
sealed class BaseMeasureUnit<T, U> {
    abstract val baseType: BaseType
    /**
     * приводим значение к нормальной форме (для физических величин - единицы СИ)
     */
    abstract fun normalize(value: T, unit: U): T

    /**
     * конвертируем значение в заданное измерение (например из мм в метры)
     */
    abstract fun restore(value: T, unit: U): T

}


//todo: конверсия в СИ
object LengthMeasure : BaseMeasureUnit<BigDecimal, LengthMeasure.Unit>() {
    override val baseType: BaseType
        get() = BaseType.Decimal

    override fun normalize(value: BigDecimal, unit: Unit): BigDecimal = BigDecimal.ZERO
    override fun restore(value: BigDecimal, unit: Unit): BigDecimal = BigDecimal.ZERO

    enum class Unit {
        Kilometre,
        Decimetre,
        Centimetre,
        Millimetre,
        Micrometre,
        Nanometre;

        override fun toString(): String {
            return "LengthMeasure"
        }
    }
}

object MassMeasure : BaseMeasureUnit<BigDecimal, MassMeasure.Unit>() {
    override val baseType: BaseType
        get() = BaseType.Decimal

    override fun normalize(value: BigDecimal, unit: MassMeasure.Unit): BigDecimal = BigDecimal.ZERO
    override fun restore(value: BigDecimal, unit: MassMeasure.Unit): BigDecimal = BigDecimal.ZERO

    enum class Unit {
        Milligram,
        Microgram,
        Nanogram,
        Picogram,
        Kilogram
    }
}


fun restoreMeasureUnit(measureUnit: String?): BaseMeasureUnit<*, *>? =
    when (measureUnit) {
        "LengthMeasure" -> LengthMeasure
        "MassMeasure" -> MassMeasure
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
