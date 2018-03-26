package com.infowings.catalog.external

import com.infowings.catalog.auth.JwtInfo
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectsList
import com.infowings.catalog.data.aspect.Aspect
import com.infowings.catalog.data.aspect.AspectService
import com.infowings.catalog.data.aspect.toAspectData
import com.infowings.catalog.loggerFor
import org.springframework.web.bind.annotation.*
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import java.security.Principal


//todo: перехватывание exception и генерация внятных сообщений об ошибках наружу
@RestController
@RequestMapping("/api/aspect")
class AspectApi(val aspectService: AspectService) {

    //todo: json in request body
    @PostMapping("create")
    fun createAspect(@RequestBody aspectData: AspectData, principal: Principal): AspectData {
        logger.info("New aspect create request: $aspectData")
        val user = principal.name
        logger.info("user: $user")
        return aspectService.save(aspectData, user).toAspectData()
    }

    @PostMapping("update")
    fun updateAspect(@RequestBody aspectData: AspectData, principal: Principal): AspectData {
        logger.info("Update aspect request: $aspectData")
        return aspectService.save(aspectData, principal.name).toAspectData()
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

    @GetMapping("remove")
    fun removeAspect(@RequestBody aspect: Aspect) {
        logger.debug("Remove aspect request: ${aspect.id}")
        aspectService.remove(aspect)
    }

    @GetMapping("forceRemove")
    fun forceRemoveAspect(@RequestBody aspect: Aspect) {
        logger.debug("Forced remove aspect request: ${aspect.id}")
        aspectService.remove(aspect, true)
    }
}
private val logger = loggerFor<AspectApi>()