package com.infowings.catalog.external

import com.infowings.catalog.common.objekt.*
import com.infowings.catalog.data.objekt.ObjectPropertyValue
import com.infowings.catalog.data.objekt.ObjectService
import com.infowings.catalog.loggerFor
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController
@RequestMapping("/api/object")
class ObjectApi(val objectService: ObjectService) {
    private val logger = loggerFor<ObjectApi>()

    @PostMapping("create")
    fun createObject(@RequestBody request: ObjectCreateRequest, principal: Principal): ObjectCreateResponse {
        val username = principal.name
        logger.debug("New object create request: $request by $username")
        val result = objectService.create(request, username)
        return ObjectCreateResponse(result)
    }

    @PostMapping("update")
    fun updateObject(@RequestBody request: ObjectUpdateRequest, principal: Principal): ObjectUpdateResponse {
        val username = principal.name
        logger.debug("Object ${request.id} update request: $request by $username")
        val result = objectService.update(request, username)
        return ObjectUpdateResponse(result)
    }

    @PostMapping("createProperty")
    fun createObjectProperty(@RequestBody request: PropertyCreateRequest, principal: Principal): PropertyCreateResponse {
        val username = principal.name
        logger.debug("Object property update request: $request by $username")
        return PropertyCreateResponse(objectService.create(request, username))
    }

    @PostMapping("updateProperty")
    fun createObjectProperty(@RequestBody request: PropertyUpdateRequest, principal: Principal): PropertyUpdateResponse {
        val username = principal.name
        logger.debug("Object property update request: $request by $username")
        return PropertyUpdateResponse(objectService.update(request, username))
    }

    @PostMapping("createValue")
    fun createObjectValue(@RequestBody requestDTO: ValueCreateRequestDTO, principal: Principal): ValueCreateResponse {
        val username = principal.name
        val request = requestDTO.toRequest()
        logger.debug("New object property value create request: $requestDTO by $username")
        val result: ObjectPropertyValue = objectService.create(request, username)
        return ValueCreateResponse(result.id.toString())
    }

    @PostMapping("updateValue")
    fun createObjectValue(@RequestBody requestDTO: ValueUpdateRequestDTO, principal: Principal): ValueUpdateResponse {
        val username = principal.name
        val request = requestDTO.toRequest()
        logger.debug("Object property value update request: $requestDTO by $username")
        val result: ObjectPropertyValue = objectService.update(request, username)
        return ValueUpdateResponse(result.id.toString())
    }
}