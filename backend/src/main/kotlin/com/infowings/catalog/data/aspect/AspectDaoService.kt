package com.infowings.catalog.data.aspect

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import com.infowings.catalog.data.MeasureService
import com.infowings.catalog.loggerFor
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OEdge
import com.orientechnologies.orient.core.record.OVertex

/** Should be used externally for query building. */
const val notDeletedSql = "(deleted is NULL or deleted = false)"
const val selectWithNameDifferentId =
    "SELECT from $ASPECT_CLASS WHERE name=:name and (@rid <> :aspectId) and $notDeletedSql"
const val selectFromAspectWithoutDeleted = "SELECT FROM Aspect WHERE $notDeletedSql"
const val selectFromAspectWithDeleted = "SELECT FROM Aspect"
const val selectAspectByName = "SELECT FROM Aspect where name = ? AND $notDeletedSql"

class AspectDaoService(private val db: OrientDatabase, private val measureService: MeasureService) {

    fun createNewAspectVertex() = db.createNewVertex(ASPECT_CLASS).toAspectVertex()

    fun getVertex(id: String): OVertex? = db.getVertexById(id)

    fun getAspectVertex(aspectId: String) = db.getVertexById(aspectId)?.toAspectVertex()

    fun createNewAspectPropertyVertex() = db.createNewVertex(ASPECT_PROPERTY_CLASS).toAspectPropertyVertex()

    fun getAspectPropertyVertex(aspectPropertyId: String) = getVertex(aspectPropertyId)?.toAspectPropertyVertex()

    fun findByName(name: String): Set<AspectVertex> = db.query(selectAspectByName, name) { rs ->
        rs.map { it.toVertex().toAspectVertex() }.toSet()
    }

    fun remove(vertex: OVertex) {
        db.delete(vertex)
    }

    fun fakeRemove(vertex: AspectVertex) {
        session(db) {
            vertex.deleted = true
            vertex.save<OVertex>()
        }
    }


    fun getAspects(): Set<AspectVertex> = db.query(selectFromAspectWithDeleted) { rs ->
        rs.mapNotNull { it.toVertexOrNull()?.toAspectVertex() }.toSet()
    }

    fun saveAspect(aspectVertex: AspectVertex, aspectData: AspectData): AspectVertex = session(db) {
        logger.debug("Saving aspect ${aspectData.name}, ${aspectData.measure}, ${aspectData.baseType}, ${aspectData.properties.size}")

        aspectVertex.name = aspectData.name?.trim() ?: throw AspectNameCannotBeNull()

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

        aspectData.subject?.id?.let {
            aspectVertex.getEdges(ODirection.OUT, ASPECT_SUBJECT_EDGE).toList().forEach { it.delete<OEdge>() }
            aspectVertex.addEdge(db[it], ASPECT_SUBJECT_EDGE).save<OEdge>()
        }

        return@session aspectVertex.save<OVertex>().toAspectVertex().also {
            logger.debug("Aspect ${aspectData.name} saved with id: ${it.id}")
        }
    }

    fun saveAspectProperty(
        ownerAspectVertex: AspectVertex,
        aspectPropertyVertex: AspectPropertyVertex,
        aspectPropertyData: AspectPropertyData
    ): AspectPropertyVertex = transaction(db) {

        logger.debug("Saving aspect property ${aspectPropertyData.name} linked with aspect ${aspectPropertyData.aspectId}")

        val aspectVertex: OVertex = db.getVertexById(aspectPropertyData.aspectId)
                ?: throw AspectDoesNotExist(aspectPropertyData.aspectId)

        val cardinality = AspectPropertyCardinality.valueOf(aspectPropertyData.cardinality)

        aspectPropertyVertex.name = aspectPropertyData.name.trim()
        aspectPropertyVertex.aspect = aspectPropertyData.aspectId
        aspectPropertyVertex.cardinality = cardinality.name

        // it is not aspectPropertyVertex.properties in mind. This links describe property->aspect relation
        if (!aspectPropertyVertex.getVertices(ODirection.OUT, ASPECT_ASPECTPROPERTY_EDGE).contains(aspectVertex)) {
            aspectPropertyVertex.addEdge(aspectVertex, ASPECT_ASPECTPROPERTY_EDGE).save<OEdge>()
        }

        if (!ownerAspectVertex.properties.contains(aspectPropertyVertex)) {
            ownerAspectVertex.addEdge(aspectPropertyVertex, ASPECT_ASPECTPROPERTY_EDGE).save<OEdge>()
        }

        return@transaction aspectPropertyVertex.save<OVertex>().toAspectPropertyVertex().also {
            logger.debug("Saved aspect property ${aspectPropertyData.name} with temporary id: ${it.id}")
        }
    }

    fun getAspectsByNameAndSubjectWithDifferentId(name: String, subjectId: String?, id: String?): Set<OVertex> {
        val q = if (subjectId == null) {
            selectWithNameDifferentId
        } else {
            "$selectWithNameDifferentId and (@rid in (select out.@rid from $ASPECT_SUBJECT_EDGE WHERE in.@rid = :subjectId))"
        }
        val args: Map<String, Any?> =
            mapOf("name" to name, "aspectId" to ORecordId(id), "subjectId" to ORecordId(subjectId))
        return db.query(q, args) { it.map { it.toVertex() }.toSet() }
    }

    /**
     * @param aspectId aspect id to start
     * @return list of the current aspect and all its parents
     */
    fun findParentAspects(aspectId: String): List<AspectData> = session(db) {
        val q = "traverse in(\"$ASPECT_ASPECTPROPERTY_EDGE\").in() FROM :aspectRecord"
        return@session db.query(q, mapOf("aspectRecord" to ORecordId(aspectId))) {
            it.mapNotNull { it.toVertexOrNull()?.toAspectVertex()?.toAspectData() }.toList()
        }
    }

}

private val logger = loggerFor<AspectDaoService>()