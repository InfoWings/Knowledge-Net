package com.infowings.catalog.external

import com.infowings.catalog.EntityMetadata
import com.infowings.catalog.MetadataService
import com.infowings.catalog.loggerFor
import com.infowings.catalog.metadata.Aspect
import org.springframework.web.bind.annotation.*

//todo: перехватывание exception и генерация внятных сообщений об ошибках наружу
@RestController
@RequestMapping("/metadata")
class MetadataApi(val metadataService: MetadataService) {

    @GetMapping(path = ["entity/{id}"])
    fun getEntityMetadata(@PathVariable id: Long): EntityMetadata {
        return EntityMetadata(1, "", mutableSetOf())
    }

    //todo: json in request body
    @PostMapping("aspect/create")
    fun createAspect(name: String, measureUnit: String?, baseType: String?): Aspect {
        logger.info("New aspect create request: $name, $measureUnit, $baseType")
        return metadataService.createAspect(name, measureUnit, baseType)
    }

    @PostMapping("aspect/create/property")
    fun createPropertyForAspect(aspectId: Long, name: String, propertyAspect: String, propertyPower: String): Aspect {
        logger.info("Create property for aspect: $aspectId, $name, $propertyAspect, $propertyPower")
        return metadataService.createPropertyForAspect(aspectId, name, propertyAspect, propertyPower)
    }


    @GetMapping("aspect/get/{name}")
    fun getAspect(@PathVariable name: String): Aspect? {
        logger.debug("Get aspect request: $name")
        return metadataService.getAspect(name)
    }

    @GetMapping("aspect/get/all")
    fun getAspects(): List<Aspect> {
        logger.debug("Get all aspects request")
        return metadataService.getAspects()
    }
}

private val logger = loggerFor<MetadataApi>()