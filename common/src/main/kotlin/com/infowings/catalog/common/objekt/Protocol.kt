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
data class PropertyCreateRequest(
    val objectId: String,
    val name: String?,
    val cardinality: String,
    val aspectId: String
)

data class ValueCreateRequest(
    val value: ObjectValueData,
    val objectPropertyId: String,
    val measureId: String?,
    val aspectPropertyId: String?,
    val parentValueId: String?
) {
    constructor(value: ObjectValueData, objectPropertyId: String) : this(value, objectPropertyId, null, null, null)

    constructor(value: ObjectValueData, objectPropertyId: String, measureId: String) : this(
        value,
        objectPropertyId,
        measureId,
        null,
        null
    )

    fun toDTO() = ValueCreateRequestDTO(value.toDTO(), objectPropertyId, measureId, aspectPropertyId, parentValueId)
}

@Serializable
data class ValueCreateRequestDTO(
    val value: ValueDTO,
    val objectPropertyId: String,
    val measureId: String?,
    val aspectPropertyId: String?,
    val parentValueId: String?
) {
    fun toRequest() = ValueCreateRequest(
        value = value.toData(),
        objectPropertyId = objectPropertyId,
        aspectPropertyId = aspectPropertyId,
        parentValueId = parentValueId,
        measureId = measureId
    )
}

@Serializable
data class ObjectCreateResponse(val id: String)

@Serializable
data class PropertyCreateResponse(val id: String)

@Serializable
data class ValueCreateResponse(val id: String)