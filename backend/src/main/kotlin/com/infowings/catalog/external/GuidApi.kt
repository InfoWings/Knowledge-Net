package com.infowings.catalog.external

import com.infowings.catalog.common.objekt.EntityMetadata
import com.infowings.catalog.data.guid.GuidService
import com.infowings.catalog.loggerFor
import org.springframework.web.bind.annotation.*
import java.security.Principal


@RestController
@RequestMapping("/api/guid")
class GuidApi(val guidService: GuidService) {

    @GetMapping("/meta/{id}")
    fun getMetaById(@PathVariable guid: String): EntityMetadata {
        logger.debug("Get aspect by guid: $guid")
        return guidService.metadata(listOf(guid)).first()
    }

    @PostMapping("/set/{id}")
    fun setGuid(@PathVariable id: String, principal: Principal): EntityMetadata {
        logger.debug("Set guid for $id")
        return guidService.setGuid(id, principal.name)
    }
}

private val logger = loggerFor<GuidApi>()