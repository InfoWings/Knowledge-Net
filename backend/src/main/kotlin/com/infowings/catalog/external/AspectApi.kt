package com.infowings.catalog.external

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectsList
import com.infowings.catalog.data.aspect.*
import com.infowings.catalog.loggerFor
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

    @ExceptionHandler(AspectException::class)
    fun handleAspectException(exception: AspectException): ResponseEntity<String> = when (exception) {
        is AspectAlreadyExist -> ResponseEntity.badRequest()
                .body("Aspect with such name already exists (${exception.name}).")
        is AspectConcurrentModificationException -> ResponseEntity.badRequest()
                .body("Attempt to modify old version of entity, please refresh.")
        is AspectModificationException -> if (exception.message == "aspect is removed") {
            ResponseEntity.badRequest().body("Attempt to modify already deleted entity, please refresh.")
        } else {
            ResponseEntity.badRequest().body("Updates to aspect ${exception.id} violates update constraints: ${exception.message}")
        }
        is AspectPropertyModificationException -> ResponseEntity.badRequest()
                .body("Updates to aspect property ${exception.id} violates update constraints: ${exception.message}")
        is AspectCyclicDependencyException -> ResponseEntity.badRequest()
                .body("Failed to create/modify aspect due to emerging cycle among aspects")
        else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("${exception.message}")
    }
}
private val logger = loggerFor<AspectApi>()