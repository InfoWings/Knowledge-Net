package com.infowings.common

import kotlinx.serialization.Serializable

@Serializable
data class SubjectInstanceDto(var state: String, var children: List<SubjectInstanceDto>)


