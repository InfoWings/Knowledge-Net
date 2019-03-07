package com.infowings.catalog.data.aspect

import com.infowings.catalog.common.*
import com.infowings.catalog.data.MeasureService
import com.infowings.catalog.data.reference.book.ASPECT_REFERENCE_BOOK_EDGE
import com.infowings.catalog.external.logTime
import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.*
import com.infowings.catalog.utils.toNullable
import com.orientechnologies.orient.core.id.ORID
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OEdge
import com.orientechnologies.orient.core.record.OVertex
import com.orientechnologies.orient.core.sql.executor.OResult
import notDeletedSql
import java.time.Instant

/** Should be used externally for query building. */
const val selectWithNameDifferentId = "SELECT from $ASPECT_CLASS WHERE name = :name and (@rid <> :aspectId) and $notDeletedSql"
const val selectWithName = "SELECT from $ASPECT_CLASS WHERE name = :name and $notDeletedSql"
const val selectFromAspectWithoutDeleted = "SELECT FROM $ASPECT_CLASS WHERE $notDeletedSql"
const val selectFromAspectWithDeleted = "SELECT FROM $ASPECT_CLASS"

data class AspectDaoDetails(val subject: SubjectData?, val refBookName: String?, val propertyIds: List<ORID>)

class AspectDaoService(private val db: OrientDatabase, private val measureService: MeasureService) {

    init {
        //todo: migration code for #395
        // aspects guid
        transaction(db) {
            val aspectIds = db.query("select @rid from Aspect") { it.map { it.getProperty<ORecordId>("@rid") }.toList() }
            for (id in aspectIds) {
                val vertex = getVertex(id.toString())!!
                val guid = vertex.getVertices(ODirection.OUT, "GuidOfAspectEdge").singleOrNull()?.getProperty<String>(ATTR_GUID)
                if (vertex.getProperty<String>(ATTR_GUID) == null) {
                    vertex.setProperty(ATTR_GUID, guid)
                    vertex.save<OVertex>()
                }
            }
        }
        // aspect properties guid
        transaction(db) {
            val aspectProperties = db.query("select @rid from $ASPECT_PROPERTY_CLASS") { it.map { it.getProperty<ORecordId>("@rid") }.toList() }
            for (id in aspectProperties) {
                val vertex = getVertex(id.toString())!!
                val guid = vertex.getVertices(ODirection.OUT, "GuidOfAspectPropertyEdge").singleOrNull()?.getProperty<String>(ATTR_GUID)
                if (vertex.getProperty<String>(ATTR_GUID) == null) {
                    vertex.setProperty(ATTR_GUID, guid)
                    vertex.save<OVertex>()
                }
            }
        }
        //todo: migration for #432
        transaction(db) {
            val aspectIds = db.query("select @rid from Aspect") { it.map { it.getProperty<ORecordId>("@rid") }.toList() }
            for (id in aspectIds) {
                val vertex = find(id.toString())!!
                val lastUpdate = vertex.lastChange
                if (vertex.getProperty<String>(ATTR_LAST_UPDATE) == null) {
                    vertex.setProperty(ATTR_LAST_UPDATE, lastUpdate!!.toEpochMilli())
                    vertex.save<OVertex>()
                }
            }
        }
    }

    fun createNewAspectVertex() = db.createNewVertex(ASPECT_CLASS).assignGuid().toAspectVertex()

    fun getVertex(id: String): OVertex? = db.getVertexById(id)

    fun find(id: String): AspectVertex? = transaction(db) {
        val v = db.getVertexById(id)
        return@transaction v?.toAspectVertex()
    }

    fun findStrict(id: String) = find(id) ?: throw AspectDoesNotExist(id)

    fun createNewAspectPropertyVertex() = db.createNewVertex(ASPECT_PROPERTY_CLASS).assignGuid().toAspectPropertyVertex()

    fun findProperty(id: String) = transaction(db) { getVertex(id)?.toAspectPropertyVertex() }

    fun findPropertyStrict(id: String) = findProperty(id) ?: throw AspectPropertyDoesNotExist(id)

    fun findByName(name: String): Set<AspectVertex> = db.query(selectWithName, mapOf("name" to name)) { rs ->
        rs.map { it.toVertex().toAspectVertex() }.toSet()
    }

    fun findAspectsByIds(ids: List<ORID>): List<AspectVertex> {
        return db.query(
            "select from $ASPECT_CLASS where @rid in :ids ", mapOf("ids" to ids)
        ) { rs ->
            rs.mapNotNull {
                it.toVertexOrNull()?.toAspectVertex()
            }.toList()
        }
    }

