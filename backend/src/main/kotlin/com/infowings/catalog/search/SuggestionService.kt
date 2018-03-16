package com.infowings.catalog.search

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.Measure
import com.infowings.catalog.data.*
import com.infowings.catalog.data.aspect.AspectVertex
import com.infowings.catalog.data.aspect.toAspectVertex
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
            val traversFrom = "(SELECT FROM $MEASURE_GROUP_VERTEX WHERE name like \"$measureGroupName\")"
            "SELECT FROM $MEASURE_VERTEX WHERE " +
                    "@rid IN (SELECT @rid FROM (TRAVERSE $edgeSelector FROM $traversFrom)) " +
                    "AND SEARCH_CLASS(?) = true"
        }
        return database.query(q, "($text*)^3 (*$text*)^2 ($text~1)") {
            it.mapNotNull { it.toVertexOrNUll() }
        }
    }

    private fun findAspectInDb(
        context: SearchContext,
        commonParam: CommonSuggestionParam?,
        aspectParam: AspectSuggestionParam?
    ): Sequence<AspectVertex> {
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
                        "               FROM (SELECT FROM $MEASURE_VERTEX WHERE SEARCH_CLASS(:$lqm) = true OR name = :$unitName ))) " +
                        " AND SEARCH_CLASS(:$lq) = true"
                if (aspectId == null) { //т.к. не задан текущий аспект, то поикс идет без учета циклов
                    database.query(
                        q, mapOf(
                            lqm to luceneQuery(measureText),
                            lq to luceneQuery(aspectText),
                            unitName to measureName
                        )
                    ) { it }
                } else { //т.к. задан текущий аспект, то из результатов поиска исключаем его ветку, чтобы избежать циклических зависимостей
                    database.query(
                        "$q $noCycle",
                        mapOf(
                            lqm to luceneQuery(measureText),
                            lq to luceneQuery(aspectText),
                            unitName to measureName,
                            aspectRecord to ORecordId(aspectId)
                        )
                    ) { it }
                }
            } else {
                if (aspectId == null) {
                    val q = "SELECT FROM $ASPECT_CLASS WHERE SEARCH_CLASS(:$lq) = true"
                    database.query(q, mapOf(lq to luceneQuery(textOrAllWildcard(commonParam?.text)))) { it }
                } else {
                    findAspectVertexNoCycle(aspectId, textOrAllWildcard(commonParam?.text))
                }
            }
        return res.mapNotNull { it.toVertexOrNUll()?.toAspectVertex() }
    }

    private fun textOrAllWildcard(text: String?): String = if (text == null || text.isBlank()) "*" else text

    private fun luceneQuery(text: String) = "($text~) ($text*) (*$text*)"

    /**
     * The method search for aspects that contains "text" in its name or other fields
     * It filters out "parentAspectId" aspect and all its parents aspects to prevent cyclic dependencies on insert.
     * @return list of aspects that contains "text" in its name or other fields
     */
    fun findAspectNoCycle(aspectId: String, text: String): List<AspectData> = session(database) {
        findAspectVertexNoCycle(aspectId, text).mapNotNull { it.toVertexOrNUll()?.toAspectVertex()?.toAspectData() }.toList()
    }

    private fun findAspectVertexNoCycle(aspectId: String, text: String): Sequence<OResult> = session(database) {
        val q = "select * from $ASPECT_CLASS where SEARCH_CLASS(:$lq) = true and $noCycle"
        database.query(q, mapOf(lq to luceneQuery(text), aspectRecord to ORecordId(aspectId))) { it }
    }

    /**
     * @param aspectId aspect id to start
     * @return list of the current aspect and all its parents
     */
    fun findParentAspects(aspectId: String): List<AspectData> = session(database) {
        val q = "traverse in(\"$ASPECT_ASPECTPROPERTY_EDGE\").in() FROM :$aspectRecord"
        return@session database.query(q, mapOf(aspectRecord to ORecordId(aspectId))) {
            it.mapNotNull { it.toVertexOrNUll()?.toAspectVertex()?.toAspectData() }.toList()
        }
    }

    private val aspectRecord = "a"
    private val unitName = "un"
    private val lq = "lq"
    private val lqm = "lqm"
    private val noCycle =
        "@rid not in (select @rid from (traverse in(\"$ASPECT_ASPECTPROPERTY_EDGE\").in() FROM :$aspectRecord))"
}