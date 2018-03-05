package com.infowings.catalog.common

import java.math.BigDecimal
import kotlin.reflect.KClass


actual sealed class BaseType actual constructor(_name: String) {

    actual val name = _name

    //todo:
    class Directory(val dName: String) : BaseType("") // временно

    class Link : BaseType("")

    companion object {
        fun restoreBaseType(name: String?): BaseType =
            when (name) {
                null -> Nothing
                Nothing.name -> Nothing
                Integer.name -> Integer
                Long.name -> Long
                Decimal.name -> Decimal
                Boolean.name -> Boolean
                Text.name -> Text
                Binary.name -> Binary

                else -> TODO("реализовать хранение сложных типов")
            }

        fun getTypeClass(name: String): KClass<*> =
            when (name) {
                Integer.name -> Int::class
                Long.name -> Long::class
                Decimal.name -> BigDecimal::class
                Boolean.name -> Boolean::class
                Text.name -> String::class
                Binary.name -> ByteArray::class

                else -> TODO("реализовать хранение сложных типов")

            }
    }

    override fun toString(): String {
        return "BaseType($name)"
    }

    actual object Nothing : BaseType("Composite Aspect")
    actual object Integer : BaseType("Integer")
    actual object Long : BaseType("Long")
    actual object Decimal : BaseType("Decimal")
    actual object Boolean : BaseType("Boolean")
    actual object Text : BaseType("String")
    actual object Binary : BaseType("Binary")
}