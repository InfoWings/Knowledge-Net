package com.infowings.catalog.objects

import com.infowings.catalog.common.*
import com.infowings.catalog.objects.edit.SubjectTruncated

data class ObjectEditModel(
    val id: String,
    var name: String,
    var subject: SubjectTruncated,
    var description: String?,
    var properties: MutableList<ObjectPropertyEditModel>,
    var expanded: Boolean = true
) {
    constructor(response: ObjectEditDetailsResponse) : this(
        response.id,
        response.name,
        SubjectTruncated(response.subjectId, response.subjectName),
        response.description,
        response.properties.map(::ObjectPropertyEditModel).toMutableList()
    )
}

data class ObjectPropertyEditModel(
    val id: String? = null,
    var name: String? = null,
    var cardinality: PropertyCardinality? = null,
    var description: String? = null,
    var aspect: TreeAspectResponse? = null,
    var values: MutableList<ObjectPropertyValueEditModel>? = ArrayList(),
    var expanded: Boolean = true
) {
    constructor(response: ObjectPropertyEditDetailsResponse) : this(
        response.id,
        response.name,
        response.cardinality,
        response.description,
        response.aspectDescriptor,
        response.rootValues.toTreeView(response.valueDescriptors)
    )
}

fun List<ValueTruncated>.toTreeView(values: List<ValueTruncated>): MutableList<ObjectPropertyValueEditModel> {
    val objectsMap = values.associateBy { it.id }
    return this.map { ObjectPropertyValueEditModel(it, objectsMap) }.toMutableList()
}

data class ObjectPropertyValueEditModel(
    val id: String? = null,
    var value: ObjectValueData? = null,
    var expanded: Boolean = false,
    var valueGroups: MutableList<AspectPropertyValueGroupEditModel> = ArrayList()
) {

    constructor(value: ValueTruncated, valueMap: Map<String, ValueTruncated>) : this(
        id = value.id,
        value = value.value.toData(),
        valueGroups = value.childrenIds
            .map { valueMap[it] ?: error("Child value does not exist in supplied list of values") }
            .sortedBy { it.propertyId }
            .foldRight(mutableListOf<AspectPropertyValueGroupEditModel>()) { childValue, propertyGroups ->
                when {
                    propertyGroups.isEmpty() -> {
                        propertyGroups.add(AspectPropertyValueGroupEditModel(childValue.propertyId ?: TODO()))
                        propertyGroups.last().values.add(AspectPropertyValueEditModel(childValue, valueMap))
                        propertyGroups
                    }
                    propertyGroups.last().propertyId == childValue.propertyId -> {
                        propertyGroups.last().values.add(AspectPropertyValueEditModel(childValue, valueMap))
                        propertyGroups
                    }
                    else -> {
                        propertyGroups.add(AspectPropertyValueGroupEditModel(childValue.propertyId ?: TODO()))
                        propertyGroups.last().values.add(AspectPropertyValueEditModel(childValue, valueMap))
                        propertyGroups
                    }
                }
            }
    )

}

data class AspectPropertyValueGroupEditModel(
    val propertyId: String,
    var values: MutableList<AspectPropertyValueEditModel> = ArrayList()
)

data class AspectPropertyValueEditModel(
    val id: String? = null,
    var value: ObjectValueData? = null,
    var expanded: Boolean = false,
    var children: MutableList<AspectPropertyValueGroupEditModel> = ArrayList()
) {
    constructor(value: ValueTruncated, valueMap: Map<String, ValueTruncated>) : this(
        id = value.id,
        value = value.value.toData(),
        children = value.childrenIds
            .map { valueMap[it] ?: error("Child value does not exist in supplied list of values") }
            .sortedBy { it.propertyId }
            .foldRight(mutableListOf<AspectPropertyValueGroupEditModel>()) { childValue, propertyGroups ->
                when {
                    propertyGroups.isEmpty() -> {
                        propertyGroups.add(AspectPropertyValueGroupEditModel(childValue.propertyId ?: TODO()))
                        propertyGroups.last().values.add(AspectPropertyValueEditModel(childValue, valueMap))
                        propertyGroups
                    }
                    propertyGroups.last().propertyId == childValue.propertyId -> {
                        propertyGroups.last().values.add(AspectPropertyValueEditModel(childValue, valueMap))
                        propertyGroups
                    }
                    else -> {
                        propertyGroups.add(AspectPropertyValueGroupEditModel(childValue.propertyId ?: TODO()))
                        propertyGroups.last().values.add(AspectPropertyValueEditModel(childValue, valueMap))
                        propertyGroups
                    }
                }
            }
    )

}
