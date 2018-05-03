package com.infowings.catalog.common

data class ObjectPropertyData(
    val id: String?,
    val name: String,
    val cardinality: PropertyCardinality,
    val objectId: String,
    val aspectId: String,
    val valueIds: List<String>
)