    fun findAspectsByIdsStr(ids: List<String>): List<AspectVertex> = findAspectsByIds(ids.map { ORecordId(it) })

    fun findPropertiesByIds(ids: List<ORID>): List<AspectPropertyVertex> {
        return db.query(
            "select from $ASPECT_PROPERTY_CLASS where @rid in :ids ", mapOf("ids" to ids)
        ) { rs ->
            rs.mapNotNull {
                it.toVertexOrNull()?.toAspectPropertyVertex()
            }.toList()
        }
    }

    fun findTransitiveByNameQuery(nameFragment: String): Set<AspectVertex> {
        val selectQuery = "$selectFromAspectWithoutDeleted AND name LUCENE :nameQuery"
        val traverseQuery = "TRAVERSE IN(\"$ASPECT_ASPECT_PROPERTY_EDGE\") FROM ($selectQuery)"
        val filterQuery = "SELECT FROM ($traverseQuery) WHERE @class = \"$ASPECT_CLASS\" AND $notDeletedSql"
        return logTime(logger, "transitive request by name") {
            db.query(filterQuery, mapOf("nameQuery" to "($nameFragment~) ($nameFragment*) (*$nameFragment*)")) {
                it.map { it.toVertex().toAspectVertex() }.toSet()
            }
        }
    }

    fun getAspectTreeForProperty(propertyRid: ORID): AspectTree =
        transaction(db) {
            val query = "TRAVERSE OUT(\"$ASPECT_ASPECT_PROPERTY_EDGE\") " +
                    "FROM (SELECT EXPAND(FIRST(OUT(\"$ASPECT_OBJECT_PROPERTY_EDGE\"))) FROM :propertyRid) " +
                    "STRATEGY DEPTH_FIRST"
            return@transaction db.query(query, mapOf("propertyRid" to propertyRid)) {
                val aspectTreeBuilder = AspectTreeBuilder()
                it.forEach { record ->
                    val vertex = record.toVertex()
                    when (vertex.schemaType.toNullable()?.name) {
                        ASPECT_CLASS -> {
                            val aspectVertex = vertex.toAspectVertex()
                            aspectTreeBuilder.apply { appendAspect(aspectVertex) }
                        }
                        ASPECT_PROPERTY_CLASS -> {
                            val propertyVertex = vertex.toAspectPropertyVertex()
                            aspectTreeBuilder.apply { appendAspectProperty(propertyVertex) }
                        }
                        else -> throw IllegalStateException("Illegal class name or link in storage: ${vertex.schemaType.toNullable()?.name}")
                    }
                }
                aspectTreeBuilder.buildAspectTree()
            }
        }

    fun getAspectTreeById(aspectRid: ORID): AspectTree =
        transaction(db) {
            val query = "TRAVERSE OUT(\"$ASPECT_ASPECT_PROPERTY_EDGE\") FROM :aspectRid STRATEGY DEPTH_FIRST"
            return@transaction db.query(query, mapOf("aspectRid" to aspectRid)) {
                val aspectTreeBuilder = AspectTreeBuilder()
                it.forEach { record ->
                    val vertex = record.toVertex()
                    when (vertex.schemaType.toNullable()?.name) {
                        ASPECT_CLASS -> {
                            val aspectVertex = vertex.toAspectVertex()
                            aspectTreeBuilder.apply { appendAspect(aspectVertex) }
                        }
                        ASPECT_PROPERTY_CLASS -> {
                            val propertyVertex = vertex.toAspectPropertyVertex()
                            aspectTreeBuilder.apply { appendAspectProperty(propertyVertex) }
                        }
                        else -> throw IllegalStateException("Illegal class name or link in storage: ${vertex.schemaType.toNullable()?.name}")
                    }
                }
                aspectTreeBuilder.buildAspectTree()
            }
        }

    fun remove(vertex: AspectPropertyVertex) {
        session(db) {
            db.delete(vertex)
        }
    }

    fun fakeRemove(vertex: AspectPropertyVertex) {
        transaction(db) {
            vertex.deleted = true
            vertex.save<OVertex>()
        }
    }

    fun remove(vertex: AspectVertex) {
        transaction(db) {
            vertex.properties.forEach {
                db.delete(it)
            }
            db.delete(vertex)
        }
    }


