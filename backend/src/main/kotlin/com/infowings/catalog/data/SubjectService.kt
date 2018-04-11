package com.infowings.catalog.data

import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.data.history.HistoryService
import com.infowings.catalog.data.subject.SubjectDao
import com.infowings.catalog.storage.*
import com.orientechnologies.orient.core.record.OVertex


class SubjectService(private val dao: SubjectDao, private val history: HistoryService) {
    fun getSubjects(): List<Subject> = dao.getSubjects()

    fun createSubject(sd: SubjectData): Subject = dao.createSubject(sd)

    fun updateSubject(sd: SubjectData): Subject = dao.updateSubject(sd)
}

class SubjectIdIsNull : Throwable()
class SubjectWithNameAlreadyExist(val subject: Subject) : Throwable("Subject already exist: ${subject.name}")
