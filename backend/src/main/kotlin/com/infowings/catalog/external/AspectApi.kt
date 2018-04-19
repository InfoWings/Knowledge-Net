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
        return aspectService.save(aspectData, username).toAspectData()
    }

    @PostMapping("update")
    fun updateAspect(@RequestBody aspectData: AspectData, principal: Principal): AspectData {
        val username = principal.name
        logger.debug("Update aspect request: $aspectData by $username")
        return aspectService.save(aspectData, username).toAspectData()
    }

    @GetMapping("get/{name}")
    fun getAspect(@PathVariable name: String): List<AspectData> {
        logger.debug("Get aspect request: $name")
        return aspectService.findByName(name).map { it.toAspectData() }
    }

    @GetMapping("all")
    fun getAspects(@RequestParam(required = false) orderFields: List<String>, @RequestParam(required = false) direct: List<String>): AspectsList {
        logger.debug("Get all aspects request, orderFields: ${orderFields.joinToString { it }}  direct: ${direct.joinToString { it }}")
        val directIterator = direct.map { Direction.valueOf(it) }.iterator()
        val orderBy = mutableListOf<AspectOrderBy>()
        orderFields.map { AspectSortField.valueOf(it) }.forEach {
            orderBy += AspectOrderBy(it, directIterator.next())
        }
        return AspectsList(aspectService.getAspects(orderBy).toAspectData())
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

    @ExceptionHandler(AspectException::class)
    fun handleAspectException(exception: AspectException): ResponseEntity<String> {
        logger.error(exception.toString(), exception)
        return when (exception) {
            is AspectAlreadyExist -> ResponseEntity.badRequest()
                .body(
                    JSON.Companion.stringify(
                        BadRequest(
                            BadRequestCode.INCORRECT_INPUT,
                            "Aspect with such name already exists (${exception.name})."
                        )
                    )
                )
            is AspectConcurrentModificationException -> ResponseEntity.badRequest()
                .body(
                    JSON.Companion.stringify(
                        BadRequest(
                            BadRequestCode.INCORRECT_INPUT,
                            "Attempt to modify old version of aspect, please refresh."
                        )
                    )
                )
            is AspectModificationException -> ResponseEntity.badRequest()
                .body(
                    JSON.Companion.stringify(
                        BadRequest(
                            BadRequestCode.INCORRECT_INPUT,
                            "Updates to aspect ${exception.id} violates update constraints: ${exception.message}"
                        )
                    )
                )
            is AspectPropertyModificationException -> ResponseEntity.badRequest()
                .body(
                    JSON.Companion.stringify(
                        BadRequest(
                            BadRequestCode.INCORRECT_INPUT,
                            "Updates to aspect property ${exception.id} violates update constraints: ${exception.message}"
                        )
                    )
                )
            is AspectCyclicDependencyException -> ResponseEntity.badRequest()
                .body(
                    JSON.Companion.stringify(
                        BadRequest(
                            BadRequestCode.INCORRECT_INPUT,
                            "Failed to create/modify aspect due to emerging cycle among aspects"
                        )
                    )
                )
            is AspectInconsistentStateException -> ResponseEntity.badRequest()
                .body(
                    JSON.Companion.stringify(
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
            else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("${exception.message}")
        }
    }
}

private val logger = loggerFor<AspectApi>()