package com.infowings.catalog.search

/**
 * Контекст поиска
 */
data class SearchContext(
        var aspects: Array<String>? = null,
        var edgeTypes: Array<String>? = null)
