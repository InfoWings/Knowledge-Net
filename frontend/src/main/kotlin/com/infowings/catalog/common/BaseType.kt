package com.infowings.catalog.common

actual sealed class BaseType actual constructor(_name: String) {

    companion object {
        fun valueOf(name: String): BaseType =
            when (name) {
                Integer.name -> Integer
                Decimal.name -> Decimal
                Boolean.name -> Boolean
                Text.name -> Text
                Range.name -> Range
                Reference.name -> Reference
                else -> throw IllegalArgumentException("No such base type: $name")
            }
    }

    actual object Integer : BaseType("Integer")
    actual object Decimal : BaseType("Decimal")
    actual object Boolean : BaseType("Boolean")
    actual object Text : BaseType("String")
    actual object Range : BaseType("Range")
    actual object Reference : BaseType("Reference")

    actual val name: String = _name
}

fun BaseType.defaultValue(): ObjectValueData? =
    when (this) {
        is BaseType.Text -> null
        is BaseType.Integer -> ObjectValueData.IntegerValue(0, null)
        is BaseType.Decimal -> ObjectValueData.DecimalValue("0")
        is BaseType.Boolean -> ObjectValueData.BooleanValue(false)
        is BaseType.Range -> ObjectValueData.RangeValue(Range(0, 1))
        is BaseType.Reference -> ObjectValueData.NullValue
    }