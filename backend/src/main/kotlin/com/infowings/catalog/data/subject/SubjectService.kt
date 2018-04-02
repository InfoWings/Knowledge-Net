package com.infowings.catalog.data.subject

import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.OVertex

class SubjectService(private val db: OrientDatabase, private val aspectService: AspectService) {

    fun getSubjects(): List<Subject> = db.query(selectSubjects) { rs ->
        rs.mapNotNull { it.toVertexOrNull()?.toSubjectVertex()?.toSubject() }.toList()
    }

    fun getSubject(vertex: SubjectVertex): Subject = vertex.toSubject()

    fun findByName(name: String): Subject? = db.query(SELECT_BY_NAME, SUBJECT_CLASS, name) { rs ->
        rs.map { it.toVertex().toSubjectVertex().toSubject() }.firstOrNull()
    }

    fun createSubject(sd: SubjectData): Subject =
        transaction(db) {
            findByName(sd.name)?.let { throw SubjectWithNameAlreadyExist(sd.name) } ?: save(sd)
        }.toSubjectVertex().toSubject()

    fun updateSubject(sd: SubjectData): Subject =
        transaction(db) {
            val vertex: SubjectVertex = db[sd.id ?: throw SubjectIdIsNull()].toSubjectVertex()
            vertex.name = sd.name
            vertex.save<OVertex>().toSubjectVertex().toSubject()
        }

    private fun save(sd: SubjectData): OVertex =
        transaction(db) { session ->
            val vertex: SubjectVertex = session.newVertex(SUBJECT_CLASS).toSubjectVertex()
            vertex.name = sd.name
            return@transaction vertex.save<OVertex>()
        }
}

const val selectSubjects = "SELECT FROM $SUBJECT_CLASS"

class SubjectIdIsNull : Throwable()
class SubjectWithNameAlreadyExist(name: String) : Throwable("Subject already exist: $name")
