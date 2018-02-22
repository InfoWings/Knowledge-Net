package com.infowings.catalog.common

import kotlinx.serialization.Serializable

@Serializable
data class AspectsList(
        val aspects: Array<AspectData> = emptyArray()
)

@Serializable
data class AspectData(
        var id: String,
        var name: String,
        var measure: String?,
        var domain: String?,
        var baseType: String?,
        var properties: List<AspectPropertyData>? = null
)


@Serializable
data class AspectPropertyData(
    val id: String,
    val name: String,
    val aspectId: String,
    val power: String
)