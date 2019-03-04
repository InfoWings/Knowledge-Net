package com.infowings.catalog.data.subject

import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.data.Subject
import com.infowings.catalog.data.SubjectNotFoundException
import com.infowings.catalog.data.SubjectWithNameAlreadyExist
import com.infowings.catalog.data.guid.toGuidVertex
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.id.ORID
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.record.ODirection
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
        deleted = this.deleted,
        guid = this.guid
    )

class SubjectDao(private val db: OrientDatabase) {

    init {
        //todo: migration code for #395
        // subjects guid
        transaction(db) {

            db.query("SELECT FROM $SUBJECT_CLASS") { rs ->
                val subjects = rs.mapNotNull { it.toVertexOrNull()?.toSubjectVertex() }.toList()
                for (subjectVertex in subjects) {
                    val guid = subjectVertex.getVertices(ODirection.OUT, "GuidOfSubjectEdge").singleOrNull()?.toGuidVertex()?.guid
                    if (subjectVertex.getProperty<String>(ATTR_GUID) == null) {
                        subjectVertex.setProperty(ATTR_GUID, guid)
                        subjectVertex.save<OVertex>()
                    }
                }
            }
        }

    }

    fun getSubjects(): List<Subject> = db.query(SelectSubjectsQuery) { rs ->
        rs.mapNotNull { it.toVertexOrNull()?.toSubjectVertex()?.toSubject() }.toList()
    }

    fun findByName(name: String): SubjectVertex? = db.query(SELECT_BY_NAME, SUBJECT_CLASS, name) { rs ->
        rs.map { it.toVertex().toSubjectVertex() }.firstOrNull()
    }

    fun findById(id: String): SubjectVertex? = transaction(db) {
        try {
            db[id].toSubjectVertex()
        } catch (e: VertexNotFound) {
            null
        }
    }

    fun find(ids: List<ORID>): List<SubjectVertex> {
        return db.query(
            "select from $SUBJECT_CLASS where @rid in :ids ", mapOf("ids" to ids)
        ) { rs ->
            rs.mapNotNull {
                it.toVertexOrNull()?.toSubjectVertex()
            }.toList()
        }
    }

    fun findStr(ids: List<String>): List<SubjectVertex> = find(ids.map { ORecordId(it) })

    fun findByIdStrict(id: String): SubjectVertex = transaction(db) {
        try {
            db[id].toSubjectVertex()
        } catch (e: VertexNotFound) {
            throw SubjectNotFoundException(id)
        }
    }

    private fun newSubjectVertex(): SubjectVertex = db.createNewVertex(SUBJECT_CLASS).assignGuid().toSubjectVertex()

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
            if (sd.name != vertex.name) {
                findByName(sd.name)?.let { throw SubjectWithNameAlreadyExist(it.toSubject()) }
            }
            vertex.name = sd.name
            vertex.description = sd.description
            vertex.save<SubjectVertex>().toSubjectVertex()
        }

    fun remove(vertex: OVertex) {
        db.delete(vertex)
    }

    fun softRemove(vertex: SubjectVertex) {
        transaction(db) {
            vertex.deleted = true
            vertex.save<OVertex>()
        }
    }
}