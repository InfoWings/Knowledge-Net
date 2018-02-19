package com.infowings.common.catalog.data

import kotlinx.serialization.Serializable


@Serializable
data class AspectData(
    val id: String,
    val name: String,
    val measure: String?,
    val domain: String?,
    val baseType: String?,
    val properties: Set<AspectPropertyData> = emptySet()
)


@Serializable
data class AspectPropertyData(
    val id: String,
    val name: String,
    val aspectId: String,
    val power: String
)