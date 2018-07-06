package com.infowings.catalog.external

import com.infowings.catalog.common.BadRequest
import com.infowings.catalog.common.BadRequestCode
import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.common.SubjectsList
import com.infowings.catalog.data.*
import com.infowings.catalog.data.subject.toSubject
import com.infowings.catalog.loggerFor
import kotlinx.serialization.json.JSON
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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

    @GetMapping("get/{name}")
    fun getSubjectByName(@PathVariable("name") subjectName: String): SubjectData {
        logger.debug("Get subject by name $subjectName")
        return subjectService.findByName(subjectName) ?: throw SubjectNotFoundException("No subject with name $subjectName")
    }

    @GetMapping("{id}")
    fun getSubjectById(@PathVariable("id") id: String): SubjectData {
        logger.debug("Get subject by id $id")
        return subjectService.findById(id)?.toSubject()?.toSubjectData() ?: throw SubjectNotFoundException("No subject with id $id")
    }

    @PostMapping("update")
    fun updateSubject(@RequestBody subjectData: SubjectData, principal: Principal): SubjectData {
        val username = principal.name
        logger.debug("Update subject create request: $subjectData by $username")
        return subjectService.updateSubject(subjectData, username).toSubjectData()
    }

    @PostMapping("remove")
    fun removeSubject(@RequestBody subjectData: SubjectData, principal: Principal) {
        val username = principal.name
        logger.debug("Remove subject request: ${subjectData.id} by $username")
        subjectService.remove(subjectData, username)
    }

    @PostMapping("forceRemove")
    fun forceRemoveSubject(@RequestBody subjectData: SubjectData, principal: Principal) {
        val username = principal.name
        logger.debug("Forced remove subject request: ${subjectData.id} by $username")
        subjectService.remove(subjectData, username, true)
    }

    @ExceptionHandler(SubjectException::class)
    fun handleSubjectException(exception: SubjectException): ResponseEntity<String> {
        logger.error(exception.toString(), exception)
        return when (exception) {
            SubjectIdIsNull -> ResponseEntity.badRequest()
                .body(
                    JSON.stringify(
                        BadRequest(
                            BadRequestCode.INCORRECT_INPUT,
                            "Subject Id is null"
                        )
                    )
                )
            is SubjectWithNameAlreadyExist -> ResponseEntity.badRequest()
                .body(
                    JSON.stringify(
                        BadRequest(
                            BadRequestCode.INCORRECT_INPUT,
                            "Subject with name ${exception.subject.name} is already exist"
                        )
                    )
                )

            is SubjectNotFoundException -> ResponseEntity.badRequest()
                .body(
                    JSON.stringify(
                        BadRequest(
                            BadRequestCode.INCORRECT_INPUT,
                            "Supplied subject with id ${exception.id} has not been found"
                        )
                    )
                )
            is SubjectConcurrentModificationException -> ResponseEntity.badRequest()
                .body(
                    JSON.stringify(
                        BadRequest(
                            BadRequestCode.INCORRECT_INPUT,
                            exception.message
                        )
                    )
                )
            is SubjectIsLinkedByAspect -> {
                ResponseEntity.badRequest().body(
                    JSON.stringify(
                        BadRequest(
                            BadRequestCode.NEED_CONFIRMATION,
                            "Subject ${exception.subject.name} is linked by aspect"
                        )
                    )
                )
            }
            is SubjectEmptyChangeException -> ResponseEntity(HttpStatus.NOT_MODIFIED)
        }
    }

}

private val logger = loggerFor<SubjectApi>()