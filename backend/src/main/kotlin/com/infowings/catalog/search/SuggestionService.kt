package com.infowings.catalog.search

import com.infowings.catalog.common.GlobalMeasureMap
import com.infowings.catalog.common.Measure
import com.infowings.catalog.data.MEASURE_VERTEX
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.session
import com.infowings.catalog.storage.toVertexOrNUll

import com.infowings.common.search.SearchContext
import com.orientechnologies.orient.core.record.OVertex

/**
 * Сервис поиска в OrientDB
 */
class SuggestionService(val database: OrientDatabase) {

    private val q = "SELECT FROM $MEASURE_VERTEX WHERE SEARCH_CLASS(?) = true"

    fun find(context: SearchContext, text: String): List<Measure<*>> = session(database) {
        findInDb(context, text)
                .mapNotNull { GlobalMeasureMap[it.getProperty("name")] }
                .toList()
    }

    private fun findInDb(context: SearchContext, text: String): Sequence<OVertex> =
            database.query(q, "($text~) ($text*) (*$text*)") {
                it.mapNotNull { it.toVertexOrNUll() }
            }
}

