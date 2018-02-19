package com.infowings.catalog.external

import com.infowings.catalog.data.Aspect
import com.infowings.catalog.data.AspectService
import com.infowings.catalog.loggerFor
import com.infowings.common.catalog.data.AspectData
import org.springframework.web.bind.annotation.*

//todo: перехватывание exception и генерация внятных сообщений об ошибках наружу
@RestController
@RequestMapping("/aspect")
class AspectApi(val aspectService: AspectService) {

    //todo: json in request body
    @PostMapping("create")
    fun createAspect(aspectData: AspectData): Aspect {
        logger.info("New aspect create request: $aspectData")
        return aspectService.createAspect(aspectData)
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