package com.infowings.catalog.common

actual sealed class BaseType actual constructor(_name: String) {

    actual object Integer : BaseType("Integer")
    actual object Decimal : BaseType("Decimal")
    actual object Boolean : BaseType("Boolean")
    actual object Text : BaseType("String")

    actual val name: String = _name
}