package com.infowings.catalog.external

import com.infowings.catalog.common.*
import com.infowings.catalog.data.AspectPropertyPower
import com.infowings.catalog.data.AspectService
import com.infowings.catalog.data.toAspectData
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
        return aspectService.createAspect(aspectData).toAspectData()
    }

    @GetMapping("get/{name}")
    fun getAspect(@PathVariable name: String): AspectData? {
        logger.debug("Get aspect request: $name")
        return aspectService.findByName(name)?.toAspectData()
    }

    @GetMapping("all")
    fun getAspects(): AspectsList {
        logger.debug("Get all aspects request")
        return AspectsList(aspectService.getAspects().toAspectData())
    }

    @PostMapping("edit/name")
    fun editAspectName(@RequestParam("aspect_id") id: String, @RequestParam("name") newName: String): AspectData {
        logger.debug("Change aspect $id name to $newName")
        return aspectService.changeName(id, newName).toAspectData()
    }

    @PostMapping("edit/measure")
    fun editAspectMeasure(@RequestParam("aspect_id") id: String, @RequestParam("measure") measureName: String): AspectData {
        logger.debug("Change aspect $id measure to $measureName")
        val measure = GlobalMeasureMap[measureName]
                ?: throw IllegalArgumentException("There are no measure with name $measureName")
        return aspectService.changeMeasure(id, measure).toAspectData()
    }

    @PostMapping("edit/base_type")
    fun editAspectBaseType(@RequestParam("aspect_id") id: String, @RequestParam("base_type") baseType: String): AspectData {
        logger.debug("Change aspect $id baseType to $baseType")
        return aspectService.changeBaseType(id, BaseType.restoreBaseType(baseType)).toAspectData()
    }

    @GetMapping("property/get")
    fun getPropertyById(@RequestParam("property_id") propertyId: String): AspectPropertyData {
        logger.debug("Get aspect property request: $propertyId")
        return aspectService.loadAspectProperty(propertyId).toAspectPropertyData()
    }

    @PostMapping("property/create")
    fun createAspectProperty(@RequestParam("aspect_id") id: String, @RequestParam("property") property: AspectPropertyData): AspectData {
        logger.debug("Create aspect property $property for aspect $id")
        return aspectService.addProperty(id, property).toAspectData()
    }

    @PostMapping("property/edit/name")
    fun editAspectPropertyName(@RequestParam("property_id") propertyId: String, @RequestParam("name") newName: String): AspectPropertyData {
        logger.debug("Change aspect property $propertyId name to $newName")
        return aspectService.changePropertyName(propertyId, newName).toAspectPropertyData()
    }

    @PostMapping("property/edit/name")
    fun editAspectPropertyPower(@RequestParam("property_id") propertyId: String, @RequestParam("power") newPower: AspectPropertyPower): AspectPropertyData {
        logger.debug("Change aspect property $propertyId power to $newPower")
        return aspectService.changePropertyPower(propertyId, newPower).toAspectPropertyData()
    }

    @PostMapping("property/edit/aspect")
    fun editAspectPropertyAspect(@RequestParam("property_id") propertyId: String, @RequestParam("aspect_id") aspectId: String): AspectPropertyData {
        logger.debug("Change aspect property $propertyId aspect to $aspectId")
        return aspectService.changePropertyAspect(propertyId, aspectId).toAspectPropertyData()
    }
}
private val logger = loggerFor<AspectApi>()