    fun fakeRemove(vertex: AspectVertex) {
        transaction(db) {
            vertex.properties.forEach {
                it.deleted = true
                it.save<OVertex>()
            }
            vertex.deleted = true
            updateAspectTimestamp(vertex)
            vertex.save<OVertex>()
        }
    }


    /**
     * all aspects including deleted required on front end to show aspect tree correctly
     */
    fun getAspects(): Set<AspectVertex> = logTime(logger, "all aspects extraction at dao level") {
        db.query(selectFromAspectWithDeleted) { rs ->
            rs.mapNotNull { it.toVertexOrNull()?.toAspectVertex() }.toSet()
        }
    }

    fun getAspectsWithDeleted(ids: List<ORID>): Set<AspectVertex> = logTime(logger, "aspects extraction with deleted at dao level") {
        db.query("SELECT FROM $ASPECT_CLASS WHERE @rid in :ids", mapOf("ids" to ids)) { rs ->
            rs.mapNotNull { it.toVertexOrNull()?.toAspectVertex() }.toSet()
        }
    }


    fun getProperties(ids: List<ORID>): Set<AspectPropertyVertex> = logTime(logger, "all properties extraction at dao level") {
        transaction(db) {
            db.query("select from  (traverse out(\"$ASPECT_ASPECT_PROPERTY_EDGE\") from :ids  maxdepth 1) where \$depth==1", mapOf("ids" to ids)) { rs ->
                rs.mapNotNull { it.toVertexOrNull()?.toAspectPropertyVertex() }.toSet()
            }
        }
    }

    fun getDetails(ids: List<ORID>): Map<String, AspectDaoDetails> = logTime(logger, "aspects details extraction at dao level") {
        val aliasPropIds = "propertyIds"
        val aliasSubjects = "subjectIds"
        val aliasRefBookNames = "refBookNames"
        val aliasId = "id"
        val aliasName = "name"
        val aliasDescription = "description"
        val aliasVersion = "version"

        db.query(
            "select" +
                    " @rid as $aliasId," +
                    " out('$ASPECT_ASPECT_PROPERTY_EDGE').@rid as $aliasPropIds," +
                    " out('$ASPECT_SUBJECT_EDGE'):{@rid as $aliasId, $aliasName, $aliasDescription, @version as $aliasVersion} as $aliasSubjects," +
                    " out('$ASPECT_REFERENCE_BOOK_EDGE').value as $aliasRefBookNames" +
                    "  from  :ids GROUP BY $aliasId ;", mapOf("ids" to ids)
        ) { rs ->
            rs.mapNotNull {
                it.toVertexOrNull()
                val aspectId = it.getProperty<ORID>(aliasId)

                val propertyIds: List<ORID> = it.getProperty(aliasPropIds)
                val subjects: List<OResult> = it.getProperty(aliasSubjects)
                val refBookNames: List<String> = it.getProperty(aliasRefBookNames)

                val subject = subjects.firstOrNull()?.let { subjectResult ->
                    SubjectData(
                        id = subjectResult.getProperty<ORID>(aliasId).toString(), name = subjectResult.getProperty(aliasName),
                        description = subjectResult.getProperty(aliasDescription), version = subjectResult.getProperty(aliasVersion),
                        deleted = false
                    )
                }

                aspectId.toString() to AspectDaoDetails(
                    propertyIds = propertyIds, subject = subject,
                    refBookName = refBookNames.firstOrNull()
                )
            }.toMap()
        }
    }

    fun saveAspect(aspectVertex: AspectVertex, aspectData: AspectData): AspectVertex = transaction(db) {
        logger.debug("Saving aspect ${aspectData.name}, ${aspectData.measure}, ${aspectData.baseType}, ${aspectData.properties.size}")

        aspectVertex.name = aspectData.name
        aspectVertex.description = aspectData.description

        aspectVertex.baseType = when (aspectData.measure) {
            null -> aspectData.baseType
            else -> null
        }

        aspectVertex.measureName = aspectData.measure

        val measureVertex: OVertex? = aspectData.measure?.let { measureService.findMeasure(it) }

        measureVertex?.let {
            if (!aspectVertex.getVertices(ODirection.OUT, ASPECT_MEASURE_CLASS).contains(it)) {
                aspectVertex.addEdge(it, ASPECT_MEASURE_CLASS).save<OEdge>()
            }
        }

        aspectVertex.getEdges(ODirection.OUT, ASPECT_SUBJECT_EDGE).toList().forEach { it.delete<OEdge>() }
        aspectData.subject?.id?.let {
            aspectVertex.addEdge(db[it], ASPECT_SUBJECT_EDGE).save<OEdge>()
        }
        updateAspectTimestamp(aspectVertex)
        return@transaction aspectVertex.save<OVertex>().toAspectVertex()
    }

