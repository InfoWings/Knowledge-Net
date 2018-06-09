package com.infowings.catalog.common

import java.math.BigDecimal
import kotlin.reflect.KClass


actual sealed class BaseType actual constructor(_name: String) {

    actual val name = _name

    companion object {
        fun restoreBaseType(name: String?): BaseType =
            when (name) {
                Integer.name -> Integer
                Decimal.name -> Decimal
                Boolean.name -> Boolean
                Text.name -> Text
                else -> TODO("Base type $name is not supported")
            }

        fun getTypeClass(name: String): KClass<*> =
            when (name) {
                Integer.name -> Int::class
                Decimal.name -> BigDecimal::class
                Boolean.name -> Boolean::class
                Text.name -> String::class
                else -> TODO("Base type $name is not supported")
            }
    }

    override fun toString(): String {
        return "BaseType($name)"
    }

    actual object Integer : BaseType("Integer")
    actual object Decimal : BaseType("Decimal")
    actual object Boolean : BaseType("Boolean")
    actual object Text : BaseType("String")
}