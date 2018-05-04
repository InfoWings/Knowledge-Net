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
    val cardinality: String,
    val aspect: AspectData,
    val values: List<ObjectPropertyValueData>
)

@Serializable
data class ObjectPropertyValueData(
    val id: String? = null,
    val characteristics: List<Characteristics>,
    val scalarValue: String
)

@Serializable
data class Characteristics(
    val aspectId: String
)

