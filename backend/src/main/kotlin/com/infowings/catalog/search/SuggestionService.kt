package com.infowings.catalog.search

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.BaseType
import com.infowings.catalog.common.GlobalMeasureMap
import com.infowings.catalog.common.Measure
import com.infowings.catalog.data.*
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.record.OVertex
import com.orientechnologies.orient.core.sql.executor.OResult

/**
 * Сервис поиска в OrientDB
 */
class SuggestionService(val database: OrientDatabase) {

    fun findMeasure(commonParam: CommonSuggestionParam?, measureGroupName: String?): List<Measure<*>> =
        session(database) {
            findMeasureInDb(measureGroupName, textOrAllWildcard(commonParam?.text)).mapNotNull { it.toMeasure() }
                .toList()
        }

    fun findAspect(
        context: SearchContext,
        commonParam: CommonSuggestionParam?,
        aspectParam: AspectSuggestionParam?
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
        return database.query(q, "($text*)^3 (*$text*)^2 ($text~1)") {
            it.mapNotNull { it.toVertexOrNUll() }
        }
    }

    private fun findAspectInDb(
        context: SearchContext,
        commonParam: CommonSuggestionParam?,
        aspectParam: AspectSuggestionParam?
    ): Sequence<OVertex> {
        val aspectId = context.aspectId
        val res: Sequence<OResult> =
            if (aspectParam?.measureName != null || aspectParam?.measureText != null) {
                val measureName = textOrAllWildcard(aspectParam.measureName)
                val measureText = textOrAllWildcard(aspectParam.measureText)
                val aspectText = textOrAllWildcard(commonParam?.text)
                val q = "SELECT FROM $ASPECT_CLASS " +
                        "   WHERE @rid IN " +
                        "          (SELECT @rid FROM " +
                        "              (TRAVERSE both(\"$MEASURE_BASE_EDGE\", \"$ASPECT_MEASURE_CLASS\") " +
                        "               FROM (SELECT FROM $MEASURE_VERTEX WHERE SEARCH_CLASS(?) = true OR name = ? ))) " +
                        " AND SEARCH_CLASS(?) = true"
                if (aspectId == null) {
                    database.query(q, luceneQuery(measureText), luceneQuery(aspectText), measureName) { it }
                } else {
                    database.query(
                        "$q @rid not in (select @rid from (traverse in(\"$ASPECT_ASPECTPROPERTY_EDGE\").in() FROM ?))",
                        luceneQuery(measureText),
                        luceneQuery(aspectText),
                        measureName,
                        ORecordId(aspectId)
                    ) { it }
                }
            } else {
                if (aspectId == null) {
                    val q = "SELECT FROM $ASPECT_CLASS WHERE SEARCH_CLASS(?) = true"
                    database.query(q, luceneQuery(textOrAllWildcard(commonParam?.text))) { it }
                } else {
                    findAspectVertexNoCycle(aspectId, textOrAllWildcard(commonParam?.text))
                }
            }
        return res.mapNotNull { it.toVertexOrNUll() }
    }

    private fun textOrAllWildcard(text: String?): String = if (text.isNullOrEmpty()) "*" else text!!

    private fun luceneQuery(text: String) = "($text~) ($text*) (*$text*)"

    /**
     * The method search for aspects that contains "text" in its name or other fields
     * It filters out "parentAspectId" aspect and all its parents aspects to prevent cyclic dependencies on insert.
     * @return list of aspects that contains "text" in its name or other fields
     */
    fun findAspectNoCycle(aspectId: String, text: String): List<AspectData> = session(database) {
        findAspectVertexNoCycle(aspectId, text).mapNotNull { it.toVertexOrNUll()?.toAspectData() }.toList()
    }

    private fun findAspectVertexNoCycle(aspectId: String, text: String): Sequence<OResult> = session(database) {
        val q = "select * from $ASPECT_CLASS where SEARCH_CLASS(?) = true and " +
                "@rid not in (select @rid from (traverse in(\"$ASPECT_ASPECTPROPERTY_EDGE\").in() FROM ?))"
        database.query(q, "($text~) ($text*) (*$text*)", ORecordId(aspectId)) { it }
    }

    /**
     * @param aspectId aspect id to start
     * @return list of the current aspect and all its parents
     */
    fun findParentAspects(aspectId: String): List<AspectData> = session(database) {
        val q = "traverse in(\"$ASPECT_ASPECTPROPERTY_EDGE\").in() FROM ?"
        return database.query(q, ORecordId(aspectId)) {
            it.mapNotNull { it.toVertexOrNUll()?.toAspectData() }.toList()
        }
    }

    private fun OVertex.toMeasure() = GlobalMeasureMap[this["name"]]

    private fun OVertex.toAspectData() = AspectData(
        id = identity.toString(),
        name = this["name"],
        measure = this["measure"],
        baseType = this["baseType"] ?: BaseType.Nothing.name,
        domain = BaseType.restoreBaseType(this["baseType"]).let { OpenDomain(it).toString() },
        version = this.version
    )
}


