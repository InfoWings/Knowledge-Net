package com.infowings.catalog.common

data class SubjectData(
    val id: String? = null,
    val name: String,
    val aspectIds: List<String> = emptyList()
)