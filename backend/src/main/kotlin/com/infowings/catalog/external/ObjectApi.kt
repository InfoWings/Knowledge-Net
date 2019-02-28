package com.infowings.catalog.external

import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.*
import com.infowings.catalog.data.objekt.*
import com.infowings.catalog.loggerFor
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/api/objects")
class ObjectApi(val objectService: ObjectService) {
    private val logger = loggerFor<ObjectApi>()

    @PostMapping
    fun getAllObjects(@RequestBody(required = true) objectsRequestData: ObjectsRequestData, principal: Principal): ObjectsResponse {
        val username = principal.name
        logger.debug("Get objects request by $username, $objectsRequestData")

        return logTime(logger, "request of all object briefs") {
            val objects = objectService.fetch(objectsRequestData)
            val list = objects.drop(objectsRequestData.pagination.offset).take(objectsRequestData.pagination.limit).map { it.toResponse() }
            ObjectsResponse(list, objects.size)
        }
    }

    @GetMapping("recalculateValue")
    fun recalculateValue(
        @RequestParam("from", required = true) fromMeasure: String,
        @RequestParam("to", required = true) toMeasure: String,
        @RequestParam("value", required = true) value: String,
        principal: Principal
    ): ValueRecalculationResponse {
        val username = principal.name
        logger.debug("Recalculate value $value $fromMeasure -> $toMeasure request by $username")
        return ValueRecalculationResponse(
            targetMeasure = toMeasure,
            value = objectService.recalculateValue(fromMeasure, toMeasure, DecimalNumber(value)).toPlainString()
        )
    }

    @GetMapping("/viewdetails")
    fun getDetailedObjects(@RequestAttribute("ids", required = true) ids: String, principal: Principal): DetailedObjectViewResponse {
        val username = principal.name
        logger.debug("Get objects request by $username")
        val res = logTime(logger, "request of all object details") {
            //objectService.getDetailedObject(id)
        }
        logger.debug("viewdetails for ids $ids result: $res")
        return DetailedObjectViewResponse("", "", "", "", "", 0, emptyList(), lastUpdated = null) //res
    }

    @GetMapping("{id}/viewdetails")
    fun getDetailedObject(@PathVariable("id", required = true) id: String, principal: Principal): DetailedObjectViewResponse {
        val username = principal.name
        logger.debug("Get objects request by $username")
        val res = objectService.getDetailedObject(id)
        logger.debug("viewdetails for id $id result: $res")
        return res
    }

    @GetMapping("{id}/editdetails")
    fun getDetailedObjectForEdit(@PathVariable("id", required = true) id: String, principal: Principal): ObjectEditDetailsResponse {
        val username = principal.name
        logger.debug("Get object for edit request by $username")
        return objectService.getDetailedObjectForEdit(id)
    }

    @PostMapping("create")
    fun createObject(@RequestBody request: ObjectCreateRequest, principal: Principal): ObjectChangeResponse {
        val username = principal.name
        logger.debug("New object create request: $request by $username")
        return objectService.create(request, username)
    }

    @PostMapping("update")
    fun updateObject(@RequestBody request: ObjectUpdateRequest, principal: Principal): ObjectChangeResponse {
        val username = principal.name
        logger.debug("Object ${request.id} update request: $request by $username")
        return objectService.update(request, username)
    }

    @PostMapping("createProperty")
    fun createObjectProperty(@RequestBody request: PropertyCreateRequest, principal: Principal): PropertyCreateResponse {
        val username = principal.name
        logger.debug("Object property update request: $request by $username")
        return objectService.create(request, username)
    }

    @PostMapping("updateProperty")
    fun updateObjectProperty(@RequestBody request: PropertyUpdateRequest, principal: Principal): PropertyUpdateResponse {
        val username = principal.name
        logger.debug("Object property update request: $request by $username")
        return objectService.update(request, username)
    }

    @PostMapping("createValue")
    fun createObjectValue(@RequestBody requestDTO: ValueCreateRequestDTO, principal: Principal): ValueChangeResponse {
        val username = principal.name
        val request = requestDTO.toRequest()
        logger.debug("New object property value create request: $requestDTO by $username")
        return objectService.create(request, username)
    }

    @PostMapping("updateValue")
    fun updateObjectValue(@RequestBody requestDTO: ValueUpdateRequestDTO, principal: Principal): ValueChangeResponse {
        val username = principal.name
        val request = requestDTO.toRequest()
        logger.debug("Object property value update request: $requestDTO by $username")
        return objectService.update(request, username)
    }

    @DeleteMapping("value/{id}")
    fun deleteValue(@PathVariable id: String, @RequestParam("force") force: Boolean, principal: Principal): ValueDeleteResponse {
        val username = principal.name
        logger.debug("Object value delete request: $id by $username")
        return if (force) {
            objectService.softDeleteValue(id, username)
        } else {
            try {
                objectService.deleteValue(id, username)
            } catch (e: Throwable) {
                logger.info("thrown $e")
                throw e
            }
        }
    }

    @DeleteMapping("property/{id}")
    fun deleteProperty(@PathVariable id: String, @RequestParam("force") force: Boolean, principal: Principal): PropertyDeleteResponse {
        val username = principal.name
        logger.debug("Object property delete request: $id by $username")
        return if (force) {
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

    @ExceptionHandler(ObjectException::class)
    fun handleObjectException(exception: ObjectException): ResponseEntity<String> {
        logger.info("object exception handler $exception...")
        return when (exception) {
            is ObjectIsLinkedException -> ResponseEntity.badRequest().body(exception.message)
            is ObjectAlreadyExists -> ResponseEntity.badRequest().body(exception.message)
            is EmptyObjectCreateNameException -> ResponseEntity.badRequest().body(exception.message)
            is EmptyObjectUpdateNameException -> ResponseEntity.badRequest().body(exception.message)
            is ObjectPropertyAlreadyExistException -> ResponseEntity.badRequest().body(exception.message)
            is ObjectConcurrentEditException -> ResponseEntity.badRequest().body(exception.message)
            is ObjectPropertyConcurrentEditException -> ResponseEntity.badRequest().body(exception.message)
            is ObjectPropertyValueConcurrentModificationException -> ResponseEntity.badRequest().body(exception.message)
            else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("${exception.message}")
        }
    }

    @ExceptionHandler(ObjectPropertyException::class)
    fun handleObjectPropertyException(exception: ObjectPropertyException): ResponseEntity<String> {
        logger.info("property exception handler $exception...")
        return when (exception) {
            is ObjectPropertyIsLinkedException -> ResponseEntity.badRequest().body(exception.message)
        }
    }

    @ExceptionHandler(ObjectValueException::class)
    fun handleObjectValueException(exception: ObjectValueException): ResponseEntity<String> {
        logger.debug("value exception handler $exception...")
        return when (exception) {
            is ObjectValueIsLinkedException -> ResponseEntity.badRequest().body(exception.message)
            else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("${exception.message}")
        }
    }
}