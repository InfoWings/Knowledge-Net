package com.infowings.catalog.common.guid

import com.infowings.catalog.common.ValueDTO
import kotlinx.serialization.Serializable

@Serializable
data class BriefObjectViewResponse(
    val name: String,
    val subjectName: String?
)

data class BriefObjectView(
    val id: String,
    val guid: String,
    val name: String,
    val subjectName: String?
) {
    companion object {
        fun of(id: String, guid: String, response: BriefObjectViewResponse) = BriefObjectView(id, guid, response.name, response.subjectName)
    }
}

@Serializable
data class BriefValueViewResponse(
    val guid: String?,
    val value: ValueDTO,
    val propertyName: String?,
    val aspectName: String,
    val measure: String?,
    val objectId: String,
    val objectGuid: String
)