package com.infowings.catalog.common

import kotlinx.serialization.Serializable

@Serializable
data class AspectsList(
    val aspects: List<AspectData> = emptyList()
)

enum class PropertyCardinality {
    ZERO, ONE, INFINITY
}

@Serializable
data class AspectData(
    val id: String? = null,
    val name: String? = null,
    val measure: String? = null,
    val domain: String? = null,
    val baseType: String? = null,
    val properties: List<AspectPropertyData> = emptyList(),
    val version: Int = 0,
    val subject: SubjectData? = null,
    val deleted: Boolean = false,
    val description: String? = null,
    val refBookName: String? = null
)

@Serializable
data class AspectPropertyData(
    val id: String,
    val name: String,
    val aspectId: String,
    val cardinality: String,
    val description: String?,
    val version: Int = 0,
    val deleted: Boolean = false
)

/** Data about AspectProperty together with data about relevant aspect */
@Serializable
data class AspectPropertyDataExtended(
    val id: String,
    val name: String,
    val aspectId: String,
    val cardinality: String,
    val aspectName: String,
    val aspectMeasure: String?,
    val aspectDomain: String,
    val aspectBaseType: String
)

/** Helpful extensions */
val emptyAspectData: AspectData
    get() = AspectData(null, "", null, null, null)

val emptyAspectPropertyData: AspectPropertyData
    get() = AspectPropertyData("", "", "", "", null)

fun AspectData.actualData() = copy(properties = properties.filter { !it.deleted })