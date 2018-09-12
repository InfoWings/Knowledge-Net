package com.infowings.catalog.common.guid

import kotlinx.serialization.Serializable

enum class EntityClass {
    ASPECT,
    ASPECT_PROPERTY,
    SUBJECT,
    OBJECT,
    OBJECT_PROPERTY,
    OBJECT_VALUE,
    REFBOOK_ITEM
}

@Serializable
data class EntityMetadata(val guid: String, val entityClass: EntityClass, val id: String)