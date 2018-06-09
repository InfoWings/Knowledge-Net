package com.infowings.catalog.data

import com.infowings.catalog.auth.user.UserService
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.data.history.HistoryContext
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.data.subject.SubjectDao
import com.infowings.catalog.data.subject.SubjectVertex
import com.infowings.catalog.data.subject.toSubject
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.transaction

class SubjectService(
    private val db: OrientDatabase,
    private val dao: SubjectDao,
    private val history: HistoryService,
    private val userService: UserService
) {
    fun getSubjects(): List<Subject> = dao.getSubjects()

    fun findById(id: String): SubjectVertex? = dao.findById(id)

    fun findByIdStrict(id: String): SubjectVertex = dao.findByIdStrict(id)

    fun findByName(name: String): SubjectData? = dao.findByName(name)?.toSubject()?.toSubjectData()

    fun createSubject(sd: SubjectData, username: String): Subject {
        val userVertex = userService.findUserVertexByUsername(username)

        val vertex = transaction(db) {
            val vertex = dao.createSubject(sd)
            history.storeFact(vertex.toCreateFact(HistoryContext(userVertex)))
            return@transaction vertex
        }

        return vertex.toSubject()
    }

    fun updateSubject(subjectData: SubjectData, username: String): Subject {
        val id = subjectData.id ?: throw SubjectIdIsNull

        val userVertex = userService.findUserVertexByUsername(username)

        val resultVertex = transaction(db) {
            val vertex: SubjectVertex = dao.findByIdStrict(id)
            if (subjectData == vertex.toSubject().toSubjectData()) {
                throw SubjectEmptyChangeException()
            }

            // временно отключим до гарантированной поддержки на фронте
            //if (subjectData.isModified(vertex.version)) {
            //    throw SubjectConcurrentModificationException(expected =  sd.version, real = vertex.version)
            //}

            val before = vertex.currentSnapshot()
            val res = dao.updateSubjectVertex(vertex, subjectData)
            history.storeFact(vertex.toUpdateFact(HistoryContext(userVertex), before))

            return@transaction res
        }

        return resultVertex.toSubject()
    }

    fun remove(subjectData: SubjectData, username: String, force: Boolean = false) {
        val id = subjectData.id ?: throw SubjectIdIsNull

        val userVertex = userService.findUserVertexByUsername(username)

        transaction(db) {
            val vertex = dao.findByIdStrict(id)

            // временно отключим до гарантированной поддержки на фронте
            //if (subjectData.isModified(vertex.version)) {
            //    throw SubjectConcurrentModificationException(expected =  sd.version, real = vertex.version)
            //}

            val linkedByAspects = vertex.linkedByAspects()

            when {
                linkedByAspects.isNotEmpty() && force -> {
                    history.storeFact(vertex.toSoftDeleteFact(HistoryContext(userVertex)))
                    dao.softRemove(vertex)
                }
                linkedByAspects.isNotEmpty() -> {
                    throw SubjectIsLinkedByAspect(subjectData, linkedByAspects.first().toAspectData())
                }

                else -> {
                    history.storeFact(vertex.toDeleteFact(HistoryContext(userVertex)))
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

class SubjectEmptyChangeException : SubjectException()
