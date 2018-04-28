package com.infowings.catalog.external

import com.infowings.catalog.common.ObjectData
import com.infowings.catalog.common.ObjectPropertyData
import com.infowings.catalog.common.ObjectPropertyValueData
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
    fun createObject(@RequestBody objectData: ObjectData, principal: Principal): ObjectData {
        val username = principal.name
        logger.debug("New object create request: $objectData by $username")
        return objectService.create(objectData, username).toObjectData()
    }

    @PostMapping("createProperty")
    fun createObjectProperty(@RequestBody objectPropertyData: ObjectPropertyData, principal: Principal): ObjectPropertyData {
        val username = principal.name
        logger.debug("New object property create request: $objectPropertyData by $username")
        return objectService.create(objectPropertyData, username).toObjectPropertyData()
    }

    @PostMapping("createValue")
    fun createObjectValue(@RequestBody objectValueData: ObjectPropertyValueData, principal: Principal): ObjectPropertyValueData {
        val username = principal.name
        logger.debug("New object property value create request: $objectValueData by $username")
        return objectService.create(objectValueData, username).toObjectPropertyValueData()
    }
}