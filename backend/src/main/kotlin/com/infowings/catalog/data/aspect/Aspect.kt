package com.infowings.catalog.data.aspect

import com.infowings.catalog.common.*
import com.infowings.catalog.data.Subject
import com.infowings.catalog.data.toSubjectData


/**
 * Аспект - https://iwings.atlassian.net/wiki/spaces/CHR/pages/219217938
 */
data class Aspect(
    val id: String,
    val name: String,
    val measure: Measure<*>?,
    val domain: AspectDomain? = OpenDomain(
        measure?.baseType
                ?: throw IllegalArgumentException("Measure unit cannot be null if no base type specified")
    ),
    val baseType: BaseType? = measure?.baseType
            ?: throw IllegalArgumentException("Measure unit cannot be null if no base type specified"),
    val properties: List<AspectProperty> = emptyList(),
    val version: Int = 0,
    val subject: Subject? = null,
    val deleted: Boolean = false,
    val description: String? = null,
    val refBookName: String?    // только имя, потому что ссылка на RefBook чревата разверткой дерева - а всегда
    // таскать за собой все дерево не хочется
) {

    operator fun get(property: String) = properties.filter { it.name == property }

    fun getSubjectName() = subject?.name

    fun toAspectData(): AspectData =
        AspectData(
            id,
            name,
            measure?.name,
            domain?.toString(),
            baseType?.name,
            properties.toAspectPropertyData(),
            version,
            subject?.toSubjectData(),
            deleted,
            description,
            refBookName
        )
}

fun List<Aspect>.toAspectData(): List<AspectData> = map { it.toAspectData() }
fun List<AspectProperty>.toAspectPropertyData(): List<AspectPropertyData> = map { it.toAspectPropertyData() }

data class AspectProperty(
    val id: String,
    val name: String,
    val aspect: Aspect,
    val cardinality: PropertyCardinality,
    val version: Int = 0
) {
    fun toAspectPropertyData() = AspectPropertyData(id, name, aspect.id, cardinality.name, version)
}