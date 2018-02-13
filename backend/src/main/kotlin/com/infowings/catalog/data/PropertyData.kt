package com.infowings.catalog.data

import com.infowings.catalog.PropertyPower


data class PropertyEntity(
    val id: Long,
    val name: String,
    val aspect: Aspect,
    val propertyPower: PropertyPower
)

class PropertyService {
    fun findByName(name: String): PropertyEntity? = null
    fun save(name: PropertyEntity): PropertyEntity? = null

    fun save(name: String, aspect: Aspect, propertyPower: String): PropertyEntity {
        val power = PropertyPower.valueOf(propertyPower)
        return PropertyEntity(0, name, aspect, power)
//        return save(PropertyEntity(0, name, aspect, power))
    }

}