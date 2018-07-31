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
    val subjectId: String,
    val subjectVersion: Int?
)

@Serializable
data class ObjectUpdateRequest(
    val id: String,
    val name: String,
    val description: String?
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
    val objectPropertyId: String,
    val name: String?,
    val description: String?
)

data class ValueCreateRequest(
    val value: ObjectValueData,
    val description: String?,
    val objectPropertyId: String,
    val measureId: String?,
    val aspectPropertyId: String?,
    val parentValueId: String?
) {
    constructor(value: ObjectValueData, description: String?, objectPropertyId: String) : this(value, description, objectPropertyId, null, null, null)

    constructor(value: ObjectValueData, description: String?, objectPropertyId: String, measureId: String) : this(
        value,
        description,
        objectPropertyId,
        measureId,
        null,
        null
    )

    fun toDTO() = ValueCreateRequestDTO(value.toDTO(), description, objectPropertyId, measureId, aspectPropertyId, parentValueId)

    companion object {
        fun root(value: ObjectValueData, description: String?, objectPropertyId: String, measureId: String? = null) = ValueCreateRequest(
            value = value,
            description = description,
            objectPropertyId = objectPropertyId,
            measureId = measureId,
            aspectPropertyId = null,
            parentValueId = null
        )
    }
}

@Serializable
data class ValueCreateRequestDTO(
    val value: ValueDTO,
    val description: String?,
    val objectPropertyId: String,
    val measureId: String?,
    val aspectPropertyId: String?,
    val parentValueId: String?
) {
    fun toRequest() = ValueCreateRequest(
        value = value.toData(),
        description = description,
        objectPropertyId = objectPropertyId,
        aspectPropertyId = aspectPropertyId,
        parentValueId = parentValueId,
        measureId = measureId
    )
}

data class ValueUpdateRequest(val valueId: String, val value: ObjectValueData, val description: String?) {
    fun toDTO() = ValueUpdateRequestDTO(valueId, value.toDTO(), description)
}

@Serializable
data class ValueUpdateRequestDTO(
    val valueId: String,
    val value: ValueDTO,
    val description: String?
) {
    fun toRequest() = ValueUpdateRequest(value = value.toData(), valueId = valueId, description = description)
}

@Serializable
data class ObjectCreateResponse(val id: String)

@Serializable
data class ObjectUpdateResponse(val id: String)

@Serializable
data class PropertyCreateResponse(val id: String)

@Serializable
data class PropertyUpdateResponse(val id: String)

@Serializable
data class ValueCreateResponse(val id: String)

@Serializable
data class ValueUpdateResponse(val id: String)