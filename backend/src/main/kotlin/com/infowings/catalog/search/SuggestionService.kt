package com.infowings.catalog.search

import com.infowings.catalog.common.GlobalMeasureMap
import com.infowings.catalog.common.Measure
import com.infowings.catalog.data.Aspect
import com.infowings.catalog.data.MEASURE_VERTEX
import com.infowings.catalog.storage.*

import com.infowings.common.search.SearchContext
import com.orientechnologies.orient.core.record.OVertex

/**
 * Сервис поиска в OrientDB
 */
class SuggestionService(val database: OrientDatabase) {

    fun findMeasure(context: SearchContext, text: String): List<Measure<*>> = session(database) {
        findInDb(MEASURE_VERTEX, context, text)
                .mapNotNull { toMeasure(it) }
                .toList()
    }

    fun findAspect(context: SearchContext, text: String): List<Aspect> = session(database) {
        findInDb(ASPECT_CLASS, context, text)
                .mapNotNull { toAspect(it) }
                .toList()
    }


    private fun findInDb(classType: String, context: SearchContext, text: String): Sequence<OVertex> {
        val q = "SELECT FROM $classType WHERE SEARCH_CLASS(?) = true"
        return database.query(q, "($text~) ($text*) (*$text*)") {
            it.mapNotNull { it.toVertexOrNUll() }
        }
    }

    private fun toMeasure(oVertex: OVertex) : Measure<*>? {
        return GlobalMeasureMap[oVertex.getProperty("name")]
    }

    private fun toAspect(oVertex: OVertex): Aspect {
        return Aspect(id = oVertex.identity.toString(),
                name = oVertex.getProperty("name"),
                measure = GlobalMeasureMap[oVertex.getProperty("measure")],
                baseType = oVertex.getProperty("baseType"))
    }


}

