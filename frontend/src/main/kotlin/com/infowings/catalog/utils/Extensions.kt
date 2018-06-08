package com.infowings.catalog.utils

inline fun <T> List<T>.replaceBy(newItem: T, predicate: (T) -> Boolean) = this.map {
    if (predicate(it)) newItem else it
}

