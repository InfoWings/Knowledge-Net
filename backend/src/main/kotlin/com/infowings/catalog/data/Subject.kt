package com.infowings.catalog.data

import com.infowings.catalog.common.SubjectData

data class Subject(
    val id: String,
    val name: String,
    val aspects: List<Aspect> = emptyList()
)

fun Subject.toSubjectData(): SubjectData {
    return SubjectData(
        id = this.id,
        name = this.name,
        aspectIds = this.aspects.map { it.id })
}