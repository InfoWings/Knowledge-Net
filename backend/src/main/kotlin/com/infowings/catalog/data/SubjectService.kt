package com.infowings.catalog.data

import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.OVertex

fun OVertex.toSubject(): Subject =
    Subject(this.id, this.name, this.description)

class SubjectService(private val db: OrientDatabase) {

    fun getSubjects(): List<Subject> = db.query(selectSubjects) { rs ->
        rs.mapNotNull { it.toVertexOrNull()?.toSubject() }.toList()
    }

    fun findByName(name: String): Subject? = db.query(SELECT_BY_NAME, SUBJECT_CLASS, name) { rs ->
        rs.map { it.toVertex().toSubject() }.firstOrNull()
    }

    fun createSubject(sd: SubjectData): Subject =
        transaction(db) {
            findByName(sd.name)?.let { throw SubjectWithNameAlreadyExist(sd.name) } ?: save(sd)
        }.toSubject()

    fun updateSubject(sd: SubjectData): Subject =
        transaction(db) {
            val vertex: OVertex = db[sd.id ?: throw SubjectIdIsNull()]
            vertex.name = sd.name
            vertex.description = sd.description
            vertex.save<OVertex>()
        }.toSubject()

    private fun save(sd: SubjectData): OVertex =
        transaction(db) { session ->
            val vertex: OVertex = session.newVertex(SUBJECT_CLASS)
            vertex.name = sd.name
            vertex.description = sd.description
            return@transaction vertex.save<OVertex>()
        }
}

const val selectSubjects = "SELECT FROM $SUBJECT_CLASS"

class SubjectIdIsNull : Throwable()
class SubjectWithNameAlreadyExist(name: String) : Throwable("Subject already exist: $name")
