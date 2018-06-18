package com.infowings.catalog.external

import com.infowings.catalog.common.ObjectsResponse
import com.infowings.catalog.common.objekt.*
import com.infowings.catalog.data.objekt.ObjectPropertyValue
import com.infowings.catalog.data.objekt.ObjectService
import com.infowings.catalog.loggerFor
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/api/objects")
class ObjectApi(val objectService: ObjectService) {
    private val logger = loggerFor<ObjectApi>()

    @GetMapping
    fun getAllObjects(principal: Principal): ObjectsResponse {
        val username = principal.name
        logger.debug("Get objects request by $username")
        return ObjectsResponse(objectService.fetch().map { it.toResponse() })
    }


    @PostMapping("create")
    fun createObject(@RequestBody request: ObjectCreateRequest, principal: Principal): ObjectCreateResponse {
        val username = principal.name
        logger.debug("New object create request: $request by $username")
        val result = objectService.create(request, username)
        return ObjectCreateResponse(result)
    }

    @PostMapping("createProperty")
    fun createObjectProperty(@RequestBody request: PropertyCreateRequest, principal: Principal): PropertyCreateResponse {
        val username = principal.name
        logger.debug("New object property create request: $request by $username")
        return PropertyCreateResponse(objectService.create(request, username))
    }

    @PostMapping("createValue")
    fun createObjectValue(@RequestBody requestDTO: ValueCreateRequestDTO, principal: Principal): ValueCreateResponse {
        val username = principal.name
        val request = requestDTO.toRequest()
        logger.debug("New object property value create request: $requestDTO by $username")
        val result: ObjectPropertyValue = objectService.create(request, username)
        return ValueCreateResponse(result.id.toString())
    }
}