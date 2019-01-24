package com.infowings.catalog.common

import kotlinx.serialization.Serializable

@Serializable
data class ObjectsResponse(
    val objects: List<ObjectGetResponse>,
    val totalObjects: Int
)

@Serializable
data class ObjectGetResponse(
    val id: String,
    val name: String,
    val guid: String?,
    val description: String?,
    val subjectName: String,
    val propertiesCount: Int,
    val lastUpdated: Long?
)


@Serializable
data class DetailedObjectViewResponse(
    val id: String,
    val name: String,
    val guid: String?,
    val description: String?,
    val subjectName: String,
    val propertiesCount: Int,
    val objectPropertyViews: List<DetailedObjectPropertyViewResponse>,
    val lastUpdated: Long?
)

@Serializable
data class ObjectsList(val objects: List<ObjectGetResponse>)

@Serializable
data class DetailedObjectPropertyViewResponse(
    val id: String,
    val name: String?,
    val description: String?,
    val aspect: AspectTruncated,
    val cardinality: String,
    val guid: String?,
    val values: List<DetailedRootValueViewResponse>
)

@Serializable
data class DetailedRootValueViewResponse(
    val id: String,
    val value: ValueDTO,
    val guid: String?,
    val measureSymbol: String?,
    val description: String?,
    val children: List<DetailedValueViewResponse>
)

@Serializable
data class DetailedValueViewResponse(
    val id: String,
    val value: ValueDTO,
    val guid: String?,
    val measureSymbol: String?,
    val description: String?,
    val aspectProperty: AspectPropertyDataExtended,
    val children: List<DetailedValueViewResponse>
)

@Serializable
data class ObjectEditDetailsResponse(
    val id: String,
    val name: String,
    val guid: String?,
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
    val guid: String?,
    val measureName: String?,
    val description: String?,
    val propertyId: String?,
    val version: Int,
    val childrenIds: List<String>
)

@Serializable
data class AspectTruncated(
    val id: String,
    val name: String,
    val baseType: String,
    val referenceBookName: String?,
    val subjectName: String?
)