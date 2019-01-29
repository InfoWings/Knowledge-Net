package com.infowings.catalog.external

import com.infowings.catalog.common.*
import com.infowings.catalog.data.aspect.*
import com.infowings.catalog.loggerFor
import kotlinx.serialization.json.JSON
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal

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
        logger.debug(
            "Get all aspects request, orderFields: ${orderFields.joinToString { it }}, " +
                    "direct: ${direct.joinToString { it }}, query: $query"
        )

        return logTime(logger, "Get all aspects took: ") {
            val orderBy = SortOrder.listOf(orders = orderFields, directions = direct)
            val aspects = aspectService.getAspects(orderBy, query)
            return@logTime AspectsList(aspects, aspects.size)
        }
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
            is AspectAlreadyExist -> badRequest(
                "Aspect with such name already exists (${exception.name}).",
                BadRequestCode.INCORRECT_INPUT
            )
            is AspectDoesNotExist -> badRequest(
                "Supplied aspect does not exist or it is deleted",
                BadRequestCode.INCORRECT_INPUT
            )
            is AspectConcurrentModificationException -> badRequest(
                "Attempt to modify old version of aspect, please refresh.",
                BadRequestCode.INCORRECT_INPUT
            )
            is AspectModificationException -> badRequest(
                "Updates to aspect ${exception.id} violates update constraints: ${exception.message}",
                BadRequestCode.INCORRECT_INPUT

            )
            is AspectPropertyModificationException -> badRequest(
                "Updates to aspect property ${exception.id} violates update constraints: ${exception.message}",
                BadRequestCode.INCORRECT_INPUT
            )
            is AspectCyclicDependencyException -> badRequest(
                "Failed to create/modify aspect due to emerging cycle among aspects",
                BadRequestCode.INCORRECT_INPUT
            )
            is AspectInconsistentStateException -> badRequest(
                exception.message ?: "",
                BadRequestCode.INCORRECT_INPUT
            )
            is AspectPropertyConcurrentModificationException -> badRequest(
                "Attempt to modify old version of aspect property (${exception.id}), please refresh.",
                BadRequestCode.INCORRECT_INPUT
            )
            is AspectHasLinkedEntitiesException -> badRequest(
                "Attempt to remove aspect that has linked entities pointed to it",
                BadRequestCode.NEED_CONFIRMATION
            )
            is AspectPropertyIsLinkedByValue -> badRequest(
                "Attempt to remove aspect property that is linked by other entities",
                BadRequestCode.NEED_CONFIRMATION
            )
            is AspectNameCannotBeNull -> badRequest("Aspect name cannot be empty string", BadRequestCode.INCORRECT_INPUT)
            is AspectEmptyChangeException -> ResponseEntity(HttpStatus.NOT_MODIFIED)

            else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("${exception.message}")
        }
    }
}

fun badRequest(message: String, badRequestCode: BadRequestCode): ResponseEntity<String> =
    ResponseEntity.badRequest().body(JSON.stringify(BadRequest.serializer(), BadRequest(badRequestCode, message)))

private val logger = loggerFor<AspectApi>()