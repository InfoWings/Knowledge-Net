package com.infowings.catalog.external

import com.infowings.catalog.common.DetailedObjectResponse
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

    @GetMapping("{id}")
    fun getDetailedObject(@PathVariable("id", required = true) id: String, principal: Principal): DetailedObjectResponse {
        val username = principal.name
        logger.debug("Get objects request by $username")
        return objectService.getDetailedObject(id)
    }

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
    fun updateObjectValue(@RequestBody requestDTO: ValueUpdateRequestDTO, principal: Principal): ValueUpdateResponse {
        val username = principal.name
        val request = requestDTO.toRequest()
        logger.debug("Object property value update request: $requestDTO by $username")
        val result: ObjectPropertyValue = objectService.update(request, username)
        return ValueUpdateResponse(result.id.toString())
    }

    @DeleteMapping("value/{id}")
    fun deleteValue(@PathVariable id: String, @RequestParam("force") force: Boolean, principal: Principal) {
        val username = principal.name
        logger.debug("Object value delete request: $id by $username")
        if (force) {
            objectService.softDeleteValue(id, username)
        } else {
            objectService.deleteValue(id, username)
        }
    }

    @DeleteMapping("property/{id}")
    fun deleteProperty(@PathVariable id: String, @RequestParam("force") force: Boolean, principal: Principal) {
        val username = principal.name
        logger.debug("Object property delete request: $id by $username")
        if (force) {
            objectService.softDeleteProperty(id, username)
        } else {
            objectService.deleteProperty(id, username)
        }
    }

    @DeleteMapping("object/{id}")
    fun deleteObject(@PathVariable id: String, @RequestParam("force") force: Boolean, principal: Principal) {
        val username = principal.name
        logger.debug("Object delete request: $id by $username")
        if (force) {
            objectService.softDeleteObject(id, username)
        } else {
            objectService.deleteObject(id, username)
        }
    }
}