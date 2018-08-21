package com.infowings.catalog.common

import kotlinx.serialization.Serializable

@Serializable
data class ObjectsResponse(
    val objects: List<ObjectGetResponse>
)

@Serializable
data class ObjectGetResponse(
    val id: String,
    val name: String,
    val description: String?,
    val subjectName: String,
    val propertiesCount: Int
)

@Serializable
data class DetailedObjectViewResponse(
    val id: String,
    val name: String,
    val description: String?,
    val subjectName: String,
    val propertiesCount: Int,
    val objectPropertyViews: List<DetailedObjectPropertyViewResponse>
)

@Serializable
data class DetailedObjectPropertyViewResponse(
    val id: String,
    val name: String?,
    val description: String?,
    val aspect: AspectData,
    val cardinality: String,
    val values: List<DetailedRootValueViewResponse>
)

@Serializable
data class DetailedRootValueViewResponse(
    val id: String,
    val value: ValueDTO,
    val description: String?,
    val children: List<DetailedValueViewResponse>
)

@Serializable
data class DetailedValueViewResponse(
    val id: String,
    val value: ValueDTO,
    val description: String?,
    val aspectProperty: AspectPropertyDataExtended,
    val children: List<DetailedValueViewResponse>
)

@Serializable
data class ObjectEditDetailsResponse(
    val id: String,
    val name: String,
    val description: String?,
    val subjectName: String,
    val subjectId: String,
    val version: Int,
    val properties: List<ObjectPropertyEditDetailsResponse>
)

@Serializable
data class ObjectPropertyEditDetailsResponse(
    val id: String,
    val name: String?,
    val description: String?,
    val version: Int,
    val rootValues: List<ValueTruncated>,
    val valueDescriptors: List<ValueTruncated>,
    val aspectDescriptor: AspectTree
)

@Serializable
data class ValueTruncated(
    val id: String,
    val value: ValueDTO,
    val description: String?,
    val propertyId: String?,
    val version: Int,
    val childrenIds: List<String>
)