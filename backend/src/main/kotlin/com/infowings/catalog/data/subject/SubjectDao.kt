package com.infowings.catalog.data.subject

import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.data.Subject
import com.infowings.catalog.data.SubjectNotFoundException
import com.infowings.catalog.data.SubjectWithNameAlreadyExist
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.OVertex
import notDeletedSql

private const val SelectSubjectsQuery = "SELECT FROM $SUBJECT_CLASS where $notDeletedSql"
private const val SELECT_BY_NAME = "SELECT FROM ? where $ATTR_NAME = ? and $notDeletedSql "

fun SubjectVertex.toSubject(): Subject =
    Subject(
        this.id,
        name = this.name,
        version = this.version,
        description = this.description,
        deleted = this.deleted
    )

class SubjectDao(private val db: OrientDatabase) {
    fun getSubjects(): List<Subject> = db.query(SelectSubjectsQuery) { rs ->
        rs.mapNotNull { it.toVertexOrNull()?.toSubjectVertex()?.toSubject() }.toList()
    }

    fun findByName(name: String): SubjectVertex? = db.query(SELECT_BY_NAME, SUBJECT_CLASS, name) { rs ->
        rs.map { it.toVertex().toSubjectVertex() }.firstOrNull()
    }

    fun findById(id: String): SubjectVertex? = try {
        db[id].toSubjectVertex()
    } catch (e: VertexNotFound) {
        null
    }

    fun findByIdStrict(id: String): SubjectVertex = try {
        db[id].toSubjectVertex()
    } catch (e: VertexNotFound) {
        throw SubjectNotFoundException(id)
    }

    private fun newSubjectVertex(): SubjectVertex = db.createNewVertex(SUBJECT_CLASS).toSubjectVertex()

    private fun save(sd: SubjectData): SubjectVertex = transaction(db) {
        val vertex: SubjectVertex = newSubjectVertex()
        vertex.name = sd.name
        vertex.description = sd.description
        vertex.save<SubjectVertex>().toSubjectVertex()
        return@transaction vertex.save<SubjectVertex>().toSubjectVertex()
    }

    fun createSubject(sd: SubjectData): SubjectVertex =
        transaction(db) {
            findByName(sd.name)?.let { throw SubjectWithNameAlreadyExist(it.toSubject()) } ?: save(sd)
        }

    fun updateSubjectVertex(vertex: SubjectVertex, sd: SubjectData): SubjectVertex =
        transaction(db) {
            vertex.name = sd.name
            vertex.description = sd.description
            vertex.save<SubjectVertex>().toSubjectVertex()
        }

    fun remove(vertex: OVertex) {
        db.delete(vertex)
    }

    fun softRemove(vertex: SubjectVertex) {
        session(db) {
            vertex.deleted = true
            vertex.save<OVertex>()
        }
    }
}