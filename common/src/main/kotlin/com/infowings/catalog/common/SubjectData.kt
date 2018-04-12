package com.infowings.catalog.common

import kotlinx.serialization.Serializable

@Serializable
data class SubjectsList(
    val subject: List<SubjectData> = emptyList()
)

@Serializable
data class SubjectData (
    val id: String? = null,
    val name: String,
    override val version: Int = 0,
    val description: String?
) : VersionAware

val emptySubjectData
    get() = SubjectData(name = "", description = null, version = 1)