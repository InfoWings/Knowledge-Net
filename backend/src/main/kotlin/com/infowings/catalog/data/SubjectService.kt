package com.infowings.catalog.data

import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.ODirection
import com.orientechnologies.orient.core.record.OEdge
import com.orientechnologies.orient.core.record.OVertex

class SubjectService(private val db: OrientDatabase, private val aspectService: AspectService) {

    fun getSubjects(): List<Subject> = db.query("SELECT FROM $SUBJECT_CLASS") { rs ->
        rs.mapNotNull { it.toVertexOrNull()?.toSubject() }.toList()
    }

    private fun OVertex.toSubject(): Subject =
        Subject(this.id, this[ATTR_NAME], getAspects(this))

    private fun getAspects(vertex: OVertex): List<Aspect> =
        vertex.getEdges(ODirection.IN, ASPECT_SUBJECT_EDGE)
            .map { it.from }
            .map { aspectService.getAspect(it) }

    fun findByName(name: String): Subject? = db.query(SELECT_BY_NAME, SUBJECT_CLASS, name) { rs ->
        rs.map { it.toVertex().toSubject() }.firstOrNull()
    }

    fun createSubject(sd: SubjectData): Subject =
        if (findByName(sd.name) == null) {
            save(sd)
        } else {
            throw SubjectWithNameAlreadyExist(sd.name)
        }

    private fun save(sd: SubjectData): Subject {
        val res: Subject =
            transaction(db) { session ->
                val vertex: OVertex = session.newVertex(SUBJECT_CLASS)
                vertex[ATTR_NAME] = sd.name
                sd.aspectIds.forEach { aspectId ->
                    db[aspectId].addEdge(vertex, ASPECT_SUBJECT_EDGE).save<OEdge>()
                }
                vertex.save<OVertex>()
                vertex.toSubject()
            }
        return res
    }

    class SubjectWithNameAlreadyExist(name: String) : Throwable("Subject already exist: $name") {}

}
