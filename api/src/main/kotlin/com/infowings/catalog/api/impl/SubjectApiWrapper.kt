package com.infowings.catalog.api.impl

import com.infowings.catalog.api.KNServerContext
import com.infowings.catalog.api.SubjectApi
import com.infowings.catalog.api.loggerFor
import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.common.SubjectsList

internal class SubjectApiWrapper(private val context: KNServerContext) : SubjectApi {
    override fun createSubject(subjectData: SubjectData): SubjectData {
        logger.debug("Create subject request: $subjectData")
        return context.builder("/api/subject/create").body(subjectData).post()
    }

    override fun getSubject(): SubjectsList {
        logger.debug("Get subjects request")
        return context.builder("/api/subject/all").get()
    }

    override fun getSubjectByName(subjectName: String): SubjectData? {
        logger.debug("Get subject by name request: $subjectName")
        return context.builder("/api/subject/get").path(subjectName).getOrNull()
    }

    override fun getSubjectById(id: String): SubjectData? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun updateSubject(subjectData: SubjectData): SubjectData {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeSubject(subjectData: SubjectData) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun forceRemoveSubject(subjectData: SubjectData) {
        context.builder("/api/subject/forceRemove").body(subjectData).postAndIgnore()
    }

}

private val logger = loggerFor<SubjectApi>()
