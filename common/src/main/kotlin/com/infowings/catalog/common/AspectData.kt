package com.infowings.catalog.common

import com.infowings.catalog.common.objekt.Reference
import kotlinx.serialization.Serializable

@Serializable
data class AspectsList(val aspects: List<AspectData> = emptyList())

@Serializable
data class AspectsHints(
    val byAspectName: List<AspectHint> = emptyList(),
    val byAspectDesc: List<AspectHint> = emptyList(),
    val byRefBookValue: List<AspectHint> = emptyList(),
    val byRefBookDesc: List<AspectHint> = emptyList(),
    val byProperty: List<AspectHint> = emptyList()
) {
    companion object {
        fun empty() = AspectsHints(
            byAspectName = emptyList(),
            byAspectDesc = emptyList(),
            byRefBookDesc = emptyList(),
            byRefBookValue = emptyList(),
            byProperty = emptyList()
        )
    }

    fun defaultOrder() = byAspectName + byProperty + byRefBookValue + byAspectDesc + byRefBookDesc
}

enum class PropertyCardinality {
    ZERO {
        override val label = "Group"
    },
    ONE {
        override val label = "0..1"
    },
    INFINITY {
        override val label = "0..∞"
    };

    abstract val label: String
}

enum class AspectHintSource {
    ASPECT_NAME, ASPECT_DESCRIPTION, REFBOOK_NAME, REFBOOK_DESCRIPTION, ASPECT_PROPERTY_WITH_ASPECT
}

@Serializable
data class AspectHintAspectInfo(
    val guid: String,
    val id: String,
    val name: String,
    val description: String?,
    val subjectName: String
)

@Serializable
data class AspectHintAspectPropInfo(
    val guid: String,
    val id: String,
    val name: String?,
    val cardinality: String,
    val description: String?
)

@Serializable
data class AspectHint(
    val name: String,
    val description: String?,
    val refBookItem: String?,
    val refBookItemDesc: String?,
    val subAspectName: String?,
    val aspectName: String?,
    val subjectName: String?,
    val parentAspect: AspectHintAspectInfo?,
    val property: AspectHintAspectPropInfo?,
    val guid: String,
    val id: String,
    val source: String
) {
    companion object {
        fun byAspect(aspect: AspectData, source: AspectHintSource) =
            AspectHint(
                name = aspect.name,
                description = aspect.description,
                source = source.toString(),
                refBookItem = null,
                refBookItemDesc = null,
                subAspectName = null,
                parentAspect = null,
                property = null,
                aspectName = null,
                subjectName = aspect.nameWithSubject(),
                id = aspect.idStrict(),
                guid = aspect.guidSoft()
            )
    }
}


@Serializable
data class AspectData(
    val id: String? = null,
    val name: String,
    val measure: String? = null,
    val domain: String? = null,
    val baseType: String? = null,
    val properties: List<AspectPropertyData> = emptyList(),
    val version: Int = 0,
    val subject: SubjectData? = null,
    val deleted: Boolean = false,
    val description: String? = null,
    val lastChangeTimestamp: Long? = null,
    val refBookName: String? = null,
    val guid: String? = null
) {
    operator fun get(id: String): AspectPropertyData? = properties.find { it.id == id }

    fun idStrict(): String = id ?: throw IllegalStateException("No id for aspect $this")
    fun guidSoft(): String = guid ?: "???"

    fun nameWithSubject(): String = "$name ( ${subject?.name ?: "Global"} )"

    companion object {
        fun initial(name: String) = AspectData(name = name)
    }
}

@Serializable
data class AspectPropertyData(
    val id: String,
    val name: String?,
    val aspectId: String,
    val aspectGuid: String,
    val cardinality: String,
    val description: String?,
    val version: Int = 0,
    val deleted: Boolean = false,
    override val guid: String? = null
) : GuidAware {
    companion object {
        fun Initial(name: String, description: String?, aspectId: String, aspectGuid: String, cardinality: String) =
            AspectPropertyData(
                id = "",
                name = name,
                description = description,
                aspectId = aspectId,
                aspectGuid = aspectGuid,
                cardinality = cardinality,
                version = 0,
                deleted = false
            )
    }
}

/** Data about AspectProperty together with data about relevant aspect */
@Serializable
data class AspectPropertyDataExtended(
    val name: String?,
    val aspectName: String,
    val aspectBaseType: String,
    val aspectSubjectName: String?,
    val refBookName: String?
)

@Serializable
data class AspectTree(
    val id: String,
    val name: String,
    val subjectId: String? = null,
    val subjectName: String? = null,
    val measure: String? = null,
    val baseType: String? = null,
    val domain: String? = null,
    val refBookId: String? = null,
    val refBookNameSoft: String? = null,
    val deleted: Boolean = false,
    val properties: List<AspectPropertyTree> = emptyList()
)

@Serializable
data class AspectPropertyTree(
    val id: String,
    val cardinality: PropertyCardinality,
    val name: String?,
    val aspect: AspectTree,
    val deleted: Boolean
)

@Serializable
data class AspectPropertyDeleteResponse(
    val id: String,
    val cardinality: PropertyCardinality,
    val name: String?,
    val parentAspect: Reference,
    val childAspect: Reference
)

/** Helpful extensions */
val emptyAspectData: AspectData
    get() = AspectData(null, "", null, null, null)

val emptyAspectPropertyData: AspectPropertyData
    get() = AspectPropertyData("", "", "", "", "", null)

fun AspectData.actualData() = copy(properties = properties.filter { !it.deleted })