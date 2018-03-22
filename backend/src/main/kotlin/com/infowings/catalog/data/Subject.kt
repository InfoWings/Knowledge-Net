package com.infowings.catalog.data

import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.data.aspect.Aspect

data class Subject(
    val id: String,
    val name: String,
    val aspects: List<Aspect> = emptyList()
)

fun Subject.toSubjectData() = SubjectData(
    id = this.id,
    name = this.name,
    aspects = this.aspects.map { it.toAspectData() }
)
