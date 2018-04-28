package com.infowings.catalog.common

enum class ObjectPropertyCardinality {
    ZERO, ONE, INFINITY
}

data class ObjectPropertyData(
    val id: String?,
    val name: String,
    val cardinality: ObjectPropertyCardinality,
    val objectId: String,
    val aspectId: String,
    val valueIds: List<String>
)