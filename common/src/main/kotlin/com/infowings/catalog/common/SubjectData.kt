package com.infowings.catalog.common

import kotlinx.serialization.Serializable

@Serializable
data class SubjectData(
    val id: String? = null,
    val name: String,
    val aspects: Array<AspectData> = emptyArray()
)