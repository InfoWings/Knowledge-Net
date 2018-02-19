package com.infowings.catalog.data

import com.infowings.catalog.data.BaseType.Directory
import java.math.BigDecimal
import kotlin.reflect.KClass


/**
 * низкоуровневый тип данных в котором хранится значение свойства, на уровне БД это не обязательно тот же тип (см. [Directory]
 **/
sealed class BaseType(val name: String) {

    object Nothing : BaseType("Composite Aspect")
    object Integer : BaseType("Integer")
    object Long : BaseType("Long")
    object Decimal : BaseType("Decimal")
    object Boolean : BaseType("Boolean")
    object Text : BaseType("String")
    object Binary : BaseType("Binary")

    //todo:
    class Directory(val dName: String) : BaseType("") // временно

    class Link : BaseType("")

    companion object {
        fun restoreBaseType(name: String?): BaseType =
            when (name) {
                null -> BaseType.Nothing
                BaseType.Nothing.name -> BaseType.Nothing
                BaseType.Integer.name -> BaseType.Integer
                BaseType.Long.name -> BaseType.Long
                BaseType.Decimal.name -> BaseType.Decimal
                BaseType.Boolean.name -> BaseType.Boolean
                BaseType.Text.name -> BaseType.Text
                BaseType.Binary.name -> BaseType.Binary

                else -> TODO("реализовать хранение сложных типов")
            }

        fun getTypeClass(name: String): KClass<*> =
            when (name) {
                BaseType.Integer.name -> Int::class
                BaseType.Long.name -> Long::class
                BaseType.Decimal.name -> BigDecimal::class
                BaseType.Boolean.name -> Boolean::class
                BaseType.Text.name -> String::class
                BaseType.Binary.name -> ByteArray::class

                else -> TODO("реализовать хранение сложных типов")

            }
    }

    override fun toString(): String {
        return "BaseType($name)"
    }
}

enum class AspectPropertyPower {
    ZERO, ONE, INFINITY
}

/**
 * Аспект - https://iwings.atlassian.net/wiki/spaces/CHR/pages/219217938
 */
data class Aspect(
    val id: String,
    val name: String,
    val measure: Measure<*>?,
    val domain: AspectDomain? = OpenDomain(measure?.baseType ?: throw IllegalArgumentException("Measure unit cannot be null if no base type specified")),
    val baseType: BaseType? = measure?.baseType ?: throw IllegalArgumentException("Measure unit cannot be null if no base type specified"),
    val properties: List<AspectProperty> = emptyList()
)

data class AspectProperty(
    val id: String,
    val name: String,
    val aspect: Aspect,
    val power: AspectPropertyPower
)