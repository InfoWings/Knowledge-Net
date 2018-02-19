package com.infowings.catalog.search

import com.infowings.catalog.data.MEASURE_VERTEX
import com.infowings.catalog.storage.OrientDatabase
import com.orientechnologies.orient.core.record.OElement
import com.orientechnologies.orient.core.record.OVertex
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * Сервис поиска в OrientDB
 */
class SuggestionService(val database: OrientDatabase) {

    fun find(user: String, text: String): List<MeasureSuggestionDto> =
            findInDb(user, text)
                    .map { toMeasureSuggestionDto(it) }
                    .collect(Collectors.toList())

    private fun toMeasureSuggestionDto(oElement: OElement?): MeasureSuggestionDto =
            when (oElement) {
                is OVertex -> vertexToMeasureSuggestionDto(oElement)
                else -> throw IllegalStateException("Wrong measure unit $oElement")
            }

    private fun vertexToMeasureSuggestionDto(oVertex: OVertex): MeasureSuggestionDto =
            MeasureSuggestionDto(oVertex.getProperty("name"))

    private fun findInDb(user: String, text: String): Stream<OElement> {
        val q = "SELECT FROM $MEASURE_VERTEX WHERE SEARCH_CLASS(?) = true"
        database.acquire().query(q, "($text~) ($text*) (*$text*)").use {
            return it.elementStream()
        }
    }

}