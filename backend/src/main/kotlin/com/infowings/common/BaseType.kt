package com.infowings.common

import java.math.BigDecimal
import kotlin.reflect.KClass


actual sealed class BaseType actual constructor(val name: String) {

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

    actual object Nothing : BaseType("Composite Aspect")
    actual object Integer : BaseType("Integer")
    actual object Long : BaseType("Long")
    actual object Decimal : BaseType("Decimal")
    actual object Boolean : BaseType("Boolean")
    actual object Text : BaseType("String")
    actual object Binary : BaseType("Binary")
}