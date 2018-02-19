package com.infowings.catalog.external

import com.infowings.catalog.data.Aspect
import com.infowings.catalog.data.AspectService
import com.infowings.catalog.loggerFor
import org.springframework.web.bind.annotation.*

//todo: перехватывание exception и генерация внятных сообщений об ошибках наружу
@RestController
@RequestMapping("/api/aspect")
class AspectApi(val aspectService: AspectService) {

    //todo: json in request body
    @PostMapping("create")
    fun createAspect(name: String, measureUnit: String?, baseType: String?): Aspect {
        logger.info("New aspect create request: $name, $measureUnit, $baseType")
        return aspectService.createAspect(name, measureUnit, baseType)
    }

//    @PostMapping("create/property")
//    fun createPropertyForAspect(aspectId: Long, name: String, propertyAspect: String, propertyPower: String): Aspect {
//        logger.info("Create property for aspect: $aspectId, $name, $propertyAspect, $propertyPower")
//        return aspectService.createPropertyForAspect(aspectId, name, propertyAspect, propertyPower)
//    }


    @GetMapping("get/{name}")
    fun getAspect(@PathVariable name: String): Aspect? {
        logger.debug("Get aspect request: $name")
        return aspectService.findByName(name)
    }

    @GetMapping("all")
    fun getAspects(): List<Aspect> {
        logger.debug("Get all aspects request")
        return aspectService.getAspects()
    }
}

private val logger = loggerFor<AspectApi>()