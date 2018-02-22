package com.infowings.catalog.common

import kotlinx.serialization.Serializable

@Serializable
data class AspectsList(
        val aspects: List<AspectData> = emptyList()
)

@Serializable
data class AspectData(
        val id: String?,
        val name: String,
        val measure: String?,
        val domain: String?,
        val baseType: String?,
        val properties: List<AspectPropertyData> = emptyList()
) {
    fun withName(name: String) = AspectData(id, name, measure, domain, baseType, properties)

    fun withMeasure(measure: String) = AspectData(id, name, measure, domain, baseType, properties)

    fun withDomain(domain: String) = AspectData(id, name, measure, domain, baseType, properties)

    fun withBaseType(baseType: String) = AspectData(id, name, measure, domain, baseType, properties)
}


@Serializable
data class AspectPropertyData(
    val id: String,
    val name: String,
    val aspectId: String,
    val power: String
)