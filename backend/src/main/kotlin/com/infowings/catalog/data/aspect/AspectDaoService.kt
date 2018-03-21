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

class AspectDaoService(private val db: OrientDatabase, private val measureService: MeasureService) {

    fun createNewAspectVertex() = db.createNewVertex(ASPECT_CLASS)

    fun getAspectVertex(aspectId: String) = db.getVertexById(aspectId)

    fun createNewAspectPropertyVertex() = db.createNewVertex(ASPECT_PROPERTY_CLASS)

    fun getAspectPropertyVertex(aspectPropertyId: String) = db.getVertexById(aspectPropertyId)

    fun findByName(name: String): Set<OVertex> = db.query(selectAspectByName, name) { rs ->
        rs.map { it.toVertex() }.toSet()
    }

    fun getAspects(): Set<OVertex> = db.query(selectFromAspect) { rs -> rs.mapNotNull { it.toVertexOrNull() }.toSet() }

    fun saveAspect(aspectVertex: OVertex, aspectData: AspectData): OVertex = session(db) {
        logger.debug("Saving aspect ${aspectData.name}, ${aspectData.measure}, ${aspectData.baseType}, ${aspectData.properties.size}")

        aspectVertex.name = aspectData.name ?: throw AspectNameCannotBeNull()

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

        aspectData.subject?.id?.let { aspectVertex.addEdge(db[it], ASPECT_SUBJECT_EDGE).save<OEdge>() }

        return@session aspectVertex.save<OVertex>().also {
            logger.debug("Aspect ${aspectData.name} saved with id: ${it.id}")
        }
    }

    fun saveAspectProperty(ownerAspectVertex: OVertex,
                           aspectPropertyVertex: OVertex,
                           aspectPropertyData: AspectPropertyData): OVertex = transaction(db) {

        logger.debug("Saving aspect property ${aspectPropertyData.name} linked with aspect ${aspectPropertyData.aspectId}")

        val aspectVertex: OVertex = db.getVertexById(aspectPropertyData.aspectId)
                ?: throw AspectDoesNotExist(aspectPropertyData.aspectId)

        val cardinality = AspectPropertyCardinality.valueOf(aspectPropertyData.cardinality)

        aspectPropertyVertex.name = aspectPropertyData.name
        aspectPropertyVertex.aspect = aspectPropertyData.aspectId
        aspectPropertyVertex.cardinality = cardinality.name

        // it is not aspectPropertyVertex.properties in mind. This links describe property->aspect relation
        if (!aspectPropertyVertex.getVertices(ODirection.OUT, ASPECT_ASPECTPROPERTY_EDGE).contains(aspectVertex)) {
            aspectPropertyVertex.addEdge(aspectVertex, ASPECT_ASPECTPROPERTY_EDGE).save<OEdge>()
        }

        if (!ownerAspectVertex.properties.contains(aspectPropertyVertex)) {
            ownerAspectVertex.addEdge(aspectPropertyVertex, ASPECT_ASPECTPROPERTY_EDGE).save<OEdge>()
        }

        return@transaction aspectPropertyVertex.save<OVertex>().also {
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
}

private const val selectWithNameDifferentId = "SELECT from $ASPECT_CLASS WHERE name=:name and (@rid <> :aspectId)"
private const val selectFromAspect = "SELECT FROM Aspect"
private const val selectAspectByName = "SELECT FROM Aspect where name = ? "

private val logger = loggerFor<AspectDaoService>()