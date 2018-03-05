package com.infowings.catalog.search

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.BaseType
import com.infowings.catalog.common.GlobalMeasureMap
import com.infowings.catalog.common.Measure
import com.infowings.catalog.data.MEASURE_VERTEX
import com.infowings.catalog.data.OpenDomain
import com.infowings.catalog.storage.*
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
    fun findAspectNoCycle(parentAspectId: String, text: String): List<AspectData> = session(database) {
        val q = "select * from $ASPECT_CLASS where SEARCH_CLASS(?) = true and " +
                "@rid not in (select @rid from (traverse in(\"$ASPECT_ASPECTPROPERTY_EDGE\").in() FROM ?))"

        return database.query(q, "($text~) ($text*) (*$text*)", ORecordId(parentAspectId)) {
            it.mapNotNull { it.toVertexOrNUll()?.toAspectData() }.toList()
        }
    }

    private fun OVertex.toMeasure() = GlobalMeasureMap[this["name"]]

    private fun OVertex.toAspectData() = AspectData(
            id = identity.toString(),
            name = this["name"],
            measure = this["measure"],
            baseType = this["baseType"],
            domain = BaseType.restoreBaseType(this["baseType"])?.let { OpenDomain(it).toString() }
    )
}

