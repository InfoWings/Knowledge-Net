package com.infowings.catalog.common

import kotlinx.serialization.Serializable

@Serializable
data class AspectsList(
        val aspects: List<AspectData> = emptyList()
)

@Serializable
data class AspectData(
    val id: String? = null,
    val name: String? = null,
    val measure: String? = null,
    val domain: String? = null,
    val baseType: String? = null,
    val properties: List<AspectPropertyData> = emptyList(),
    val version: Int = 0,
    val subject: SubjectData? = null
)

@Serializable
data class AspectPropertyData(
    val id: String,
    val name: String,
    val aspectId: String,
    val cardinality: String,
    val version: Int = 0
)