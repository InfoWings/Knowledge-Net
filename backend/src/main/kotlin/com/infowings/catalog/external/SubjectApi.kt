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
        val user = principal.name
        logger.debug("New subject create request: $subjectData by $user")
        return subjectService.createSubject(subjectData, user).toSubjectData()
    }

    @GetMapping("all")
    fun getSubject(): SubjectsList {
        logger.debug("Get all subject request")
        return SubjectsList(subjectService.getSubjects().map { it.toSubjectData() })
    }

    @PostMapping("update")
    fun updateSubject(@RequestBody subjectData: SubjectData, principal: Principal): SubjectData {
        val user = principal.name
        logger.debug("Update subject create request: $subjectData by $user")
        return subjectService.updateSubject(subjectData, user).toSubjectData()
    }

    @PostMapping("remove")
    fun removeAspect(@RequestBody subjectData: SubjectData, principal: Principal) {
        val user = principal.name
        logger.debug("Remove subject request: ${subjectData.id} by $user")
        subjectService.remove(subjectData, user)
    }

    @PostMapping("forceRemove")
    fun forceRemoveAspect(@RequestBody subjectData: SubjectData, principal: Principal) {
        val user = principal.name
        logger.debug("Forced remove subject request: ${subjectData.id} by $user")
        subjectService.remove(subjectData, user, true)
    }
}

private val logger = loggerFor<SubjectApi>()