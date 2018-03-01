package com.infowings.catalog.search

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.BaseType
import com.infowings.catalog.common.GlobalMeasureMap
import com.infowings.catalog.common.Measure
import com.infowings.catalog.data.*
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.OVertex

/**
 * Сервис поиска в OrientDB
 */
class SuggestionService(val database: OrientDatabase) {

    fun findMeasure(commonParam: CommonSuggestionParam?, measureGroupName: String?): List<Measure<*>> =
        session(database) {
            findMeasureInDb(
                measureGroupName, textOrAllWildcard(commonParam?.text)
            ).mapNotNull { it.toMeasure() }
                .toList()
        }

    fun findAspect(
        context: SearchContext, commonParam: CommonSuggestionParam?, aspectParam: AspectSuggestionParam?
    ): List<AspectData> = session(database) {
        findAspectInDb(context, commonParam, aspectParam)
            .mapNotNull { it.toAspectData() }
            .toList()
    }

    private fun findMeasureInDb(measureGroupName: String?, text: String): Sequence<OVertex> {
        val q = if (measureGroupName == null) {
            "SELECT FROM $MEASURE_VERTEX WHERE SEARCH_CLASS(?) = true"
        } else {
            val edgeSelector = "both(\"$MEASURE_BASE_EDGE\", \"$MEASURE_BASE_AND_GROUP_EDGE\")"
            val traversFrom = "(SELECT FROM $MEASURE_GROUP_VERTEX WHERE name = \"$measureGroupName\"))"
            "SELECT FROM (TRAVERS $edgeSelector FROM $traversFrom) WHERE SEARCH_CLASS(?) = true"
        }
        return luceneSearchInDb(q, text)
    }

    private fun findAspectInDb(
        context: SearchContext, commonParam: CommonSuggestionParam?, aspectParam: AspectSuggestionParam?
    ): Sequence<OVertex> {
        if (aspectParam?.measureName != null || aspectParam?.measureText != null) {
            val measureName = textOrAllWildcard(aspectParam?.measureName)
            val measureText = textOrAllWildcard(aspectParam?.measureText)
            val aspectText = textOrAllWildcard(commonParam?.text)
            val q = "SELECT FROM $ASPECT_CLASS " +
                    "   WHERE @rid IN " +
                    "          (SELECT @rid FROM " +
                    "              (TRAVERSE both(\"$MEASURE_BASE_EDGE\", \"$ASPECT_MEASURE_CLASS\") " +
                    "               FROM (SELECT FROM $MEASURE_VERTEX WHERE SEARCH_CLASS(?) = true OR name = ? ))) " +
                    " AND SEARCH_CLASS(?) = true"
            return database.query(q, luceneQuery(measureText), luceneQuery(aspectText), measureName) {
                it.mapNotNull { it.toVertexOrNUll() }
            }
        } else {
            val q = "SELECT FROM $ASPECT_CLASS WHERE SEARCH_CLASS(?) = true"
            return luceneSearchInDb(q, textOrAllWildcard(commonParam?.text))
        }
    }

    private fun textOrAllWildcard(text: String?): String =
        if (text.isNullOrEmpty()) "*" else text!!

    private fun luceneSearchInDb(q: String, text: String): Sequence<OVertex> {
        return database.query(q, luceneQuery(text)) {
            it.mapNotNull { it.toVertexOrNUll() }
        }
    }

    private fun luceneQuery(text: String) = "($text~) ($text*) (*$text*)"

    private fun OVertex.toMeasure() = GlobalMeasureMap[this["name"]]

    private fun OVertex.toAspectData() = AspectData(
        id = identity.toString(),
        name = this["name"],
        measure = this["measure"],
        baseType = this["baseType"],
        domain = BaseType.restoreBaseType(this["baseType"])?.let { OpenDomain(it).toString() }
    )
}


