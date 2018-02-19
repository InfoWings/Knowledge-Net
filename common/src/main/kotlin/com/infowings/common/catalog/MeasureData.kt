package com.infowings.common.catalog

import kotlinx.serialization.Serializable

@Serializable
data class MeasureData(val name: String, val symbol: String, var baseType: String)

@Serializable
data class MeasureGroupData(val name: String, val measureList: List<MeasureData>)