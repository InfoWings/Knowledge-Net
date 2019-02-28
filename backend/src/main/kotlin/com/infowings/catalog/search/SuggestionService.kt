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
import com.infowings.catalog.external.logTime
import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.id.ORID
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

    fun findMeasure(commonParam: CommonSuggestionParam?, measureGroupName: String?, findInGroups: Boolean) =
        SuggestedMeasureData(
            measureNames = findMeasure(commonParam, measureGroupName).map { it.name },
            measureGroupNames = if (findInGroups) findMeasureGroups(commonParam?.text) else emptyList()
        )

    private fun findMeasureGroups(text: String?): List<String> {
        if (text == null) return emptyList()

        val q = "SELECT FROM $MEASURE_GROUP_VERTEX WHERE SEARCH_INDEX(${luceneIdx(MEASURE_GROUP_VERTEX, ATTR_NAME)}, ?) = true"

        return database.query(q, luceneQuery(text)) {
            it.mapNotNull { it.toVertexOrNull()?.getProperty<String>("name") }.toList()
        }
    }

    fun findMeasure(commonParam: CommonSuggestionParam?, measureGroupName: String?): List<Measure<*>> {
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


    private fun noCycle(aspectId: ORID, candidates: List<ORID>): List<ORID> {
        val params = mapOf(
            aspectRecord to aspectId,
            "ids" to candidates
        )
        return database.query("SELECT FROM :ids WHERE ($noCycle)", params) { it.toList().map { it.getProperty<ORID>("@rid") } }
    }


    fun aspectHints(context: SearchContext, commonParam: CommonSuggestionParam?, aspectParam: AspectSuggestionParam?): AspectsHints =
        logTime(logger, "Generate suggestions ") {
            session(database) {

                val byName = findAspectInDb(context, commonParam, aspectParam)
                    .map { AspectHint.byAspect(it.toAspectData(), AspectHintSource.ASPECT_NAME) }

                val byDesc = commonParam?.text?.let { text ->
                    findAspectsByDesc(text, context).map { AspectHint.byAspect(it.toAspectData(), AspectHintSource.ASPECT_DESCRIPTION) }
                } ?: emptyList()

                val byRefBookItemValue = commonParam?.text?.let { text ->
                    findRefBookItemsByValue(text, context).mapNotNull { it.toAspectHint() }
                } ?: emptyList()

                val byRefBookItemDesc = commonParam?.text?.let { text ->
                    findRefBookItemsByDesc(text, context).mapNotNull { it.toAspectHint() }.toList()
                } ?: emptyList()

                val byProperty = commonParam?.text?.let { text ->
                    findAspectPropertyByNameWithAspect(text, context).mapNotNull {
                        val hint = it.toAspectHint()
                        if (hint == null) {
                            logger.warn("aspect property without parent aspect: ${it.aspect} - ${it.name}")
                        }
                        return@mapNotNull hint
                    }
                } ?: emptyList()


                val keyGroups = listOf(byName, byProperty, byRefBookItemValue, byDesc).map { it.map { it.name }.toSet() }

                val keyGroupsAcc = keyGroups.fold(listOf(emptySet())) { acc: List<Set<String?>>, second ->
                    val newSet = acc.last() + second
                    acc + listOf(newSet)
                }


                return@session AspectsHints(
                    byAspectName = byName,
                    byProperty = byProperty.filterNot { keyGroupsAcc[1].contains(it.name) },
                    byRefBookValue = byRefBookItemValue.filterNot { keyGroupsAcc[2].contains(it.name) },
                    byAspectDesc = byDesc.filterNot { keyGroupsAcc[3].contains(it.name) },
                    byRefBookDesc = byRefBookItemDesc.filterNot { keyGroupsAcc[4].contains(it.name) }
                )
            }
        }

    fun findAspect(context: SearchContext, commonParam: CommonSuggestionParam?, aspectParam: AspectSuggestionParam?): List<AspectData> =
        session(database) {
            findAspectInDb(context, commonParam, aspectParam)
                .map { it.toAspectData() }
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

    fun findSubject(commonParam: CommonSuggestionParam?, subjectParam: SubjectSuggestionParam): List<SubjectData> =
        session(database) {
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

    private fun findSubjectInDb(commonParam: CommonSuggestionParam?, subjectParam: SubjectSuggestionParam?): List<OVertex> {
        val q = "SELECT FROM $SUBJECT_CLASS WHERE SEARCH_INDEX(${luceneIdx(SUBJECT_CLASS, ATTR_NAME)}, :$lq) = true AND $notDeletedSql"

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
            it.mapNotNull { it.toVertexOrNull() }.toList()
        }
    }

    private fun findMeasureInDb(measureGroupName: String?, text: String): List<OVertex> {
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
            it.mapNotNull { it.toVertexOrNull() }.toList()
        }
    }

    private fun descSuggestion(text: String, vertexClass: String): List<OVertex> {
        val q = "SELECT FROM $vertexClass WHERE SEARCH_INDEX(${luceneIdx(vertexClass, ATTR_DESC)}, ?) = true"
        return database.query(q, "($text~1)") {
            it.mapNotNull { it.toVertexOrNull() }.toList()
        }
    }

    private fun MutableList<Measure<*>>.addAnExactMatchToTheBeginning(commonParam: CommonSuggestionParam?): MutableList<Measure<*>> {
        val measure = commonParam?.text?.let { GlobalMeasureMap.values.find { m -> m.symbol == it } }
        measure?.let {
            this.remove(it)
            this.add(0, it)
        }
        return this
    }

    private fun findAspectInDb(context: SearchContext, commonParam: CommonSuggestionParam?, aspectParam: AspectSuggestionParam?): List<AspectVertex> {
        val aspectId = context.aspectId
        val res: List<OResult> = if (aspectParam?.measureName != null || aspectParam?.measureText != null) {
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
                )
            } else { //т.к. задан текущий аспект, то из результатов поиска исключаем его ветку, чтобы избежать циклических зависимостей
                database.query(
                    "$q $noCycle",
                    mapOf(
                        lqm to luceneQuery(measureText),
                        lq to luceneQuery(aspectText),
                        unitName to measureName,
                        aspectRecord to ORecordId(aspectId)
                    )
                )
            }
        } else {
            if (aspectId == null) {
                val q = "$selectFromAspectWithoutDeleted AND SEARCH_INDEX(${luceneIdx(ASPECT_CLASS, ATTR_NAME)}, :$lq) = true"
                database.query(q, mapOf(lq to luceneQuery(textOrAllWildcardNoSpaces(commonParam?.text))))
            } else {
                findAspectVertexNoCycle(aspectId, textOrAllWildcardNoSpaces(commonParam?.text))
            }
        }
        return res.mapNotNull { it.toVertexOrNull()?.toAspectVertex() }
    }

    private fun findAspectsByDesc(text: String, ctx: SearchContext): List<AspectVertex> = try {
        val res = if (ctx.aspectId.isNullOrEmpty()) {
            val q = "$selectFromAspectWithoutDeleted AND SEARCH_INDEX(${luceneIdx(ASPECT_CLASS, ATTR_DESC)}, :$lq) = true"
            val params = mapOf(lq to luceneQuery(textOrAllWildcardNoSpaces(text)))
            database.query(q, params)
        } else {
            val q = "$selectFromAspectWithoutDeleted AND SEARCH_INDEX(${luceneIdx(ASPECT_CLASS, ATTR_DESC)}, :$lq) = true AND ($noCycle)"
            val params = mapOf(
                lq to luceneQuery(textOrAllWildcard(text)),
                aspectRecord to ORecordId(ctx.aspectId)
            )
            database.query(q, params)
        }
        res.mapNotNull { it.toVertexOrNull()?.toAspectVertex() }
    } catch (e: Exception) {
        logger.warn("Unable to obtain aspects by aspect description: $e")
        emptyList()
    }

    private fun findRefBookItemsByValue(text: String, ctx: SearchContext): List<ReferenceBookItemVertex> = try {
        val q = "SELECT FROM $REFERENCE_BOOK_ITEM_VERTEX WHERE $notDeletedSql" +
                " AND SEARCH_INDEX(${luceneIdx(OrientClass.REFBOOK_ITEM.extName, ATTR_VALUE)}, :$lq) = true"
        val res = database.query(q, mapOf(lq to luceneQuery(textOrAllWildcardNoSpaces(text)))) {
            it.mapNotNull { it.toVertexOrNull()?.toReferenceBookItemVertex() }.toList()
        }

        if (ctx.aspectId != null && ctx.aspectId != "") {
            val aspectIds = res.mapNotNull { it.rootOrThis.aspect?.identity }

            logger.info("aspectIds: $aspectIds")

            val toRemove = noCycle(ORecordId(ctx.aspectId), aspectIds.toList()).toSet()

            logger.info("toKeep: $toRemove")

            res.filter { toRemove.contains(it.rootOrThis.aspect?.identity) }
        } else {
            res
        }
    } catch (e: Exception) {
        logger.warn("Unable to obtain aspects by ref book value: $e")
        emptyList()
    }

    private fun findRefBookItemsByDesc(text: String, ctx: SearchContext): List<ReferenceBookItemVertex> = try {
        val q = "SELECT FROM $REFERENCE_BOOK_ITEM_VERTEX WHERE $notDeletedSql" +
                " AND SEARCH_INDEX(${luceneIdx(OrientClass.REFBOOK_ITEM.extName, ATTR_DESC)}, :$lq) = true"
        val res = database.query(q, mapOf(lq to luceneQuery(textOrAllWildcardNoSpaces(text)))) {
            it.toList().mapNotNull { it.toVertexOrNull()?.toReferenceBookItemVertex() }
        }

        if (ctx.aspectId != null && ctx.aspectId != "") {
            val aspectIds = res.mapNotNull { it.rootOrThis.aspect?.identity }

            logger.info("aspectIds: $aspectIds")

            val toRemove = noCycle(ORecordId(ctx.aspectId), aspectIds.toList()).toSet()

            logger.info("toKeep: $toRemove")

            res.filter { toRemove.contains(it.rootOrThis.aspect?.identity) }
        } else {
            res
        }
    } catch (e: Exception) {
        logger.warn("Unable to obtain aspects by ref book value description: $e")
        emptyList()
    }

    private fun findAspectPropertyByNameWithAspect(text: String, ctx: SearchContext): List<AspectPropertyVertex> = try {
        val q = "SELECT FROM ${OrientClass.ASPECT_PROPERTY.extName} WHERE $notDeletedSql" +
                " AND SEARCH_INDEX(${luceneIdx(OrientClass.ASPECT_PROPERTY.extName, "name_with_aspect")}, :$lq) = true"
        val res = database.query(q, mapOf(lq to luceneQuery(textOrAllWildcardNoSpaces(text)))) {
            it.toList().mapNotNull {
                it.toVertexOrNull()?.toAspectPropertyVertex()
            }
        }

        if (ctx.aspectId != null && ctx.aspectId != "") {
            val aspectIds = res.map { it.aspect }

            logger.info("aspectIds: $aspectIds")

            val toRemove = noCycle(ORecordId(ctx.aspectId), aspectIds.map { ORecordId(it) }).toSet()

            logger.info("toKeep: $toRemove")

            res.filter { toRemove.contains(ORecordId(it.aspect)) }
        } else {
            res
        }
    } catch (e: Exception) {
        logger.warn("Unable to obtain aspects by property+aspect: $e")
        emptyList()
    }

    private fun textOrAllWildcard(text: String?): String = if (text == null || text.isBlank()) "*" else text

    private fun textOrAllWildcardNoSpaces(text: String?): String = if (text == null || text.isBlank()) "*" else {
        // remove spaces because they have special meaning for Lucene
        // proper escaping seems non-trivial and is not strictly necessary
        text.filterNot { it.toString().isBlank() }
    }

    private fun luceneQuery(text: String) = "($text~) ($text*) (*$text*)"

    /**
     * The method search for aspects that contains "text" in its name or other fields
     * It filters out "parentAspectId" aspect and all its parents aspects to prevent cyclic dependencies on insert.
     * @return list of aspects that contains "text" in its name or other fields
     */
    fun findAspectNoCycle(aspectId: String, text: String): List<AspectData> = session(database) {
        findAspectVertexNoCycle(aspectId, text).mapNotNull { it.toVertexOrNull()?.toAspectVertex()?.toAspectData() }.toList()
    }

    private fun findAspectVertexNoCycle(aspectId: String, text: String): List<OResult> = session(database) {
        val q =
            "$selectFromAspectWithoutDeleted AND SEARCH_INDEX(${luceneIdx(
                ASPECT_CLASS,
                ATTR_NAME
            )}, :$lq) = true AND $noCycle"
        database.query(q, mapOf(lq to luceneQuery(text), aspectRecord to ORecordId(aspectId)))
    }

    private fun luceneIdx(classType: String, attr: String) = "\"$classType.lucene.$attr\""

    private val aspectRecord = "a"
    private val unitName = "un"
    private val lq = "lq"
    private val lqm = "lqm"
    private val lqa = "lqa"
    private val noCycle = "@rid not in (select @rid from (traverse in(\"$ASPECT_ASPECT_PROPERTY_EDGE\").in() FROM :$aspectRecord))"


    fun findObject(commonParam: CommonSuggestionParam?, objectParam: ObjectSuggestionParam): List<ObjectGetResponse> = session(database) {
        val found = findObjectInDb(commonParam, objectParam)
        val result = found.map {
            val objVertex = it.toObjectVertex()
            ObjectTruncated(
                id = objVertex.identity, name = objVertex.name, description = objVertex.description, guid = objVertex.guid,
                subjectName = objVertex.subject?.name ?: "", objectPropertiesCount = 0, lastUpdated = null
            ).toResponse()
        }.toMutableList()
        //.addSubjectDescSuggestion(commonParam)

        result
    }

    private fun findObjectInDb(commonParam: CommonSuggestionParam?, objectParam: ObjectSuggestionParam?): List<OVertex> {
        val q = "SELECT FROM $OBJECT_CLASS WHERE SEARCH_INDEX(${luceneIdx(
            OBJECT_CLASS,
            ATTR_NAME
        )}, :$lq) = true AND $notDeletedSql"

        return database.query(
            q /*+ aspectFilter */,
            mapOf(
                lq to luceneQuery(textOrAllWildcard(commonParam?.text)),
                lqa to luceneQuery(textOrAllWildcard(objectParam?.aspectText))
            )
        ) {
            it.mapNotNull { it.toVertexOrNull() }.toList()
        }
    }

    private fun AspectVertex.fullName() = "$name(${subject?.name ?: "Global"})"

    private fun ReferenceBookItemVertex.toAspectHint(): AspectHint? {
        val root = root ?: this
        return root.aspect?.let { aspect ->
            AspectHint(
                name = aspect.fullName(),
                description = aspect.description,
                refBookItem = value,
                refBookItemDesc = description,
                subAspectName = null,
                aspectName = null,
                property = null,
                subjectName = aspect.subject?.name,
                guid = aspect.guid ?: "?",
                id = aspect.id,
                parentAspect = null,
                source = AspectHintSource.REFBOOK_DESCRIPTION.toString()
            )
        }
    }

    private fun AspectPropertyVertex.toAspectHint(): AspectHint? {
        return parentAspect?.let { parent ->
            aspectDao.find(this.aspect)?.let { aspect ->
                AspectHint(
                    name = parent.fullName(),
                    description = parent.description,
                    refBookItem = null,
                    refBookItemDesc = null,
                    subAspectName = aspect.fullName(),
                    aspectName = aspect.description,
                    subjectName = parent.subject?.name,
                    parentAspect = AspectHintAspectInfo(
                        id = parent.id,
                        guid = parent.guidSoft(),
                        name = parent.name,
                        description = null,
                        subjectName = parent.subject?.name ?: "Global"
                    ),
                    guid = parent.guid ?: "?",
                    source = AspectHintSource.ASPECT_PROPERTY_WITH_ASPECT.toString(),
                    id = this.aspect,
                    property = AspectHintAspectPropInfo(
                        guid = this.guidSoft(),
                        id = this.id,
                        name = this.name,
                        description = this.description,
                        cardinality = this.cardinality
                    )
                )
            }
        }
    }

}

private val logger = loggerFor<SuggestionService>()