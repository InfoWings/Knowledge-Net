package com.infowings.catalog.external

import com.infowings.catalog.data.Aspect
import com.infowings.catalog.data.AspectProperty
import com.infowings.catalog.data.AspectService
import com.infowings.catalog.loggerFor
import com.infowings.common.catalog.data.AspectData
import com.infowings.common.catalog.data.AspectPropertyData
import org.springframework.web.bind.annotation.*

//todo: перехватывание exception и генерация внятных сообщений об ошибках наружу
@RestController
@RequestMapping("/api/aspect")
class AspectApi(val aspectService: AspectService) {

    //todo: json in request body
    @PostMapping("create")
    fun createAspect(aspectData: AspectData): AspectData {
        logger.info("New aspect create request: $aspectData")
        return aspectService.createAspect(aspectData).toAspectData()
    }

//    @PostMapping("create/property")
//    fun createPropertyForAspect(aspectId: Long, name: String, propertyAspect: String, propertyPower: String): Aspect {
//        logger.info("Create property for aspect: $aspectId, $name, $propertyAspect, $propertyPower")
//        return aspectService.createPropertyForAspect(aspectId, name, propertyAspect, propertyPower)
//    }


    @GetMapping("get/{name}")
    fun getAspect(@PathVariable name: String): AspectData? {
        logger.debug("Get aspect request: $name")
        return aspectService.findByName(name)?.toAspectData()
    }

    @GetMapping("all")
    fun getAspects(): List<AspectData> {
        logger.debug("Get all aspects request")
        return aspectService.getAspects().toAspectData()
    }

    private fun Aspect.toAspectData() = AspectData(id, name, measure?.name, domain.toString(), baseType?.name, properties.toAspectPropertyData())

    private fun List<Aspect>.toAspectData(): List<AspectData> = map { it.toAspectData() }

    private fun List<AspectProperty>.toAspectPropertyData(): List<AspectPropertyData> = map { AspectPropertyData(it.id, it.name, it.aspect.id, it.power.name) }

}

private val logger = loggerFor<AspectApi>()