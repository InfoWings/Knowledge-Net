package com.infowings.catalog.external

import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.toSubjectData
import com.infowings.catalog.loggerFor
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/subject")
class SubjectApi(val subjectService: SubjectService) {

    @PostMapping("create")
    fun createAspect(@RequestBody subjectData: SubjectData): SubjectData {
        logger.info("New subject create request: $subjectData")
        return subjectService.createSubject(subjectData).toSubjectData()
    }

    @GetMapping("all")
    fun getAspects(): List<SubjectData> {
        logger.debug("Get all subject request")
        return subjectService.getSubjects().map { it.toSubjectData() }
    }
}

private val logger = loggerFor<SubjectApi>()