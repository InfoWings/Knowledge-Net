package com.infowings.catalog.data.history.providers

import java.util.concurrent.ConcurrentHashMap

class HistoryProviderCache<T> {
    // пока такой наивный кеш. Словим OOME - переделаем
    private val cache = ConcurrentHashMap<String, List<T>>()

    fun get(id: String): List<T>? = cache[id]

    fun set(id: String, elems: List<T>) = cache.putIfAbsent(id, elems)
}