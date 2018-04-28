package com.infowings.catalog.common

/**
 * Object data for passing to frontend
 * It must have no OrientDB data structure as well as no data types
 * that has no sense outside of backend
 */
data class ObjectData(
    val id: String?,
    val name: String,
    val description: String?,
    val subjectId: String,
    val propertyIds: List<String>
)