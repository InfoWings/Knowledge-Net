package com.infowings.catalog.common.guid

import kotlinx.serialization.Serializable

/**
 * EntityClass is enumeration of entities from business logic level viewpoint
 */
enum class EntityClass {
    ASPECT,
    ASPECT_PROPERTY,
    SUBJECT,
    OBJECT,
    OBJECT_PROPERTY,
    OBJECT_VALUE,
    REFBOOK_ITEM
}

/**
 * Data structure to represent response on request of entity metadata
 * by it's metadata
 *
 * It allows to figure out kind of entity is represented by some guid
 */
@Serializable
data class EntityMetadata(
    val guid: String,               // guid used in request
    val entityClass: EntityClass,   // class of entity with such guid
    val id: String                  // Orient Id of entity. Due to some legacy logic we still use Orient Id
                                    // in requests to backend. At some moment it will be eliminated
)