package com.infowings.catalog.search

import com.infowings.catalog.data.MEASURE_VERTEX
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.session
import com.orientechnologies.orient.core.record.OElement
import com.orientechnologies.orient.core.record.OVertex
import com.orientechnologies.orient.core.record.impl.ODocument
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * Сервис поиска в OrientDB
 */
class SuggestionService(val database: OrientDatabase) {

    fun find(context: SearchContext, text: String): List<MeasureSuggestion> = session(database) {
            findInDb(context, text)
                    .map { toMeasureSuggestionDto(it) }
                    .toList()
    }

    private fun toMeasureSuggestionDto(oElement: OElement?): MeasureSuggestion =
            when (oElement) {
                is OVertex -> vertexToMeasureSuggestionDto(oElement)
                is ODocument -> vertexToMeasureSuggestionDto(oElement)
                else -> throw IllegalStateException("Wrong measure unit $oElement")
            }

    private fun vertexToMeasureSuggestionDto(oVertex: OVertex): MeasureSuggestion =
            MeasureSuggestion(oVertex.getProperty("name"))

    private fun vertexToMeasureSuggestionDto(oDocument: ODocument): MeasureSuggestion =
            MeasureSuggestion(oDocument.getProperty("name"))

    private fun findInDb(context: SearchContext, text: String): Sequence<OElement> {
        val q = "SELECT FROM $MEASURE_VERTEX WHERE SEARCH_CLASS(?) = true"
        return database.query(q, "($text~) ($text*) (*$text*)") {
            it.map { it.toElement() }
        }

    }
}