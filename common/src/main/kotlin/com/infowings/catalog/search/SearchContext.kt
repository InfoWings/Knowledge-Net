package com.infowings.catalog.search

const val SEARCH_RES_LIMIT: Int = 100
const val FUZZY_MIN_DIST: Double = 1.0

data class SearchContext(
    /**
     * ID of current aspect
     */
    var aspectId: String? = null,

    /**
     * ID of current aspect property
     */
    var aspectPropertyId: String? = null
)

data class CommonSuggestionParam(
    /**
     * part of name for fulltext search
     */
    var text: String = "",

    /**
     *  maximum number of records in the result
     */
    var limit: Int? = SEARCH_RES_LIMIT,

    /**
     * minimum Lucene score
     */
    var minScore: Double? = FUZZY_MIN_DIST
)

data class AspectSuggestionParam(
    /**
     * part of name of measure for fulltext search
     */
    var measureText: String? = null,

    /**
     * exactly name of measure
     */
    var measureName: String? = null
)
