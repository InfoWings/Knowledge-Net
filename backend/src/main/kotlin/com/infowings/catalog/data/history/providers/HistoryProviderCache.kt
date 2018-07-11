package com.infowings.catalog.data.history.providers

import java.util.concurrent.ConcurrentHashMap

interface HistoryProviderCache<T> {
    fun get(id: String): List<T>?

    fun set(id: String, elems: List<T>)
}

class CHMHistoryProviderCache<T> : HistoryProviderCache<T> {
    // пока такой наивный кеш. Словим OOME - переделаем
    private val cache = ConcurrentHashMap<String, List<T>>()

    override fun get(id: String): List<T>? = cache[id]

    override fun set(id: String, elems: List<T>) {
        cache.putIfAbsent(id, elems)
    }
}