package com.infowings.catalog.common

actual sealed class BaseType actual constructor(_name: String) {

    actual object Nothing : BaseType("Composite Aspect")
    actual object Integer : BaseType("Integer")
    actual object Long : BaseType("Long")
    actual object Decimal : BaseType("Decimal")
    actual object Boolean : BaseType("Boolean")
    actual object Text : BaseType("String")
    actual object Binary : BaseType("Binary")

    actual val name: String = _name
}