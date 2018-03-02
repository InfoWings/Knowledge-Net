package com.infowings.catalog.data

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import com.infowings.catalog.common.BaseType
import com.infowings.catalog.common.Measure


enum class AspectPropertyPower {
    ZERO, ONE, INFINITY
}

/**
 * Аспект - https://iwings.atlassian.net/wiki/spaces/CHR/pages/219217938
 */
data class Aspect(
    val id: String,
    val name: String,
    val measure: Measure<*>?,
    val domain: AspectDomain? = OpenDomain(measure?.baseType ?: throw IllegalArgumentException("Measure unit cannot be null if no base type specified")),
    val baseType: BaseType? = measure?.baseType
            ?: throw IllegalArgumentException("Measure unit cannot be null if no base type specified"),
    val properties: List<AspectProperty> = emptyList()
) {

    operator fun get(property: String) = properties.first { it.name == property }

    fun toAspectData(): AspectData =
        AspectData(id, name, measure?.name, domain.toString(), baseType?.name, properties.toAspectPropertyData())
}

fun List<Aspect>.toAspectData(): List<AspectData> = map { it.toAspectData() }
fun List<AspectProperty>.toAspectPropertyData(): List<AspectPropertyData> = map { it.toAspectPropertyData() }

data class AspectProperty(
    val id: String,
    val name: String,
    val aspect: Aspect,
    val power: AspectPropertyPower
) {
    fun toAspectPropertyData() = AspectPropertyData(id, name, aspect.id, power.name)
}