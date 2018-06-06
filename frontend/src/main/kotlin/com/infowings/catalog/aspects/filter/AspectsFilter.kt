package com.infowings.catalog.aspects.filter

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.SubjectData

data class AspectsFilter(val subjects: List<SubjectData?>, val excludedAspects: List<AspectData>) {
    private val subjectIds = subjects.map { it?.id }.toSet()
    private val excludedAspectIds = excludedAspects.map { it.id }.toSet()

    fun applyToAspects(aspects: List<AspectData>) = if (subjectIds.isEmpty()) aspects else
        aspects.filter { subjectIds.contains(it.subject?.id) || excludedAspectIds.contains(it.id) }
}