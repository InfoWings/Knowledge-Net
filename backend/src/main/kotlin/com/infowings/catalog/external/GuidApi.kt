package com.infowings.catalog.external

import com.infowings.catalog.common.objekt.EntityMetadata
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
        logger.debug("Get aspect by guid: $guid")
        return guidService.metadata(listOf(guid)).first()
    }
}

private val logger = loggerFor<GuidApi>()