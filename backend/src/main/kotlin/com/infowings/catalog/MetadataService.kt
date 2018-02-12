package com.infowings.catalog

import com.infowings.catalog.metadata.Aspect
import com.infowings.catalog.metadata.AspectService
import com.infowings.catalog.metadata.PropertyService


class MetadataService(private val aspectService: AspectService, private val propertyService: PropertyService) {
    fun createAspect(name: String, measureUnit: String?, baseType: String?): Aspect {

        if (aspectService.findByName(name) != null)
            throw DataTypeAlreadyExist

        return aspectService.save(name, measureUnit, baseType)
    }

    fun getAspect(name: String): Aspect? = aspectService.findByName(name)
    fun getAspect(id: Long): Aspect? = aspectService.findOne(id)

    fun createPropertyForAspect(aspectId: Long, name: String, propertyAspect: String, propertyPower: String): Aspect {
        val parentAspect = aspectService.findOne(aspectId) ?: throw AspectDoesNotExistId(aspectId)

        val innerAspectEntity = aspectService.findByName(propertyAspect) ?: throw AspectDoesNotExistName(propertyAspect)

        // parentAspect.properties += propertyService.save(name, innerAspectEntity, propertyPower)

        return aspectService.save(parentAspect)
    }

    fun getAspects(): List<Aspect> = aspectService.findAll()
}

object DataTypeAlreadyExist : Throwable()

class AspectDoesNotExistId(id: Long) : Throwable()
class AspectDoesNotExistName(name: String) : Throwable()
