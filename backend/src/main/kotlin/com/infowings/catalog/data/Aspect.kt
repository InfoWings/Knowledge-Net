package com.infowings.catalog.data

import com.infowings.catalog.Property
import com.infowings.catalog.data.BaseType.Directory
import java.math.BigDecimal
import kotlin.reflect.KClass


/**
 * низкоуровневый тип данных в котором хранится значение свойства, на уровне БД это не обязательно тот же тип (см. [Directory]
 *
 * todo: для сложных типов поле [clazz] == null надо добавить/переработать проверки для сложных типов
 *
 */
sealed class BaseType(val name: String, val clazz: KClass<*>?) {

    object Nothing : BaseType("Composite Aspect", Any::class)

    object Integer : BaseType("Integer", Int::class)
    object Long : BaseType("Long", Long::class)
    object Decimal : BaseType("Decimal", BigDecimal::class)
    object Boolean : BaseType("Boolean", kotlin.Boolean::class)
    object Text : BaseType("String", String::class)
    object Binary : BaseType("Binary", ByteArray::class)

    //todo:
    class Directory(val dName: String) : BaseType("", null) // временно

    class Link : BaseType("", null)

    companion object {
        fun restoreBaseType(name: String?): BaseType =
            when (name) {
                null -> BaseType.Nothing
                BaseType.Integer.name -> BaseType.Integer
                BaseType.Long.name -> BaseType.Long
                BaseType.Decimal.name -> BaseType.Decimal
                BaseType.Boolean.name -> BaseType.Boolean
                BaseType.Text.name -> BaseType.Text
                BaseType.Binary.name -> BaseType.Binary


                else -> TODO("реализовать хранение сложных типов")
            }
    }
}


/**
 * Аспект - https://iwings.atlassian.net/wiki/spaces/CHR/pages/219217938
 */
data class Aspect(
    val id: String,
    val name: String,
    val measureUnit: BaseMeasureUnit<*, *>?,
    val domain: AspectDomain? = OpenDomain(measureUnit?.baseType ?: throw IllegalArgumentException("Measure unit cannot be null if no base type specified")),
    val baseType: BaseType? = measureUnit?.baseType ?: throw IllegalArgumentException("Measure unit cannot be null if no base type specified")
)

// todo: должны ли самые базовые типы лежать в БД или в коде?
//val Length = Aspect(1, "Length", LengthMeasure, OpenDomain(LengthMeasure.baseType))
//val Mass = Aspect(2, "Mass", MassMeasure, OpenDomain(MassMeasure.baseType))
//
//val RawString = Aspect(3, "String", null, OpenDomain(BaseType.Text), BaseType.Text)
