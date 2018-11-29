package com.infowings.catalog.common

import kotlinx.serialization.Serializable

@Serializable
data class SubjectsList(
    val subject: List<SubjectData> = emptyList()
)

@Serializable
data class SubjectData(
    val id: String? = null,
    val name: String,
    override val version: Int = 0,
    val description: String?,
    val deleted: Boolean = false,
    override val guid: String? = null
) : VersionAware, GuidAware {
    companion object {
        fun Initial(name: String, desctription: String? = null) = SubjectData(id = "", name = name, version = 0, description = desctription, deleted = false)
    }
}

val emptySubjectData
    get() = SubjectData(name = "", description = null, version = 1)