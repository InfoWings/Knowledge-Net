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
        val properties: List<AspectPropertyData> = emptyList(),
        val version: Int = 0
) {
    fun withName(name: String) = copy(name = name)

    fun withMeasure(measure: String) = copy(measure = measure)

    fun withDomain(domain: String) = copy(domain = domain)

    fun withBaseType(baseType: String) = copy(baseType = baseType)
}


@Serializable
data class AspectPropertyData(
        val id: String,
        val name: String,
        val aspectId: String,
        val power: String,
        val version: Int = 0
)