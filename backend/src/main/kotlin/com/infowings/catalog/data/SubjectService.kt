package com.infowings.catalog.data

import com.infowings.catalog.auth.user.UserService
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.data.guid.GuidDaoService
import com.infowings.catalog.data.history.HistoryContext
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.data.subject.SubjectDao
import com.infowings.catalog.data.subject.SubjectVertex
import com.infowings.catalog.data.subject.toSubject
import com.infowings.catalog.storage.OrientDatabase
import com.infowings.catalog.storage.transaction

interface SubjectService {
    fun getSubjects(): List<Subject>
    fun findById(id: String): SubjectVertex?
    fun findDataById(id: String): SubjectData?
    fun findDataByIdStrict(id: String): SubjectData
    fun findByIdStrict(id: String): Subject
    fun findVertexByIdStrict(id: String): SubjectVertex
    fun findByName(name: String): SubjectData?
    fun createSubject(sd: SubjectData, username: String): Subject
    fun updateSubject(subjectData: SubjectData, username: String): Subject
    fun remove(subjectData: SubjectData, username: String, force: Boolean = false)
}

class NormalizedSubjectService(private val innerService: SubjectService) : SubjectService by innerService {
    override fun createSubject(sd: SubjectData, username: String): Subject = innerService.createSubject(sd.normalize(), username)
    override fun updateSubject(subjectData: SubjectData, username: String): Subject = innerService.updateSubject(subjectData.normalize(), username)
    override fun remove(subjectData: SubjectData, username: String, force: Boolean) = innerService.remove(subjectData.normalize(), username, force)
}

class DefaultSubjectService(
    private val db: OrientDatabase,
    private val dao: SubjectDao,
    private val guidDao: GuidDaoService,
    private val history: HistoryService,
    private val userService: UserService
) : SubjectService {

    override fun getSubjects(): List<Subject> = dao.getSubjects()

    override fun findById(id: String): SubjectVertex? = dao.findById(id)

    override fun findDataById(id: String): SubjectData = transaction (db) {
        dao.findById(id)?.toSubject()?.toSubjectData() ?: throw SubjectNotFoundException("No subject with id $id")
    }

    override fun findDataByIdStrict(id: String): SubjectData = transaction (db) {
        dao.findByIdStrict(id).toSubject().toSubjectData()
    }

    override fun findByIdStrict(id: String): Subject = transaction (db) {
        dao.findByIdStrict(id).toSubject()
    }

    override fun findVertexByIdStrict(id: String): SubjectVertex = dao.findByIdStrict(id)

    override fun findByName(name: String): SubjectData? = dao.findByName(name)?.toSubject()?.toSubjectData()

    override fun createSubject(sd: SubjectData, username: String): Subject {
        val userVertex = userService.findUserVertexByUsername(username)

        val vertex = transaction(db) {
            val vertex = dao.createSubject(sd)
            guidDao.newGuidVertex(vertex)
            history.storeFact(vertex.toCreateFact(HistoryContext(userVertex)))
            return@transaction vertex
        }

        return transaction(db) { vertex.toSubject() }
    }

    override fun updateSubject(subjectData: SubjectData, username: String): Subject {
        val id = subjectData.id ?: throw SubjectIdIsNull

        val userVertex = userService.findUserVertexByUsername(username)

        val resultVertex = transaction(db) {
            val vertex = dao.findByIdStrict(id)
            val existing: SubjectData = vertex.toSubject().toSubjectData()
            if (subjectData == existing) {
                throw SubjectEmptyChangeException()
            }

            // временно отключим до гарантированной поддержки на фронте
            //if (normalizedSubjectData.isModified(vertex.version)) {
            //    throw SubjectConcurrentModificationException(expected =  normalizedSubjectData.version, real = vertex.version)
            //}

            return@transaction history.trackUpdate(vertex, HistoryContext(userVertex)) {
                dao.updateSubjectVertex(vertex, subjectData)
            }
        }

        return transaction(db) { resultVertex.toSubject() }
    }

    override fun remove(subjectData: SubjectData, username: String, force: Boolean) {
        val id = subjectData.id ?: throw SubjectIdIsNull

        val userVertex = userService.findUserVertexByUsername(username)

        transaction(db) {
            val vertex = dao.findByIdStrict(id)

            // временно отключим до гарантированной поддержки на фронте
            //if (normalizedSubjectData.isModified(vertex.version)) {
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

private fun SubjectData.normalize(): SubjectData = copy(name = this.name.trim(), description = this.description?.trim())

sealed class SubjectException(override val message: String? = null) : Exception(message)

object SubjectIdIsNull : SubjectException()
class SubjectWithNameAlreadyExist(val subject: Subject) : SubjectException("Subject already exist: ${subject.name}")
class SubjectNotFoundException(val id: String) : SubjectException("Subject with id $id not found")
class SubjectConcurrentModificationException(expected: Int, real: Int) :
    SubjectException("Found version $real instead of $expected")

class SubjectIsLinkedByAspect(val subject: SubjectData, val aspect: AspectData) :
    SubjectException("Subject ${subject.id} is linked by ${aspect.id}")

class SubjectEmptyChangeException : SubjectException("such subject already exists")
