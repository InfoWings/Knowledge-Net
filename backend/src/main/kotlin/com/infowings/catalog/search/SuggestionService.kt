package com.infowings.catalog.search

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.Measure
import com.infowings.catalog.data.MEASURE_VERTEX
import com.infowings.catalog.data.toAspectData
import com.infowings.catalog.data.toMeasure
import com.infowings.catalog.storage.ASPECT_ASPECTPROPERTY_EDGE
import com.infowings.catalog.storage.ASPECT_CLASS
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.session
import com.infowings.catalog.storage.toVertexOrNUll
import com.orientechnologies.orient.core.id.ORecordId
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

    /**
     * The method search for aspects that contains "text" in its name or other fields
     * It filters out "parentAspectId" aspect and all its parents aspects to prevent cyclic dependencies on insert.
     * @return list of aspects that contains "text" in its name or other fields
     */
    fun findAspectNoCycle(aspectId: String, text: String): List<AspectData> = session(database) {
        val q = "select * from $ASPECT_CLASS where SEARCH_CLASS(?) = true and " +
                "@rid not in (select @rid from (traverse in(\"$ASPECT_ASPECTPROPERTY_EDGE\").in() FROM ?))"

        return@session database.query(q, "($text~) ($text*) (*$text*)", ORecordId(aspectId)) {
            it.mapNotNull { it.toVertexOrNUll()?.toAspectData() }.toList()
        }
    }

    /**
     * @param aspectId aspect id to start
     * @return list of the current aspect and all its parents
     */
    fun findParentAspects(aspectId: String): List<AspectData> = session(database) {
        val q = "traverse in(\"$ASPECT_ASPECTPROPERTY_EDGE\").in() FROM ?"
        return@session database.query(q, ORecordId(aspectId)) {
            it.mapNotNull { it.toVertexOrNUll()?.toAspectData() }.toList()
        }
    }
}

