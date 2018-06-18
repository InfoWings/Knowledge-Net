package com.infowings.catalog.common

import kotlinx.serialization.Serializable

/**
 * Object data for passing to frontend
 * It must have no OrientDB data structure as well as no data types
 * that has no sense outside of backend
 */
data class ObjectData2(
    val id: String?,
    val name: String,
    val description: String?,
    val subjectId: String,
    val propertyIds: List<String>
)

@Serializable
data class ObjectsResponse(
    val objects: List<ObjectGetResponse>
)

@Serializable
data class ObjectGetResponse(
    val id: String,
    val name: String,
    val description: String?,
    val subjectId: String,
    val subjectName: String,
    val subjectDescription: String?
)

@Serializable
data class DetailedObjectResponse(
    val id: String,
    val name: String,
    val description: String?,
    val subjectId: String,
    val subjectName: String,
    val subjectDescription: String?,
    val objectProperties: List<ObjectPropertyData>
)

@Serializable
data class ObjectData(
    val id: String? = null,
    val name: String? = null,
    val description: String?,
    val subject: SubjectData,
    val properties: List<ObjectPropertyData> = emptyList()
)

@Serializable
data class ObjectPropertyData(
    val id: String? = null,
    val name: String? = null,
    val cardinality: String,
    val aspect: AspectData,
    val values: List<ObjectPropertyValueData>
)

@Serializable
data class ObjectPropertyValueData(
    val id: String? = null,
    val scalarValue: String?,
    val children: List<AspectPropertyValueData>
)

@Serializable
data class AspectPropertyValueData(
    val id: String? = null,
    val scalarValue: String?,
    val aspectProperty: AspectPropertyDataExtended,
    val children: List<AspectPropertyValueData>
) {

    fun forEach(f: (AspectPropertyValueData, AspectPropertyValueData?) -> Unit, parent: AspectPropertyValueData?) {
        f(this, parent)
        children.forEach { it.forEach(f, this) }
    }
}