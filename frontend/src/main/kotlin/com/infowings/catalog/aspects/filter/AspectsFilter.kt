package com.infowings.catalog.aspects.filter

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectHint
import com.infowings.catalog.common.SubjectData

data class AspectsFilter(val subjects: List<SubjectData?>, val excludedAspects: List<AspectHint>) {
    private val subjectIds = subjects.map { it?.id }.toSet()
    private val excludedAspectGuids = excludedAspects.map { it.guid }.toSet()

    fun applyToAspects(aspects: List<AspectData>): List<AspectData> {
        return if (subjectIds.isEmpty()) aspects else
            aspects.filter {
                subjectIds.contains(it.subject?.id) || excludedAspectGuids.contains(it.guid)
            }
    }
}