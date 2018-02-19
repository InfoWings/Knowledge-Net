package com.infowings.common.catalog

import kotlinx.serialization.Serializable

@Serializable
data class SubjectInstanceDto(var state: String, var children: List<SubjectInstanceDto>)