    fun saveAspectProperty(
        ownerAspectVertex: AspectVertex,
        aspectPropertyVertex: AspectPropertyVertex,
        aspectPropertyData: AspectPropertyData
    ): AspectPropertyVertex = transaction(db) {

        logger.debug("Saving aspect property ${aspectPropertyData.name} linked with aspect ${aspectPropertyData.aspectId}")

        val aspectVertex: OVertex = db.getVertexById(aspectPropertyData.aspectId)
            ?: throw AspectDoesNotExist(aspectPropertyData.aspectId)

        val cardinality = try {
            PropertyCardinality.valueOf(aspectPropertyData.cardinality)
        } catch (exception: IllegalArgumentException) {
            throw AspectInconsistentStateException("Property has illegal cardinality value")
        }

        aspectPropertyVertex.name = aspectPropertyData.name
        aspectPropertyVertex.nameWithAspect = "${aspectPropertyData.name} , ${find(aspectPropertyData.aspectId)?.name ?: ""}"
        aspectPropertyVertex.aspect = aspectPropertyData.aspectId
        aspectPropertyVertex.cardinality = cardinality.name
        aspectPropertyVertex.description = aspectPropertyData.description

        // it is not aspectPropertyVertex.properties in mind. This links describe property -> aspect relation
        val currentAspectPropertyAspectVertex = aspectPropertyVertex.getVertices(ODirection.OUT, ASPECT_ASPECT_PROPERTY_EDGE).firstOrNull()
        if (currentAspectPropertyAspectVertex != aspectVertex) {
            if (currentAspectPropertyAspectVertex != null) {
                aspectPropertyVertex.getEdges(ODirection.OUT, ASPECT_ASPECT_PROPERTY_EDGE).forEach {
                    it.delete<OEdge>()
                }
            }
            aspectPropertyVertex.addEdge(aspectVertex, ASPECT_ASPECT_PROPERTY_EDGE).save<OEdge>()
        }

        if (!ownerAspectVertex.properties.contains(aspectPropertyVertex)) {
            ownerAspectVertex.addEdge(aspectPropertyVertex, ASPECT_ASPECT_PROPERTY_EDGE).save<OEdge>()
        }

        return@transaction aspectPropertyVertex.save<OVertex>().toAspectPropertyVertex().also {
            logger.debug("Saved aspect property ${aspectPropertyData.name} with temporary id: ${it.id}")
        }
    }


    fun getAspectsByNameAndSubjectWithDifferentId(name: String, subjectId: String?, id: String?): Set<AspectVertex> {
        val baseQuery = if (id != null) selectWithNameDifferentId else selectWithName

        val q = if (subjectId == null) {
            "$baseQuery and OUT(\"$ASPECT_SUBJECT_EDGE\").size() = 0"
        } else {
            "$baseQuery and (@rid in (select out.@rid from $ASPECT_SUBJECT_EDGE WHERE in.@rid = :subjectId))"
        }

        val args: Map<String, Any?> =
            mapOf("name" to name, "aspectId" to ORecordId(id), "subjectId" to ORecordId(subjectId))

        return db.query(q, args) { it.map { it.toVertex().toAspectVertex() }.toSet() }
    }

    /**
     * @param aspectId aspect id to start
     * @return list of the current aspect and all its parents
     */
    fun findParentAspects(aspectId: String): List<AspectData> = session(db) {
        val q = "select from (traverse in(\"$ASPECT_ASPECT_PROPERTY_EDGE\").in() FROM :aspectRecord) WHERE @class = \"$ASPECT_CLASS\""
        return@session db.query(q, mapOf("aspectRecord" to ORecordId(aspectId))) {
            it.mapNotNull {
                it.toVertexOrNull()?.toAspectVertex()?.toAspectData()
            }.toList()
        }
    }

    fun baseType(propertyVertex: AspectPropertyVertex): String? = transaction(db) {
        propertyVertex.associatedAspect.baseType
    }

    private fun updateAspectTimestamp(vertex: AspectVertex) {
        vertex.timestamp = Instant.now()
    }

    fun updateAndSaveAspectTimestamp(vertex: AspectVertex) {
        transaction(db) {
            vertex.timestamp = Instant.now()
            vertex.save<OVertex>()
        }
    }

}

private val logger = loggerFor<AspectDaoService>()