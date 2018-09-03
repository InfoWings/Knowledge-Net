package com.infowings.catalog.external

import com.infowings.catalog.common.*
import com.infowings.catalog.data.aspect.*
import com.infowings.catalog.loggerFor
import kotlinx.serialization.json.JSON
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal


//todo: перехватывание exception и генерация внятных сообщений об ошибках наружу
@RestController
@RequestMapping("/api/aspect")
class AspectApi(val aspectService: AspectService) {

    //todo: json in request body
    @PostMapping("create")
    fun createAspect(@RequestBody aspectData: AspectData, principal: Principal): AspectData {
        val username = principal.name
        logger.debug("New aspect create request: $aspectData by $username")
        return aspectService.save(aspectData, username)
    }

    @PostMapping("update")
    fun updateAspect(@RequestBody aspectData: AspectData, principal: Principal): AspectData {
        val username = principal.name
        logger.debug("Update aspect request: $aspectData by $username")
        return aspectService.save(aspectData, username)
    }

    @GetMapping("/id/{id}")
    fun getAspectById(@PathVariable id: String): AspectData {
        logger.debug("Get aspect by id: $id")
        return aspectService.findById(id)
    }

    @GetMapping("/tree/{id}")
    fun getAspectTreeById(@PathVariable id: String): AspectTree {
        logger.debug("Get aspect tree by id: $id")
        return aspectService.findTreeById(id)
    }

    @GetMapping("all")
    fun getAspects(
        @RequestParam(required = false) orderFields: List<String>,
        @RequestParam(required = false) direct: List<String>,
        @RequestParam("q", required = false) query: String?
    ): AspectsList {
        logger.debug("Get all aspects request, orderFields: ${orderFields.joinToString { it }}, direct: ${direct.joinToString { it }}, query: $query")
        val beforeMS = System.currentTimeMillis()
        val orderBy = direct.zip(orderFields).map { (direction, order) ->
            AspectOrderBy(AspectSortField.valueOf(order), Direction.valueOf(direction))
        }
        val result = AspectsList(aspectService.getAspects(orderBy, query))
        val afterMS = System.currentTimeMillis()
        logger.info("all aspects took ${afterMS - beforeMS}")        
        return result
    }

    @PostMapping("remove")
    fun removeAspect(@RequestBody aspect: AspectData, principal: Principal) {
        val username = principal.name
        logger.debug("Remove aspect request: ${aspect.id} by $username")
        aspectService.remove(aspect, username)
    }

    @PostMapping("forceRemove")
    fun forceRemoveAspect(@RequestBody aspect: AspectData, principal: Principal) {
        val username = principal.name
        logger.debug("Forced remove aspect request: ${aspect.id} by $username")
        aspectService.remove(aspect, username, true)
    }

    @DeleteMapping("property/{id}")
    fun removeAspectProperty(@PathVariable id: String, @RequestParam force: Boolean, principal: Principal): AspectPropertyDeleteResponse {
        val username = principal.name
        logger.debug("Remove aspect property request: $id by $username")
        return aspectService.removeProperty(id, username, force)
    }

    @ExceptionHandler(AspectException::class)
    fun handleAspectException(exception: AspectException): ResponseEntity<String> {
        logger.error(exception.toString(), exception)
        return when (exception) {
            is AspectAlreadyExist -> ResponseEntity.badRequest()
                .body(
                    JSON.stringify(
                        BadRequest(
                            BadRequestCode.INCORRECT_INPUT,
                            "Aspect with such name already exists (${exception.name})."
                        )
                    )
                )
            is AspectDoesNotExist -> ResponseEntity.badRequest()
                .body(
                    JSON.stringify(
                        BadRequest(
                            BadRequestCode.INCORRECT_INPUT,
                            "Supplied aspect does not exist or it is deleted"
                        )
                    )
                )
            is AspectConcurrentModificationException -> ResponseEntity.badRequest()
                .body(
                    JSON.stringify(
                        BadRequest(
                            BadRequestCode.INCORRECT_INPUT,
                            "Attempt to modify old version of aspect, please refresh."
                        )
                    )
                )
            is AspectModificationException -> ResponseEntity.badRequest()
                .body(
                    JSON.stringify(
                        BadRequest(
                            BadRequestCode.INCORRECT_INPUT,
                            "Updates to aspect ${exception.id} violates update constraints: ${exception.message}"
                        )
                    )
                )
            is AspectPropertyModificationException -> ResponseEntity.badRequest()
                .body(
                    JSON.stringify(
                        BadRequest(
                            BadRequestCode.INCORRECT_INPUT,
                            "Updates to aspect property ${exception.id} violates update constraints: ${exception.message}"
                        )
                    )
                )
            is AspectCyclicDependencyException -> ResponseEntity.badRequest()
                .body(
                    JSON.stringify(
                        BadRequest(
                            BadRequestCode.INCORRECT_INPUT,
                            "Failed to create/modify aspect due to emerging cycle among aspects"
                        )
                    )
                )
            is AspectInconsistentStateException -> ResponseEntity.badRequest()
                .body(
                    JSON.stringify(
                        BadRequest(
                            BadRequestCode.INCORRECT_INPUT,
                            exception.message
                        )
                    )
                )
            is AspectPropertyConcurrentModificationException -> ResponseEntity.badRequest()
                .body(
                    JSON.stringify(
                        BadRequest(
                            BadRequestCode.INCORRECT_INPUT,
                            "Attempt to modify old version of aspect property (${exception.id}), please refresh."
                        )
                    )
                )
            is AspectHasLinkedEntitiesException -> ResponseEntity.badRequest()
                .body(
                    JSON.stringify(
                        BadRequest(
                            BadRequestCode.NEED_CONFIRMATION,
                            "Attempt to remove aspect that has linked entities pointed to it"
                        )
                    )
                )
            is AspectPropertyIsLinkedByValue -> ResponseEntity.badRequest()
                .body(
                    JSON.stringify(
                        BadRequest(
                            BadRequestCode.NEED_CONFIRMATION,
                            "Attempt to remove aspect property that is linked by other entities"
                        )
                    )
                )
            is AspectNameCannotBeNull -> ResponseEntity.badRequest()
                .body(
                    JSON.stringify(
                        BadRequest(
                            BadRequestCode.INCORRECT_INPUT,
                            "Aspect name cannot be empty string"
                        )
                    )
                )
            is AspectEmptyChangeException -> ResponseEntity(HttpStatus.NOT_MODIFIED)
            else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("${exception.message}")
        }
    }
}

private val logger = loggerFor<AspectApi>()