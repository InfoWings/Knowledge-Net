package com.infowings.catalog.external

import com.infowings.catalog.common.guid.BriefObjectViewResponse
import com.infowings.catalog.common.guid.BriefValueViewResponse
import com.infowings.catalog.common.guid.EntityMetadata
import com.infowings.catalog.data.guid.GuidService
import com.infowings.catalog.loggerFor
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/guid")
class GuidApi(val guidService: GuidService) {

    @GetMapping("/meta/{id}")
    fun getMetaById(@PathVariable guid: String): EntityMetadata {
        logger.debug("Get entity metadata by guid: $guid")
        return guidService.metadata(listOf(guid)).single()
    }

    @GetMapping("/brief/object/{id}")
    fun getBriefObjectByGuid(@PathVariable guid: String): BriefObjectViewResponse {
        logger.debug("Get brief object view by guid: $guid")
        return guidService.findObject(guid)
    }

    @GetMapping("/brief/value/{id}")
    fun getBriefValueByGuid(@PathVariable guid: String): BriefValueViewResponse {
        logger.debug("Get brief value view by guid: $guid")
        return guidService.findObjectValue(guid)
    }
}

private val logger = loggerFor<GuidApi>()