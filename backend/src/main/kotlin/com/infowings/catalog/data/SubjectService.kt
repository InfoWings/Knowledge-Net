package com.infowings.catalog.data

import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.data.aspect.Aspect
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OEdge
import com.orientechnologies.orient.core.record.OVertex

class SubjectService(private val db: OrientDatabase, private val aspectService: AspectService) {

    fun getSubjects(): List<Subject> = db.query("SELECT FROM $SUBJECT_CLASS") { rs ->
        rs.mapNotNull { it.toVertexOrNull()?.toSubject() }.toList()
    }

    fun getSubject(vertex: OVertex): Subject = vertex.toSubject()

    private fun OVertex.toSubject(): Subject =
        Subject(this.id, this[ATTR_NAME], getAspects(this))

    private fun getAspects(subjectVertex: OVertex): List<Aspect> =
        subjectVertex.getEdges(ODirection.IN, ASPECT_SUBJECT_EDGE)
            .map { aspectService.getAspect(it.from) }

    fun findByName(name: String): Subject? = db.query(SELECT_BY_NAME, SUBJECT_CLASS, name) { rs ->
        rs.map { it.toVertex().toSubject() }.firstOrNull()
    }

    fun createSubject(sd: SubjectData): Subject =
        if (findByName(sd.name) == null) {
            save(sd)
        } else {
            throw SubjectWithNameAlreadyExist(sd.name)
        }

    private fun save(sd: SubjectData): Subject =
        transaction(db) { session ->
            val vertex: OVertex = session.newVertex(SUBJECT_CLASS)
            vertex[ATTR_NAME] = sd.name
            sd.aspects.forEach { aspectData ->
                val aspectId = aspectData.id ?: aspectService.save(aspectData).id
                db[aspectId].addEdge(vertex, ASPECT_SUBJECT_EDGE).save<OEdge>()
            }
            vertex.save<OVertex>()
            session.commit() //TODO ????
            return@transaction vertex.toSubject()
        }

    class SubjectWithNameAlreadyExist(name: String) : Throwable("Subject already exist: $name") {}

    fun updateSubject(sd: SubjectData): Subject =
        transaction(db) {
            val vertex: OVertex = db[sd.id ?: throw SubjectIdIsNull]
            vertex[ATTR_NAME] = sd.name
            sd.aspects.forEach { aspectData ->
                val aspectId = aspectData.id ?: aspectService.save(aspectData).id
                if (db[aspectId].getEdges(ODirection.OUT, ASPECT_SUBJECT_EDGE).find { it.to.id == vertex.id } == null) {
                    db[aspectId].addEdge(vertex, ASPECT_SUBJECT_EDGE).save<OEdge>()
                }
            }
            vertex.save<OVertex>()
            vertex.toSubject()
        }

    object SubjectIdIsNull : Throwable()

}

