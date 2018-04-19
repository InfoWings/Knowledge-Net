package com.infowings.catalog.data

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.data.subject.SubjectDao
import com.infowings.catalog.data.subject.SubjectVertex
import com.infowings.catalog.data.subject.toSubject
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.transaction

class SubjectService(private val db: OrientDatabase, private val dao: SubjectDao, private val history: HistoryService) {
    fun getSubjects(): List<Subject> = dao.getSubjects()

    fun findById(id: String): SubjectVertex? = dao.findById(id)

    fun createSubject(sd: SubjectData, username: String): Subject {
        val vertex = transaction(db) {
            val vertex = dao.createSubject(sd)
            history.storeFact(vertex.toCreateFact(username))
            return@transaction vertex
        }

        return vertex.toSubject()
    }

    fun updateSubject(subjectData: SubjectData, username: String): Subject {
        val id = subjectData.id ?: throw SubjectIdIsNull

        val resultVertex = transaction(db) {
            val vertex: SubjectVertex = dao.findById(id) ?: throw SubjectNotFoundException(id)

            // временно отключим до гарантированной поддержки на фронте
            //if (subjectData.isModified(vertex.version)) {
            //    throw SubjectConcurrentModificationException(expected =  sd.version, real = vertex.version)
            //}

            val before = vertex.currentSnapshot()
            val res = dao.updateSubjectVertex(vertex, subjectData)
            history.storeFact(vertex.toUpdateFact(username, before))

            return@transaction res
        }

        return resultVertex.toSubject()
    }

    fun remove(subjectData: SubjectData, username: String, force: Boolean = false) {
        val id = subjectData.id ?: throw SubjectIdIsNull

        transaction(db) {
            val vertex = dao.findById(id) ?: throw SubjectNotFoundException(id)

            // временно отключим до гарантированной поддержки на фронте
            //if (subjectData.isModified(vertex.version)) {
            //    throw SubjectConcurrentModificationException(expected =  sd.version, real = vertex.version)
            //}

            val linkedByAspects = vertex.linkedByAspects()

            when {
                linkedByAspects.isNotEmpty() && force -> {
                    history.storeFact(vertex.toSoftDeleteFact(username))
                    dao.softRemove(vertex)
                }
                linkedByAspects.isNotEmpty() -> {
                    throw SubjectIsLinkedByAspect(subjectData, linkedByAspects.first().toAspectData())
                }

                else -> {
                    history.storeFact(vertex.toDeleteFact(username))
                    dao.remove(vertex)
                }
            }
        }
    }

}

sealed class SubjectException(override val message: String? = null) : Exception(message)

object SubjectIdIsNull : SubjectException()
class SubjectWithNameAlreadyExist(val subject: Subject) : SubjectException("Subject already exist: ${subject.name}")
class SubjectNotFoundException(val id: String) : SubjectException("Subject with id $id not found")
class SubjectConcurrentModificationException(expected: Int, real: Int) :
    SubjectException("Found version $real instead of $expected")
class SubjectIsLinkedByAspect(val subject: SubjectData, val aspect: AspectData) :
    SubjectException("Subject ${subject.id} is linked by ${aspect.id}")