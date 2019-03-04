package com.infowings.catalog.external

import com.infowings.catalog.common.BadRequestCode
import com.infowings.catalog.common.guid.BriefObjectViewResponse
import com.infowings.catalog.common.guid.BriefValueViewResponse
import com.infowings.catalog.common.guid.EntityMetadata
import com.infowings.catalog.data.guid.GuidService
import com.infowings.catalog.loggerFor
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

class GuidException(message: String) : Exception(message)

@RestController
@RequestMapping("/api/guid")
class GuidApi(val guidService: GuidService) {

    @GetMapping("/meta/{guid}")
    fun getMetaByGuid(@PathVariable guid: String): EntityMetadata {
        logger.debug("Get entity metadata by guid: $guid")
        val found = guidService.metadata(listOf(guid))
        if (found.isEmpty()) {
            throw GuidException("no such guid: $guid")
        }
        return guidService.metadata(listOf(guid)).single()
    }

    @GetMapping("/brief/object/{guid}")
    fun getBriefObjectByGuid(@PathVariable guid: String): BriefObjectViewResponse {
        logger.debug("Get brief object view by guid: $guid")
        return guidService.findObject(guid)
    }

    @GetMapping("/brief/value/{guid}")
    fun getBriefValueByGuid(@PathVariable guid: String): BriefValueViewResponse {
        logger.debug("Get brief value view by guid: $guid")
        return guidService.findObjectValue(guid)
    }

    @GetMapping("/brief/object/id/{id}")
    fun getBriefObjectById(@PathVariable id: String): BriefObjectViewResponse {
        logger.debug("Get brief object view by id: $id")
        return guidService.findObjectById(id)
    }

    @GetMapping("/brief/value/id/{id}")
    fun getBriefValueById(@PathVariable id: String): BriefValueViewResponse {
        logger.debug("Get brief value view by id: $id")
        return guidService.findObjectValueById(id)
    }

    @ExceptionHandler(Exception::class)
    fun handleGuidException(exception: GuidException): ResponseEntity<String> {
        logger.error(exception.toString(), exception)
        return badRequest("Problem with guid. ${exception.message}", BadRequestCode.INCORRECT_INPUT)
    }
}

private val logger = loggerFor<GuidApi>()
