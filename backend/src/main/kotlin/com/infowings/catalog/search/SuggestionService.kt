package com.infowings.catalog.search

import com.infowings.catalog.storage.MEASURE_VERTEX_CLASS
import com.infowings.catalog.storage.OrientDatabase
import com.orientechnologies.orient.core.record.OElement
import com.orientechnologies.orient.core.record.OVertex
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * Сервис поиска в OrientDB
 */
@Service
class SuggestionService {

    @Autowired
    lateinit var database: OrientDatabase

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

    private fun findInDb(user: String, text: String): Stream<OElement> =
            database.acquire().query("SELECT from $MEASURE_VERTEX_CLASS WHERE name LUCENE ?", text).use {
                return it.elementStream()
            }

}