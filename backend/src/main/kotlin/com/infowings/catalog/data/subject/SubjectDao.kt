package com.infowings.catalog.data.subject

import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.data.Subject
import com.infowings.catalog.data.SubjectIdIsNull
import com.infowings.catalog.data.SubjectWithNameAlreadyExist
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.OVertex

private const val SelectSubjectsQuery = "SELECT FROM $SUBJECT_CLASS"
private const val SELECT_BY_NAME = "SELECT FROM ? where $ATTR_NAME = ? "

fun OVertex.toSubject(): Subject =
    Subject(this.id, this.name, this.description)

class SubjectDao(private val db: OrientDatabase) {
    fun getSubjects(): List<Subject> = db.query(SelectSubjectsQuery) { rs ->
        rs.mapNotNull { it.toVertexOrNull()?.toSubject() }.toList()
    }

    fun findByName(name: String): Subject? = db.query(SELECT_BY_NAME, SUBJECT_CLASS, name) { rs ->
        rs.map { it.toVertex().toSubject() }.firstOrNull()
    }

    private fun newSubjectVertex(): SubjectVertex = db.createNewVertex(SUBJECT_CLASS).toSubjectVertex()


    private fun save(sd: SubjectData): SubjectVertex =
        transaction(db) { session ->
            val vertex: SubjectVertex = newSubjectVertex()
            vertex.name = sd.name
            vertex.description = sd.description
            vertex.save<SubjectVertex>().toSubjectVertex()
            return@transaction vertex.save<SubjectVertex>().toSubjectVertex()
        }

    fun createSubject(sd: SubjectData): Subject =
        transaction(db) {
            findByName(sd.name)?.let { throw SubjectWithNameAlreadyExist(it) } ?: save(sd)
        }.toSubject()

    fun updateSubject(sd: SubjectData): Subject =
        transaction(db) {
            val vertex: SubjectVertex = db[sd.id ?: throw SubjectIdIsNull()].toSubjectVertex()
            vertex.name = sd.name
            vertex.description = sd.description
            vertex.save<SubjectVertex>().toSubjectVertex()
        }.toSubject()
}