package com.infowings.catalog.external

import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.common.SubjectsList
import com.infowings.catalog.data.SubjectService
import com.infowings.catalog.data.toSubjectData
import com.infowings.catalog.loggerFor
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/api/subject")
class SubjectApi(val subjectService: SubjectService) {

    @PostMapping("create")
    fun createSubject(@RequestBody subjectData: SubjectData, principal: Principal): SubjectData {
        val username = principal.name
        logger.debug("New subject create request: $subjectData by $username")
        return subjectService.createSubject(subjectData, username).toSubjectData()
    }

    @GetMapping("all")
    fun getSubject(): SubjectsList {
        logger.debug("Get all subject request")
        return SubjectsList(subjectService.getSubjects().map { it.toSubjectData() })
    }

    @PostMapping("update")
    fun updateSubject(@RequestBody subjectData: SubjectData, principal: Principal): SubjectData {
        val username = principal.name
        logger.debug("Update subject create request: $subjectData by $username")
        return subjectService.updateSubject(subjectData, username).toSubjectData()
    }

    @PostMapping("remove")
    fun removeAspect(@RequestBody subjectData: SubjectData, principal: Principal) {
        val username = principal.name
        logger.debug("Remove subject request: ${subjectData.id} by $username")
        subjectService.remove(subjectData, username)
    }

    @PostMapping("forceRemove")
    fun forceRemoveAspect(@RequestBody subjectData: SubjectData, principal: Principal) {
        val username = principal.name
        logger.debug("Forced remove subject request: ${subjectData.id} by $username")
        subjectService.remove(subjectData, username, true)
    }
}

private val logger = loggerFor<SubjectApi>()