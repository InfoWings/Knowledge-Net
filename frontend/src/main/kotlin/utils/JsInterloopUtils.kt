package utils

import kotlinext.js.Object
import kotlinext.js.js
import react.RElementBuilder
import react.RProps

fun <T : RProps> RElementBuilder<T>.plainObj(handler: T.() -> Unit) {
    attrs.handler()
    for (key in Object.keys(attrs)) {
        val value = attrs.asDynamic()[key]
        attrs.asDynamic()[key] = plain(value)
    }
}

fun isPrimitive(value: Any): Boolean {
    return value is Number || value is String || value is Boolean || value is Char
}

fun plain(obj: Any?): dynamic {
    val assigned: MutableSet<Any> = mutableSetOf()

    fun toPlain(obj: Any?): dynamic =
            if (obj == null || assigned.contains(obj) || isPrimitive(obj)) obj
            else if (obj is Map<*, *>) {
                val result = js("{}")
                for (entry in obj.entries) {
                    result[entry.key] = toPlain(entry.value)
                }
                result
            } else if (obj is Collection<*>) {
                val newArray = Array(obj.size) {}
                var i = 0
                for (elem in obj) {
                    newArray[i++] = toPlain(elem)
                }
                newArray
            } else if (obj is Array<*>) {
                val newArray = Array(obj.size) {}
                var i = 0
                for (elem in obj) {
                    newArray[i++] = toPlain(elem)
                }
                newArray
            } else js {
                assigned.add(obj)
                for (key in Object.keys(obj)) {
                    val value = obj.asDynamic()[key]
                    this[key] = toPlain(value)
                }
            }

    return toPlain(obj)
}
