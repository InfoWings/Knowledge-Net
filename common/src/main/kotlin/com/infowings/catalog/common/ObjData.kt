package com.infowings.catalog.common

import kotlinx.serialization.Serializable

@Serializable
data class ObjData(
    val id: String? = null,
    val name: String? = null,
    val subject: SubjectData,
    val properties: List<ObjPropertyData> = emptyList()
)

@Serializable
data class ObjPropertyData(
    val id: String? = null,
    val name: String? = null,
    val aspect: AspectData
)

