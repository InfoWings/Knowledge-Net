package com.infowings.catalog.external

import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.common.SubjectsList
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.toSubjectData
import com.infowings.catalog.loggerFor
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/subject")
class SubjectApi(val subjectService: SubjectService) {

    @PostMapping("create")
    fun createSubject(@RequestBody subjectData: SubjectData): SubjectData {
        logger.debug("New subject create request: $subjectData")
        return subjectService.createSubject(subjectData).toSubjectData()
    }

    @GetMapping("all")
    fun getSubject(): SubjectsList {
        logger.debug("Get all subject request")
        return SubjectsList(subjectService.getSubjects().map { it.toSubjectData() })
    }

    @PostMapping("update")
    fun updateSubject(@RequestBody subjectData: SubjectData): SubjectData {
        logger.debug("Update subject create request: $subjectData")
        return subjectService.updateSubject(subjectData).toSubjectData()
    }
}

private val logger = loggerFor<SubjectApi>()