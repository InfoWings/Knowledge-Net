package com.infowings.catalog.data

import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.OVertex

fun OVertex.toSubject(): Subject =
    Subject(this.id, this.name)

class SubjectService(private val db: OrientDatabase, private val aspectService: AspectService) {

    fun getSubjects(): List<Subject> = db.query(selectSubjects) { rs ->
        rs.mapNotNull { it.toVertexOrNull()?.toSubject() }.toList()
    }

    fun getSubject(vertex: OVertex): Subject = vertex.toSubject()

    fun findByName(name: String): Subject? = db.query(SELECT_BY_NAME, SUBJECT_CLASS, name) { rs ->
        rs.map { it.toVertex().toSubject() }.firstOrNull()
    }

    fun createSubject(sd: SubjectData): Subject {
        val vertex = transaction(db) {
            findByName(sd.name)?.let { throw SubjectWithNameAlreadyExist(sd.name) } ?: save(sd)
        }
        return vertex.toSubject()
    }

    fun updateSubject(sd: SubjectData): Subject =
        transaction(db) {
            val vertex: OVertex = db[sd.id ?: throw SubjectIdIsNull()]
            vertex.name = sd.name
            vertex.save<OVertex>().toSubject()
        }

    private fun save(sd: SubjectData): OVertex =
        transaction(db) { session ->
            val vertex: OVertex = session.newVertex(SUBJECT_CLASS)
            vertex.name = sd.name
            vertex.save<OVertex>()
            return@transaction vertex
        }
}

const val selectSubjects = "SELECT FROM $SUBJECT_CLASS"

class SubjectIdIsNull : Throwable()
class SubjectWithNameAlreadyExist(name: String) : Throwable("Subject already exist: $name")
