package com.infowings.catalog.common

import com.infowings.catalog.common.objekt.Reference
import kotlinx.serialization.Serializable

@Serializable
data class AspectsList(
    val aspects: List<AspectData> = emptyList()
)

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

    companion object {
        fun initial(name: String) = AspectData(name = name)
    }
}

@Serializable
data class AspectPropertyData(
    val id: String,
    val name: String?,
    val aspectId: String,
    val cardinality: String,
    val description: String?,
    val version: Int = 0,
    val deleted: Boolean = false,
    val guid: String? = null
) {
    companion object {
        fun Initial(name: String, description: String?, aspectId: String, cardinality: String) =
            AspectPropertyData(id = "", name = name, description = description, aspectId = aspectId, cardinality = cardinality, version = 0, deleted = false)
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
    get() = AspectPropertyData("", "", "", "", null)

fun AspectData.actualData() = copy(properties = properties.filter { !it.deleted })