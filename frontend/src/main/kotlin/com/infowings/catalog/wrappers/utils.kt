package com.infowings.catalog.wrappers

import kotlinext.js.Object
import kotlinext.js.toPlainObjectStripNull

external class TypedJsObject<T>

fun <T> TypedJsObject<T>.toMap(): Map<String, T> {
    val resultMap = mutableMapOf<String, T>()
    val that = toPlainObjectStripNull(this)

    for (key in Object.keys(that)) {
        val value = this.asDynamic()[key]
        resultMap[key] = value as T
    }
    return resultMap
}