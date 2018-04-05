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
    val description: String?
)