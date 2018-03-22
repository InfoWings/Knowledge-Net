package com.infowings.catalog.data

import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.data.aspect.Aspect
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.aspect.toAspectVertex
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OEdge
import com.orientechnologies.orient.core.record.OVertex

class SubjectService(private val db: OrientDatabase, private val aspectService: AspectService) {

    fun getSubjects(): List<Subject> = db.query(selectSubjects) { rs ->
        rs.mapNotNull { it.toVertexOrNull()?.toSubject() }.toList()
    }

    fun getSubject(vertex: OVertex): Subject = vertex.toSubject()

    fun findByName(name: String): Subject? = db.query(SELECT_BY_NAME, SUBJECT_CLASS, name) { rs ->
        rs.map { it.toVertex().toSubject() }.firstOrNull()
    }

    fun createSubject(sd: SubjectData): Subject =
        findByName(sd.name)?.let { throw SubjectWithNameAlreadyExist(sd.name) } ?: save(sd)

    fun updateSubject(sd: SubjectData): Subject =
        transaction(db) {
            val vertex: OVertex = db[sd.id ?: throw SubjectIdIsNull()]
            vertex.name = sd.name
            sd.aspects.forEach { aspectData ->
                val aspectId = aspectData.id ?: aspectService.save(aspectData).id
                db[aspectId].subjects().find { it.id == vertex.id } ?: db[aspectId].addEdge(
                    vertex,
                    ASPECT_SUBJECT_EDGE
                ).save<OEdge>()
            }
            vertex.save<OVertex>().toSubject()
        }

    private fun OVertex.toSubject(): Subject =
        Subject(this.id, this.name, this.aspects())

    private fun OVertex.aspects(): List<Aspect> =
        this.getVertices(ODirection.IN, ASPECT_SUBJECT_EDGE).map { aspectService.getAspect(it.toAspectVertex()) }

    private fun OVertex.subjects(): Iterable<OVertex> =
        this.getVertices(ODirection.OUT, ASPECT_SUBJECT_EDGE)

    private fun save(sd: SubjectData): Subject =
        transaction(db) { session ->
            val vertex: OVertex = session.newVertex(SUBJECT_CLASS)
            vertex.name = sd.name
            sd.aspects.forEach { aspectData ->
                val aspectId = aspectData.id ?: aspectService.save(aspectData).id
                db[aspectId].addEdge(vertex, ASPECT_SUBJECT_EDGE).save<OEdge>()
            }
            vertex.save<OVertex>()
            session.commit() //TODO ????
            return@transaction vertex.toSubject()
        }

}

const val selectSubjects = "SELECT FROM $SUBJECT_CLASS"

class SubjectIdIsNull : Throwable()
class SubjectWithNameAlreadyExist(name: String) : Throwable("Subject already exist: $name")
