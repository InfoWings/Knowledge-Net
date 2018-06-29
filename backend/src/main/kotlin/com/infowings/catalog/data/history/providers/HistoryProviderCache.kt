package com.infowings.catalog.data.history.providers

import java.util.concurrent.ConcurrentHashMap

class HistoryProviderCache<T> {
    private val cache = ConcurrentHashMap<String, List<T>>()

}