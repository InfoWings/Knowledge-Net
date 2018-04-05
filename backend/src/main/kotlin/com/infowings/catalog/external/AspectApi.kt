package com.infowings.catalog.external

import com.infowings.catalog.common.AspectBadRequest
import com.infowings.catalog.common.AspectBadRequestCode
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectsList
import com.infowings.catalog.data.aspect.*
import com.infowings.catalog.loggerFor
import kotlinx.serialization.json.JSON
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

//todo: перехватывание exception и генерация внятных сообщений об ошибках наружу
@RestController
@RequestMapping("/api/aspect")
class AspectApi(val aspectService: AspectService) {

    //todo: json in request body
    @PostMapping("create")
    fun createAspect(@RequestBody aspectData: AspectData): AspectData {
        logger.info("New aspect create request: $aspectData")
        return aspectService.save(aspectData).toAspectData()
    }

    @PostMapping("update")
    fun updateAspect(@RequestBody aspectData: AspectData): AspectData {
        logger.info("Update aspect request: $aspectData")
        return aspectService.save(aspectData).toAspectData()
    }

    @GetMapping("get/{name}")
    fun getAspect(@PathVariable name: String): List<AspectData> {
        logger.debug("Get aspect request: $name")
        return aspectService.findByName(name).map { it.toAspectData() }
    }

    @GetMapping("all")
    fun getAspects(): AspectsList {
        logger.debug("Get all aspects request")
        return AspectsList(aspectService.getAspects().toAspectData())
    }

    @PostMapping("remove")
    fun removeAspect(@RequestBody aspect: AspectData) {
        logger.debug("Remove aspect request: ${aspect.id}")
        aspectService.remove(aspect)
    }

    @PostMapping("forceRemove")
    fun forceRemoveAspect(@RequestBody aspect: AspectData) {
        logger.debug("Forced remove aspect request: ${aspect.id}")
        aspectService.remove(aspect, true)
    }

    @ExceptionHandler(AspectException::class)
    fun handleAspectException(exception: AspectException): ResponseEntity<String> {
        logger.error(exception.toString(), exception)
        return when (exception) {
            is AspectAlreadyExist -> ResponseEntity.badRequest()
                .body(
                    JSON.Companion.stringify(
                        AspectBadRequest(
                            AspectBadRequestCode.INCORRECT_INPUT,
                            "Aspect with such name already exists (${exception.name})."
                        )
                    )
                )
            is AspectConcurrentModificationException -> ResponseEntity.badRequest()
                .body(
                    JSON.Companion.stringify(
                        AspectBadRequest(
                            AspectBadRequestCode.INCORRECT_INPUT,
                            "Attempt to modify old version of aspect, please refresh."
                        )
                    )
                )
            is AspectModificationException -> ResponseEntity.badRequest()
                .body(
                    JSON.Companion.stringify(
                        AspectBadRequest(
                            AspectBadRequestCode.INCORRECT_INPUT,
                            "Updates to aspect ${exception.id} violates update constraints: ${exception.message}"
                        )
                    )
                )
            is AspectPropertyModificationException -> ResponseEntity.badRequest()
                .body(
                    JSON.Companion.stringify(
                        AspectBadRequest(
                            AspectBadRequestCode.INCORRECT_INPUT,
                            "Updates to aspect property ${exception.id} violates update constraints: ${exception.message}"
                        )
                    )
                )
            is AspectCyclicDependencyException -> ResponseEntity.badRequest()
                .body(
                    JSON.Companion.stringify(
                        AspectBadRequest(
                            AspectBadRequestCode.INCORRECT_INPUT,
                            "Failed to create/modify aspect due to emerging cycle among aspects"
                        )
                    )
                )
            is AspectInconsistentStateException -> ResponseEntity.badRequest()
                .body(
                    JSON.Companion.stringify(
                        AspectBadRequest(
                            AspectBadRequestCode.INCORRECT_INPUT,
                            exception.message
                        )
                    )
                )
            is AspectPropertyConcurrentModificationException -> ResponseEntity.badRequest()
                .body(
                    JSON.stringify(
                        AspectBadRequest(
                            AspectBadRequestCode.INCORRECT_INPUT,
                            "Attempt to modify old version of aspect property (${exception.id}), please refresh."
                        )
                    )
                )
            is AspectHasLinkedEntitiesException -> ResponseEntity.badRequest()
                .body(
                    JSON.stringify(
                        AspectBadRequest(
                            AspectBadRequestCode.NEED_CONFIRMATION,
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