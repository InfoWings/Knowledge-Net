package com.infowings.catalog.common.objekt

import com.infowings.catalog.common.ObjectValueData
import com.infowings.catalog.common.ValueDTO
import com.infowings.catalog.common.toDTO
import com.infowings.catalog.common.toData
import kotlinx.serialization.Serializable


@Serializable
data class ObjectCreateRequest(
    val name: String,
    val description: String?,
    val subjectId: String
) {
    companion object {
        fun simple(name: String, subjectId: String) = ObjectCreateRequest(name, null, subjectId)
    }
}

@Serializable
data class ObjectUpdateRequest(
    val id: String,
    val name: String,
    val description: String?,
    val subjectId: String,
    val version: Int
)

@Serializable
data class PropertyCreateRequest(
    val objectId: String,
    val name: String?,
    val description: String?,
    val aspectId: String
)

@Serializable
data class PropertyUpdateRequest(
    val id: String,
    val name: String?,
    val description: String?,
    val version: Int
)


data class ValueCreateRequest(
    val value: ObjectValueData,
    val description: String?,
    val objectPropertyId: String,
    val measureName: String? = null,
    val aspectPropertyId: String? = null,
    val parentValueId: String? = null
) {
    fun toDTO() = ValueCreateRequestDTO(value.toDTO(), description, objectPropertyId, measureName, aspectPropertyId, parentValueId)
}

@Serializable
data class ValueCreateRequestDTO(
    val value: ValueDTO,
    val description: String?,
    val objectPropertyId: String,
    val measureName: String?,
    val aspectPropertyId: String?,
    val parentValueId: String?
) {
    fun toRequest() = ValueCreateRequest(
        value = value.toData(),
        description = description,
        objectPropertyId = objectPropertyId,
        aspectPropertyId = aspectPropertyId,
        parentValueId = parentValueId,
        measureName = measureName
    )
}

data class ValueUpdateRequest(
    val valueId: String,
    val value: ObjectValueData,
    val measureName: String?,
    val description: String?,
    val version: Int
) {
    fun toDTO() = ValueUpdateRequestDTO(valueId, value.toDTO(), measureName, description, version)
}

@Serializable
data class ValueUpdateRequestDTO(
    val valueId: String,
    val value: ValueDTO,
    val measureName: String?,
    val description: String?,
    val version: Int
) {
    fun toRequest() = ValueUpdateRequest(valueId, value.toData(), measureName, description, version)
}

@Serializable
data class ObjectChangeResponse(
    val id: String,
    val name: String,
    val description: String?,
    val subjectId: String,
    val subjectName: String,
    val version: Int,
    val guid: String?
)

@Serializable
data class PropertyCreateResponse(
    val id: String,
    val obj: Reference,
    val rootValue: GuidReference,
    val name: String?,
    val description: String?,
    val version: Int,
    val guid: String?
)

@Serializable
data class PropertyUpdateResponse(
    val id: String,
    val obj: Reference,
    val name: String?,
    val description: String?,
    val version: Int,
    val guid: String?
)

@Serializable
data class PropertyDeleteResponse(
    val id: String,
    val obj: Reference,
    val name: String?,
    val description: String?,
    val version: Int,
    val guid: String?
)

@Serializable
data class ValueChangeResponse(
    val id: String,
    val value: ValueDTO,
    val description: String?,
    val measureName: String?,
    val objectProperty: Reference,
    val aspectPropertyId: String?,
    val parentValue: Reference?,
    val version: Int,
    val guid: String?
)

@Serializable
data class ValueDeleteResponse(
    val deletedValues: List<String>,
    val markedValues: List<String>,
    val objectProperty: Reference,
    val parentValue: Reference?
)

@Serializable
data class Reference(val id: String, val version: Int)

@Serializable
data class GuidReference(val id: String, val guid: String?, val version: Int)

@Serializable
data class ValueRecalculationResponse(val targetMeasure: String, val value: String)

