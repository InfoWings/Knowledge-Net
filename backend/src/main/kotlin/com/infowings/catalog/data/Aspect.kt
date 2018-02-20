package com.infowings.catalog.data

import com.infowings.common.BaseType


enum class AspectPropertyPower {
    ZERO, ONE, INFINITY
}

/**
 * Аспект - https://iwings.atlassian.net/wiki/spaces/CHR/pages/219217938
 */
data class Aspect(
    val id: String,
    val name: String,
    val measureUnit: Measure<*>?,
    val domain: AspectDomain? = OpenDomain(measureUnit?.baseType ?: throw IllegalArgumentException("Measure unit cannot be null if no base type specified")),
    val baseType: BaseType? = measureUnit?.baseType ?: throw IllegalArgumentException("Measure unit cannot be null if no base type specified"),
    val properties: Set<AspectProperty> = emptySet()
)

data class AspectProperty(
    val id: String,
    val name: String,
    val aspect: Aspect,
    val power: AspectPropertyPower
)