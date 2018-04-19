package com.infowings.catalog.external

import com.infowings.catalog.common.BadRequest
import com.infowings.catalog.common.BadRequestCode
import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.common.SubjectsList
import com.infowings.catalog.data.*
import com.infowings.catalog.loggerFor
import kotlinx.serialization.json.JSON
import org.springframework.http.ResponseEntity
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

    @ExceptionHandler(SubjectException::class)
    fun handleSubjectException(exception: SubjectException): ResponseEntity<String> {
        logger.error(exception.toString(), exception)
        return when (exception) {
            SubjectIdIsNull -> ResponseEntity.badRequest()
                .body(
                    JSON.Companion.stringify(
                        BadRequest(
                            BadRequestCode.INCORRECT_INPUT,
                            "Subject Id is null"
                        )
                    )
                )
            is SubjectWithNameAlreadyExist -> ResponseEntity.badRequest()
                .body(
                    JSON.Companion.stringify(
                        BadRequest(
                            BadRequestCode.INCORRECT_INPUT,
                            "Subject with name ${exception.subject.name} is already exist"
                        )
                    )
                )
            is SubjectNotFoundException -> ResponseEntity.badRequest()
                .body(
                    JSON.Companion.stringify(
                        BadRequest(
                            BadRequestCode.INCORRECT_INPUT,
                            "Supplied subject with id ${exception.id} has not been found"
                        )
                    )
                )
            is SubjectConcurrentModificationException -> ResponseEntity.badRequest()
                .body(
                    JSON.Companion.stringify(
                        BadRequest(
                            BadRequestCode.INCORRECT_INPUT,
                            exception.message
                        )
                    )
                )
            is SubjectIsLinkedByAspect -> ResponseEntity.badRequest()
                .body(
                    JSON.Companion.stringify(
                        BadRequest(
                            BadRequestCode.NEED_CONFIRMATION,
                            "Subject ${exception.subject.name} is linked by aspect"
                        )
                    )
                )
        }
    }

}

private val logger = loggerFor<SubjectApi>()