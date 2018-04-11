package com.infowings.catalog.data

import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.data.subject.SubjectDao
import com.infowings.catalog.data.subject.SubjectVertex
import com.infowings.catalog.data.subject.toSubject
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.transaction


class SubjectService(private val db: OrientDatabase, private val dao: SubjectDao, private val history: HistoryService) {
    fun getSubjects(): List<Subject> = dao.getSubjects()

    fun createSubject(sd: SubjectData): Subject {
        val vertex = transaction(db) {
            val vertex = dao.createSubject(sd)
            history.storeFact(vertex.toCreateFact(""))
            return@transaction vertex
        }

        return vertex.toSubject()
    }

    fun updateSubject(sd: SubjectData): Subject {
        val id = sd.id ?: throw SubjectIdIsNull()

        val resultVertex = transaction(db) {
            val vertex: SubjectVertex = dao.findById(id) ?: throw SubjectNotFoundException(id)
            val before = vertex.currentSnapshot()
            val res = dao.updateSubjectVertex(vertex, sd)
            history.storeFact(vertex.toUpdateFact("", before))

            return@transaction res
        }

        return resultVertex.toSubject()
    }
}

class SubjectIdIsNull : Throwable()
class SubjectWithNameAlreadyExist(val subject: Subject) : Throwable("Subject already exist: ${subject.name}")
class SubjectNotFoundException(val id: String) : Throwable("Subject with id $id not found")
