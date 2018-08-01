package com.infowings.catalog.utils

inline fun <T> List<T>.replaceBy(predicate: (T) -> Boolean, newItem: T) = this.map {
    if (predicate(it)) newItem else it
}

inline fun <T> List<T>.mapOn(predicate: (T) -> Boolean, mapFunction: (T) -> T) = this.map {
    if (predicate(it)) mapFunction(it) else it
}

