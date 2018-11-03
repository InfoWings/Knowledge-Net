package com.infowings.catalog.search

import com.infowings.catalog.common.*
import com.infowings.catalog.data.*
import com.infowings.catalog.data.aspect.*
import com.infowings.catalog.data.objekt.ObjectTruncated
import com.infowings.catalog.data.objekt.toObjectVertex
import com.infowings.catalog.data.reference.book.REFERENCE_BOOK_ITEM_VERTEX
import com.infowings.catalog.data.reference.book.ReferenceBookItemVertex
import com.infowings.catalog.data.reference.book.toReferenceBookItemVertex
import com.infowings.catalog.data.subject.toSubject
import com.infowings.catalog.data.subject.toSubjectVertex
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.record.OVertex
import com.orientechnologies.orient.core.sql.executor.OResult
import notDeletedSql

/**
 * Сервис поиска в OrientDB
 */
class SuggestionService(
    private val database: OrientDatabase,
    private val aspectDao: AspectDaoService
) {

    private val maxResultSize = 20

    fun findMeasure(
        commonParam: CommonSuggestionParam?,
        measureGroupName: String?,
        findInGroups: Boolean
    ) = SuggestedMeasureData(
        findMeasure(commonParam, measureGroupName).map { it.name },
        if (findInGroups) findMeasureGroups(commonParam?.text) else emptyList()
    )

    private fun findMeasureGroups(text: String?): List<String> {
        if (text == null) {
            return emptyList()
        }
        val q = "SELECT FROM $MEASURE_GROUP_VERTEX WHERE SEARCH_INDEX(${luceneIdx(
            MEASURE_GROUP_VERTEX,
            ATTR_NAME
        )}, ?) = true"
        return database.query(q, luceneQuery(text)) {
            it.mapNotNull { it.toVertexOrNull()?.getProperty<String>("name") }.toList()
        }
    }

    fun findMeasure(
        commonParam: CommonSuggestionParam?,
        measureGroupName: String?
    ): List<Measure<*>> {
        val text = textOrAllWildcard(commonParam?.text)
        return session(database) {
            findMeasureInDb(measureGroupName, text).mapNotNull { it.toMeasure() }
                .toMutableList<Measure<*>>()
                .addAnExactMatchToTheBeginning(commonParam)
                .addMeasureDescSuggestion(text, MEASURE_VERTEX)
                .distinct()
                .take(maxResultSize)
        }
    }

    private fun MutableList<Measure<*>>.addMeasureDescSuggestion(text: String, clazz: String): MutableList<Measure<*>> {
        if (this.size < maxResultSize) {
            this.addAll(descSuggestion(text, clazz).mapNotNull { it.toMeasure() })
        }
        return this
    }


    fun aspectHints(
        context: SearchContext,
        commonParam: CommonSuggestionParam?,
        aspectParam: AspectSuggestionParam?
    ): AspectsHints = session(database) {
        fun AspectVertex.fullName() = "$name(${subject?.name ?: "Global"})"

        val byName = findAspectInDb(context, commonParam, aspectParam)
            .filterNotNull()
            .toList().map { AspectHint(it.fullName(), it.description, null, null, null, null, null, null,
                subjectName = it.subject?.name, guid = it.guid ?: "?", source = AspectHintSource.ASPECT_NAME.toString()) }

        val byDesc = commonParam?.text?.let { text ->
            findAspectsByDesc(text)
                .filterNotNull()
                .toList().map {
                    AspectHint(it.fullName(), it.description, null, null, null, null, null, null,
                        subjectName = it.subject?.name, guid = it.guid ?: "?",
                        source = AspectHintSource.ASPECT_DESCRIPTION.toString())
                }
        } ?: emptyList()

        val byRefBookItemValue = commonParam?.text?.let { text ->
            findRefBookItemsByValue(text)
                .mapNotNull { vertex ->
                    val root = vertex.root ?: vertex
                    root.aspect?.let { aspect ->
                        AspectHint(aspect.fullName(), aspect.description, vertex.value, vertex.description, null, null, null, null,
                            subjectName = aspect.subject?.name, guid = aspect.guid ?: "?",
                            source = AspectHintSource.REFBOOK_NAME.toString())
                    }
                }.toList()
        } ?: emptyList()

        val byRefBookItemDesc = commonParam?.text?.let { text ->
            findRefBookItemsByDesc(text)
                .mapNotNull { vertex ->
                    val root = vertex.root ?: vertex
                    root.aspect?.let { aspect ->
                        AspectHint(aspect.fullName(), aspect.description, vertex.value, vertex.description, null, null, null, null,
                            subjectName = aspect.subject?.name, guid = aspect.guid ?: "?",
                            source = AspectHintSource.REFBOOK_DESCRIPTION.toString())
                    }
                }.toList()
        } ?: emptyList()

        val byProperty = commonParam?.text?.let { text ->
            findAspectPropertyByNameWithAspect(text)
                .mapNotNull { vertex ->
                    aspectDao.find(vertex.aspect)?.let { aspect ->
                        AspectHint(
                            vertex.parentAspect.fullName(),
                            vertex.parentAspect.description,
                            null,
                            null,
                            vertex.name,
                            vertex.description,
                            aspect.fullName(),
                            aspect.description,
                            subjectName = vertex.parentAspect.subject?.name, guid = vertex.parentAspect.guid ?: "?",
                            source = AspectHintSource.ASPECT_PROPERTY_WITH_ASPECT.toString()
                        )
                    }
                }.toList()
        } ?: emptyList()

        val keyGroups = listOf(byName, byProperty, byRefBookItemValue, byDesc).map { it.map { it.name }.toSet() }

        val keyGroupsAcc = keyGroups.fold(listOf(emptySet())) { acc: List<Set<String?>>, second ->
            val newSet = acc.last() + second
            acc + listOf(newSet)
        }


        return@session AspectsHints.empty()
            .copy(byAspectName = byName)
            .copy(byProperty = byProperty.filterNot { keyGroupsAcc[1].contains(it.name) })
            .copy(byRefBookValue = byRefBookItemValue.filterNot { keyGroupsAcc[2].contains(it.name) })
            .copy(byAspectDesc = byDesc.filterNot { keyGroupsAcc[3].contains(it.name) })
            .copy(byRefBookDesc = byRefBookItemDesc.filterNot { keyGroupsAcc[4].contains(it.name) })
    }

    fun findAspect(
        context: SearchContext,
        commonParam: CommonSuggestionParam?,
        aspectParam: AspectSuggestionParam?
    ): List<AspectData> = session(database) {
        findAspectInDb(context, commonParam, aspectParam)
            .mapNotNull { it.toAspectData() }
            .toMutableList()
            .addAspectDescSuggestion(commonParam)
            .take(maxResultSize)
    }

    private fun MutableList<AspectData>.addAspectDescSuggestion(commonParam: CommonSuggestionParam?): MutableList<AspectData> {
        if (this.size < maxResultSize) {
            this.addAll(
                descSuggestion(textOrAllWildcard(commonParam?.text), ASPECT_CLASS)
                    .mapNotNull { it.toAspectVertex().toAspectData() })
        }
        return this
    }

    fun findSubject(
        commonParam: CommonSuggestionParam?,
        subjectParam: SubjectSuggestionParam
    ): List<SubjectData> = session(database) {
        findSubjectInDb(commonParam, subjectParam)
            .mapNotNull { it.toSubjectVertex().toSubject().toSubjectData() }
            .toMutableList()
            .addSubjectDescSuggestion(commonParam)
    }

    private fun MutableList<SubjectData>.addSubjectDescSuggestion(commonParam: CommonSuggestionParam?): MutableList<SubjectData> {
        if (this.size < maxResultSize) {
            this.addAll(
                descSuggestion(textOrAllWildcard(commonParam?.text), SUBJECT_CLASS)
                    .mapNotNull { it.toSubjectVertex().toSubject().toSubjectData() })
        }
        return this
    }

    private fun findSubjectInDb(
        commonParam: CommonSuggestionParam?,
        subjectParam: SubjectSuggestionParam?
    ): Sequence<OVertex> {
        val q = "SELECT FROM $SUBJECT_CLASS WHERE SEARCH_INDEX(${luceneIdx(
            SUBJECT_CLASS,
            ATTR_NAME
        )}, :$lq) = true AND $notDeletedSql"
        val aspectFilter = if (subjectParam?.aspectText.isNullOrBlank()) {
            ""
        } else {
            " AND @rid IN (SELECT expand(out($ASPECT_SUBJECT_EDGE)).@rid FROM Aspect WHERE SEARCH_INDEX(${luceneIdx(
                ASPECT_CLASS,
                ATTR_NAME
            )}, :$lqa) = true)"
        }
        return database.query(
            q + aspectFilter,
            mapOf(
                lq to luceneQuery(textOrAllWildcard(commonParam?.text)),
                lqa to luceneQuery(textOrAllWildcard(subjectParam?.aspectText))
            )
        ) {
            it.mapNotNull { it.toVertexOrNull() }
        }
    }

    private fun findMeasureInDb(measureGroupName: String?, text: String): Sequence<OVertex> {
        val q = if (measureGroupName == null) {
            "SELECT FROM $MEASURE_VERTEX WHERE SEARCH_INDEX(${luceneIdx(MEASURE_VERTEX, ATTR_NAME)}, ?) = true"
        } else {
            val edgeSelector = "both(\"$MEASURE_BASE_EDGE\", \"$MEASURE_BASE_AND_GROUP_EDGE\")"
            val traversFrom = "(SELECT FROM $MEASURE_GROUP_VERTEX WHERE name like \"$measureGroupName\")"
            "SELECT FROM $MEASURE_VERTEX WHERE " +
                    "@rid IN (SELECT @rid FROM (TRAVERSE $edgeSelector FROM $traversFrom)) " +
                    "AND SEARCH_INDEX(${luceneIdx(MEASURE_VERTEX, ATTR_NAME)}, ?) = true"
        }
        return database.query(q, "($text*)^3 (*$text*)^2 ($text~1)") {
            it.mapNotNull { it.toVertexOrNull() }
        }
    }

    private fun descSuggestion(text: String, vertexClass: String): Sequence<OVertex> {
        val q = "SELECT FROM $vertexClass WHERE SEARCH_INDEX(${luceneIdx(vertexClass, ATTR_DESC)}, ?) = true"
        return database.query(q, "($text~1)") {
            it.mapNotNull { it.toVertexOrNull() }
        }
    }

    private fun MutableList<Measure<*>>.addAnExactMatchToTheBeginning(
        commonParam: CommonSuggestionParam?
    ): MutableList<Measure<*>> {
        val measure = commonParam?.text?.let { GlobalMeasureMap.values.find { m -> m.symbol == it } }
        measure?.let {
            this.remove(it)
            this.add(0, it)
        }
        return this
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
                val q = selectFromAspectWithoutDeleted +
                        "    AND @rid IN " +
                        "          (SELECT @rid FROM " +
                        "              (TRAVERSE both(\"$MEASURE_BASE_EDGE\", \"$ASPECT_MEASURE_CLASS\") " +
                        "               FROM (SELECT FROM $MEASURE_VERTEX WHERE SEARCH_INDEX(${luceneIdx(
                            MEASURE_VERTEX, ATTR_NAME
                        )}, :$lqm) = true OR name = :$unitName ))) " +
                        " AND SEARCH_INDEX(${luceneIdx(ASPECT_CLASS, ATTR_NAME)}, :$lq) = true"
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
                    val q = "$selectFromAspectWithoutDeleted AND SEARCH_INDEX(${luceneIdx(ASPECT_CLASS, ATTR_NAME)}, :$lq) = true"
                    database.query(q, mapOf(lq to luceneQuery(textOrAllWildcard(commonParam?.text)))) { it }
                } else {
                    findAspectVertexNoCycle(aspectId, textOrAllWildcard(commonParam?.text))
                }
            }
        return res.mapNotNull { it.toVertexOrNull()?.toAspectVertex() }
    }

    private fun findAspectsByDesc(text: String): Sequence<AspectVertex> {
        val q = "$selectFromAspectWithoutDeleted AND SEARCH_INDEX(${luceneIdx(ASPECT_CLASS, ATTR_DESC)}, :$lq) = true"
        val res = database.query(q, mapOf(lq to luceneQuery(textOrAllWildcard(text)))) { it }
        return res.mapNotNull { it.toVertexOrNull()?.toAspectVertex() }
    }

    private fun findRefBookItemsByValue(text: String): Sequence<ReferenceBookItemVertex> {
        val q = "SELECT FROM $REFERENCE_BOOK_ITEM_VERTEX WHERE (deleted is null or deleted = false)" +
                " AND SEARCH_INDEX(${luceneIdx(OrientClass.REFBOOK_ITEM.extName, ATTR_VALUE)}, :$lq) = true"
        val res = database.query(q, mapOf(lq to luceneQuery(textOrAllWildcard(text)))) { it }
        return res.mapNotNull { it.toVertexOrNull()?.toReferenceBookItemVertex() }
    }

    private fun findRefBookItemsByDesc(text: String): Sequence<ReferenceBookItemVertex> {
        val q = "SELECT FROM $REFERENCE_BOOK_ITEM_VERTEX WHERE (deleted is null or deleted = false)" +
                " AND SEARCH_INDEX(${luceneIdx(OrientClass.REFBOOK_ITEM.extName, ATTR_DESC)}, :$lq) = true"
        val res = database.query(q, mapOf(lq to luceneQuery(textOrAllWildcard(text)))) { it }
        return res.mapNotNull { it.toVertexOrNull()?.toReferenceBookItemVertex() }
    }

    private fun findAspectPropertyByNameWithAspect(text: String): Sequence<AspectPropertyVertex> {
        val q = "SELECT FROM ${OrientClass.ASPECT_PROPERTY.extName} WHERE (deleted is null or deleted = false)" +
                " AND SEARCH_INDEX(${luceneIdx(OrientClass.ASPECT_PROPERTY.extName, "name_with_aspect")}, :$lq) = true"
        val res = database.query(q, mapOf(lq to luceneQuery(textOrAllWildcard(text)))) { it }
        return res.mapNotNull {
            it.toVertexOrNull()?.toAspectPropertyVertex()
        }
    }

    private fun textOrAllWildcard(text: String?): String = if (text == null || text.isBlank()) "*" else text

    private fun luceneQuery(text: String) = "($text~) ($text*) (*$text*)"

    /**
     * The method search for aspects that contains "text" in its name or other fields
     * It filters out "parentAspectId" aspect and all its parents aspects to prevent cyclic dependencies on insert.
     * @return list of aspects that contains "text" in its name or other fields
     */
    fun findAspectNoCycle(aspectId: String, text: String): List<AspectData> = session(database) {
        findAspectVertexNoCycle(aspectId, text).mapNotNull { it.toVertexOrNull()?.toAspectVertex()?.toAspectData() }
            .toList()
    }

    private fun findAspectVertexNoCycle(aspectId: String, text: String): Sequence<OResult> = session(database) {
        val q =
            "$selectFromAspectWithoutDeleted AND SEARCH_INDEX(${luceneIdx(
                ASPECT_CLASS,
                ATTR_NAME
            )}, :$lq) = true AND $noCycle"
        database.query(q, mapOf(lq to luceneQuery(text), aspectRecord to ORecordId(aspectId))) { it }
    }

    private fun luceneIdx(classType: String, attr: String) = "\"$classType.lucene.$attr\""

    private val aspectRecord = "a"
    private val unitName = "un"
    private val lq = "lq"
    private val lqm = "lqm"
    private val lqa = "lqa"
    private val noCycle =
        "@rid not in (select @rid from (traverse in(\"$ASPECT_ASPECT_PROPERTY_EDGE\").in() FROM :$aspectRecord))"


    fun findObject(
        commonParam: CommonSuggestionParam?,
        objectParam: ObjectSuggestionParam
    ): List<ObjectGetResponse> = session(database) {
        val found = findObjectInDb(commonParam, objectParam)
        val result = found.mapNotNull {
            val objVertex = it.toObjectVertex()
            ObjectTruncated(
                id = objVertex.identity, name = objVertex.name, description = objVertex.description, guid = objVertex.guid,
                subjectName = objVertex.subject?.name ?: "", objectPropertiesCount = 0, lastUpdated = null
            ).toResponse()
        }.toMutableList()
        //.addSubjectDescSuggestion(commonParam)

        result
    }

    /*
    private fun MutableList<SubjectData>.addSubjectDescSuggestion(commonParam: CommonSuggestionParam?): MutableList<SubjectData> {
        if (this.size < maxResultSize) {
            this.addAll(
                descSuggestion(textOrAllWildcard(commonParam?.text), SUBJECT_CLASS)
                    .mapNotNull { it.toSubjectVertex().toSubject().toSubjectData() })
        }
        return this
    }
    */

    private fun findObjectInDb(
        commonParam: CommonSuggestionParam?,
        objectParam: ObjectSuggestionParam?
    ): Sequence<OVertex> {
        val q = "SELECT FROM $OBJECT_CLASS WHERE SEARCH_INDEX(${luceneIdx(
            OBJECT_CLASS,
            ATTR_NAME
        )}, :$lq) = true AND $notDeletedSql"

        /*
        val aspectFilter = if (objectParam?.aspectText.isNullOrBlank()) {
            ""
        } else {
            " AND @rid IN (SELECT expand(out($ASPECT_SUBJECT_EDGE)).@rid FROM Aspect WHERE SEARCH_INDEX(${luceneIdx(
                ASPECT_CLASS,
                ATTR_NAME
            )}, :$lqa) = true)"
        }
        */

        return database.query(
            q /*+ aspectFilter */,
            mapOf(
                lq to luceneQuery(textOrAllWildcard(commonParam?.text)),
                lqa to luceneQuery(textOrAllWildcard(objectParam?.aspectText))
            )
        ) {
            it.mapNotNull { it.toVertexOrNull() }
        }
    }
}