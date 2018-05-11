package com.infowings.catalog.common

import kotlinx.serialization.Serializable

@Serializable
data class ObjectData(
    val id: String? = null,
    val name: String? = null,
    val subject: SubjectData,
    val properties: List<ObjectPropertyData> = emptyList()
)

@Serializable
data class ObjectPropertyData(
    val id: String? = null,
    val name: String? = null,
    val cardinality: String,
    val aspect: AspectData,
    val values: List<ObjectPropertyValueData>
)

@Serializable
data class ObjectPropertyValueData(
    val id: String? = null,
    val scalarValue: String?,
    val children: List<AspectPropertyValueData>
)

@Serializable
data class AspectPropertyValueData(
    val id: String? = null,
    val scalarValue: String?,
    val aspectProperty: AspectPropertyDataExtended,
    val children: List<AspectPropertyValueData>
)
