package com.infowings.catalog.common.guid

import com.infowings.catalog.common.ValueDTO
import kotlinx.serialization.Serializable

@Serializable
data class BriefObjectViewResponse(
    val name: String,
    val subjectName: String?
)

@Serializable
data class BriefValueViewResponse(
    val guid: String?,
    val value: ValueDTO,
    val propertyName: String?,
    val aspectName: String,
    val measure: String?
)