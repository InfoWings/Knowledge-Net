package com.infowings.catalog.search

import com.infowings.catalog.data.MEASURE_VERTEX
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.session
import com.infowings.catalog.storage.toVertexOrNUll
import com.orientechnologies.orient.core.record.OElement
import com.orientechnologies.orient.core.record.OVertex

/**
 * Сервис поиска в OrientDB
 */
class SuggestionService(val database: OrientDatabase) {

    private val q = "SELECT FROM $MEASURE_VERTEX WHERE SEARCH_CLASS(?) = true"

    fun find(context: SearchContext, text: String): List<MeasureSuggestion> = session(database) {
        findInDb(context, text)
                .map { toMeasureSuggestionDto(it) }
                .toList()
    }

    private fun toMeasureSuggestionDto(oVertex: OVertex): MeasureSuggestion =
            MeasureSuggestion(oVertex.getProperty("name"))

    private fun findInDb(context: SearchContext, text: String): Sequence<OVertex> =
            database.query(q, "($text~) ($text*) (*$text*)") {
                it.mapNotNull { it.toVertexOrNUll() }
            }
}

/**
 * DTO сервиса поиска
 */
data class MeasureSuggestion(
        val name: String)

/**
 * Контекст поиска
 */
data class SearchContext(
        val aspects: List<String>,
        val edgeTypes: List<String>)