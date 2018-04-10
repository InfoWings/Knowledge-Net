package com.infowings.catalog.common

import kotlinx.serialization.Serializable

@Serializable
data class SuggestedMeasureData(
    val measureNames: List<String>,
    val measureGroupNames: List<String> = emptyList()
)