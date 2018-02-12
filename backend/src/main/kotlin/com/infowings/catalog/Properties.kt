package com.infowings.catalog

import com.infowings.catalog.metadata.Aspect

enum class PropertyPower {
    ZERO,
    ONE,
    UNLIMITED
}


data class Property(val id: Long, val name: String, val aspect: Aspect, val power: PropertyPower, val ownerAspect: Aspect?, val ownerEntity: EntityMetadata?)


data class PropertyValue(val property: Property, val value: Any)

data class EntityMetadata(val id: Long, val name: String, val properties: MutableSet<Property>)

data class Entity(val values: MutableMap<Property, PropertyValue>)