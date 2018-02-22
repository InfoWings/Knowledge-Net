package com.infowings.common.search

/**
 * Контекст поиска
 */
data class SearchContext(
        val aspects: List<String>,
        val edgeTypes: List<String>)