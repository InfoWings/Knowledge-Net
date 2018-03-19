package com.infowings.catalog.external

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectsList
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.aspect.toAspectData
import com.infowings.catalog.loggerFor
import org.springframework.web.bind.annotation.*

//todo: перехватывание exception и генерация внятных сообщений об ошибках наружу
@RestController
@RequestMapping("/api/aspect")
class AspectApi(val aspectService: AspectService) {

    //todo: json in request body
    @PostMapping("create")
    fun createAspect(@RequestBody aspectData: AspectData): AspectData {
        logger.info("New aspect create request: $aspectData")
        return aspectService.save(aspectData).toAspectData()
    }

    @PostMapping("update")
    fun updateAspect(@RequestBody aspectData: AspectData): AspectData {
        logger.info("Update aspect request: $aspectData")
        return aspectService.save(aspectData).toAspectData()
    }

    @GetMapping("get/{name}")
    fun getAspect(@PathVariable name: String): List<AspectData> {
        logger.debug("Get aspect request: $name")
        return aspectService.findByName(name).map { it.toAspectData() }
    }

    @GetMapping("all")
    fun getAspects(): AspectsList {
        logger.debug("Get all aspects request")
        return AspectsList(aspectService.getAspects().toAspectData())
    }
}
private val logger = loggerFor<AspectApi>()