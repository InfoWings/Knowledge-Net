package com.infowings.common

actual sealed class BaseType actual constructor(name: String) {
    actual object Nothing : BaseType("Composite Aspect")
    actual object Integer : BaseType("Integer")
    actual object Long : BaseType("Long")
    actual object Decimal : BaseType("Decimal")
    actual object Boolean : BaseType("Boolean")
    actual object Text : BaseType("String")
    actual object Binary : BaseType("Binary")
}