package com.infowings.catalog.search

const val SEARCH_RES_LIMIT: Int = 100
const val FUZZY_MIN_DIST: Double = 1.0

/**
 * Search context data
 *
 * @param aspectId - ID of current aspect
 * @param aspectPropertyId - ID of current aspect property
 * @param subjectId - ID of current subject
 */
data class SearchContext(
    var aspectId: String? = null,
    var aspectPropertyId: String? = null,
    var subjectId: String? = null
)

/**
 * Common suggestion request data
 *
 * @param text - part of name for fulltext search
 * @param limit - maximum number of records in the result
 * @param minScore - minimum Lucene score
 */
data class CommonSuggestionParam(
    var text: String = "",
    var limit: Int? = SEARCH_RES_LIMIT,
    var minScore: Double? = FUZZY_MIN_DIST
)

/**
 * Additional params of aspect suggestion
 *
 * @param measureText - part of name of measure for fulltext search
 * @param measureName - exactly name of measure
 */
data class AspectSuggestionParam(
    var measureText: String? = null,
    var measureName: String? = null
)

/**
 * Additional params of subject suggestion
 *
 * @param measureText - part of name of aspect for fulltext search
 */
data class SubjectSuggestionParam(
    var aspectText: String? = null
)
