package com.infowings.catalog.objects.filter

import com.infowings.catalog.common.ObjectGetResponse
import com.infowings.catalog.common.SubjectData

data class ObjectsFilter(val subjects: List<SubjectData?>, val excluded: List<ObjectGetResponse>) {
    private val subjectGuids = subjects.map { it?.guid }.toSet()

    //fun applyToAspects() = if (subjectIds.isEmpty()) aspects else
    //    aspects.filter { subjectIds.contains(it.subject?.id) || excludedAspectIds.contains(it.id) }
}