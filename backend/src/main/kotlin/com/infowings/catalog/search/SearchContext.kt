package com.infowings.catalog.search

data class SearchContext(
        val user: String,
        val aspects: List<String>,
        val edgeTypes: List<String>)