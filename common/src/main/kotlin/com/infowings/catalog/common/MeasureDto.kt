package com.infowings.catalog.common

import kotlinx.serialization.Serializable

@Serializable
data class MeasureNames(val names: List<String> = emptyList())