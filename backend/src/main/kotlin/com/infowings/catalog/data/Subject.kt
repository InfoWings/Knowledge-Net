package com.infowings.catalog.data

import com.infowings.catalog.common.SubjectData

data class Subject(
    val id: String,
    val name: String,
    val description: String?
)

fun Subject.toSubjectData() = SubjectData(
    id = this.id,
    name = this.name,
    description = this.description
)
