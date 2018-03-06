package com.infowings.catalog.search

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.Measure
import com.infowings.catalog.data.MEASURE_VERTEX
import com.infowings.catalog.data.toAspectData
import com.infowings.catalog.data.toMeasure
import com.infowings.catalog.storage.ASPECT_CLASS
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.session
import com.infowings.catalog.storage.toVertexOrNUll
import com.orientechnologies.orient.core.record.OVertex

/**
 * Сервис поиска в OrientDB
 */
class SuggestionService(val database: OrientDatabase) {

    fun findMeasure(context: SearchContext?, text: String): List<Measure<*>> = session(database) {
        findInDb(MEASURE_VERTEX, context, text)
                .mapNotNull { it.toMeasure() }
                .toList()
    }

    fun findAspect(context: SearchContext, text: String): List<AspectData> = session(database) {
        findInDb(ASPECT_CLASS, context, text)
                .mapNotNull { it.toAspectData() }
                .toList()
    }

    private fun findInDb(classType: String, context: SearchContext?, text: String): Sequence<OVertex> {
        val q = "SELECT FROM $classType WHERE SEARCH_CLASS(?) = true"
        return database.query(q, "($text~) ($text*) (*$text*)") {
            it.mapNotNull { it.toVertexOrNUll() }
        }
    }
}

