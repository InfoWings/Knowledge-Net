package com.infowings.catalog.objects

import com.infowings.catalog.common.*
import com.infowings.catalog.objects.edit.SubjectTruncated

data class ObjectEditViewModel(
    val id: String,
    var name: String,
    var subject: SubjectTruncated,
    var description: String?,
    val version: Int,
    var properties: MutableList<ObjectPropertyEditModel>,
    var expanded: Boolean = true
) {
    constructor(response: ObjectEditDetailsResponse) : this(
        response.id,
        response.name,
        SubjectTruncated(response.subjectId, response.subjectName),
        response.description,
        response.version,
        response.properties.map(::ObjectPropertyEditModel).toMutableList()
    )

    fun mergeFrom(response: ObjectEditDetailsResponse) {
        name = response.name
        subject = SubjectTruncated(response.subjectId, response.subjectName)
        description = response.description
        properties = properties.mergeWith(response.properties)
    }
}

data class ObjectPropertyEditModel(
    val id: String? = null,
    var name: String? = null,
    var description: String? = null,
    var aspect: AspectTree? = null,
    var values: MutableList<ObjectPropertyValueEditModel>? = ArrayList(),
    val version: Int,
    var expanded: Boolean = false
) {
    constructor(response: ObjectPropertyEditDetailsResponse) : this(
        response.id,
        response.name,
        response.description,
        response.aspectDescriptor,
        response.rootValues.toTreeView(response.valueDescriptors),
        response.version
    )

    fun mergeWith(response: ObjectPropertyEditDetailsResponse): ObjectPropertyEditModel {
        name = response.name
        description = response.description
        aspect = response.aspectDescriptor
        values = if (values == null && response.rootValues.isNotEmpty()) {
            response.rootValues.toTreeView(response.valueDescriptors)
        } else {
            values!!.mergeWith(response.rootValues, response.valueDescriptors.associateBy { it.id })
        }
        return this
    }
}

fun MutableList<ObjectPropertyEditModel>.mergeWith(response: List<ObjectPropertyEditDetailsResponse>): MutableList<ObjectPropertyEditModel> {
    val existingProperties = this.associateBy { it.id }
    return response.map {
        if (existingProperties.containsKey(it.id)) {
            existingProperties[it.id]?.mergeWith(it) ?: error("Property should already exist")
        } else {
            ObjectPropertyEditModel(it)
        }
    }.toMutableList()
}

fun List<ValueTruncated>.toTreeView(values: List<ValueTruncated>): MutableList<ObjectPropertyValueEditModel> {
    val objectsMap = values.associateBy { it.id }
    return this.map { ObjectPropertyValueEditModel(it, objectsMap) }.toMutableList()
}

data class ObjectPropertyValueEditModel(
    val id: String? = null,
    var value: ObjectValueData? = null,
    var description: String? = null,
    var expanded: Boolean = false,
    var valueGroups: MutableList<AspectPropertyValueGroupEditModel> = ArrayList()
) {

    constructor(value: ValueTruncated, valueMap: Map<String, ValueTruncated>) : this(
        id = value.id,
        value = value.value.toData(),
        description = value.description,
        valueGroups = value.childrenIds
                .map { valueMap[it] ?: error("Child value does not exist in supplied list of values") }
                .sortedBy { it.propertyId }
                .foldRight(mutableListOf<AspectPropertyValueGroupEditModel>()) { childValue, propertyGroups ->
                    when {
                        propertyGroups.isEmpty() -> {
                            propertyGroups.add(AspectPropertyValueGroupEditModel(childValue.propertyId ?: error("Value received from server has no id")))
                            propertyGroups.last().values.add(AspectPropertyValueEditModel(childValue, valueMap))
                            propertyGroups
                        }
                        propertyGroups.last().propertyId == childValue.propertyId -> {
                            propertyGroups.last().values.add(AspectPropertyValueEditModel(childValue, valueMap))
                            propertyGroups
                        }
                        else -> {
                            propertyGroups.add(AspectPropertyValueGroupEditModel(childValue.propertyId ?: error("Value received from server has no id")))
                            propertyGroups.last().values.add(AspectPropertyValueEditModel(childValue, valueMap))
                            propertyGroups
                        }
                    }
                }
    )

    fun mergeWith(value: ValueTruncated, valueMap: Map<String, ValueTruncated>): ObjectPropertyValueEditModel {
        this.value = value.value.toData()
        val existingValuesMap = this.valueGroups.flatMap { it.values }.associateBy { it.id }
        valueGroups = value.childrenIds
            .map { valueMap[it] ?: error("Child value does not exist in supplied list of values") }
            .sortedBy { it.propertyId }
            .foldRight(mutableListOf()) { childValue, propertyGroups ->
                when {
                    propertyGroups.isEmpty() -> {
                        propertyGroups.add(AspectPropertyValueGroupEditModel(childValue.propertyId ?: error("Value received from server has no id")))
                        propertyGroups.last().values.add(existingValuesMap[childValue.id].mergeWith(childValue, valueMap))
                        propertyGroups
                    }
                    propertyGroups.last().propertyId == childValue.propertyId -> {
                        propertyGroups.last().values.add(existingValuesMap[childValue.id].mergeWith(childValue, valueMap))
                        propertyGroups
                    }
                    else -> {
                        propertyGroups.add(AspectPropertyValueGroupEditModel(childValue.propertyId ?: error("Value received from server has no id")))
                        propertyGroups.last().values.add(existingValuesMap[childValue.id].mergeWith(childValue, valueMap))
                        propertyGroups
                    }
                }
            }
        return this
    }
}

fun MutableList<ObjectPropertyValueEditModel>.mergeWith(rootValues: List<ValueTruncated>, valueMap: Map<String, ValueTruncated>): MutableList<ObjectPropertyValueEditModel> {
    val existingValues = this.associateBy { it.id }
    return rootValues.map {
        if (existingValues.containsKey(it.id)) {
            existingValues[it.id]?.mergeWith(it, valueMap) ?: error("Value should already exist")
        } else {
            ObjectPropertyValueEditModel(it, valueMap)
        }
    }.toMutableList()
}

data class AspectPropertyValueGroupEditModel(
    val propertyId: String,
    var values: MutableList<AspectPropertyValueEditModel> = ArrayList()
)

data class AspectPropertyValueEditModel(
    val id: String? = null,
    var value: ObjectValueData? = null,
    var description: String? = null,
    var expanded: Boolean = false,
    var children: MutableList<AspectPropertyValueGroupEditModel> = mutableListOf()
) {
    constructor(value: ValueTruncated, valueMap: Map<String, ValueTruncated>) : this(
        id = value.id,
        value = value.value.toData(),
        description = value.description,
        children = value.childrenIds
            .map { valueMap[it] ?: error("Child value does not exist in supplied list of values") }
            .sortedBy { it.propertyId }
            .foldRight(mutableListOf<AspectPropertyValueGroupEditModel>()) { childValue, propertyGroups ->
                when {
                    propertyGroups.isEmpty() -> {
                        propertyGroups.add(AspectPropertyValueGroupEditModel(childValue.propertyId ?: error("Value received from server has no id")))
                        propertyGroups.last().values.add(AspectPropertyValueEditModel(childValue, valueMap))
                        propertyGroups
                    }
                    propertyGroups.last().propertyId == childValue.propertyId -> {
                        propertyGroups.last().values.add(AspectPropertyValueEditModel(childValue, valueMap))
                        propertyGroups
                    }
                    else -> {
                        propertyGroups.add(AspectPropertyValueGroupEditModel(childValue.propertyId ?: error("Value received from server has no id")))
                        propertyGroups.last().values.add(AspectPropertyValueEditModel(childValue, valueMap))
                        propertyGroups
                    }
                }
            }
    )
}

fun AspectPropertyValueEditModel?.mergeWith(value: ValueTruncated, valueMap: Map<String, ValueTruncated>): AspectPropertyValueEditModel =
    if (this == null)
        AspectPropertyValueEditModel(value, valueMap)
    else {
        this.value = value.value.toData()
        val existingValuesMap = this.children.flatMap { it.values }.associateBy { it.id }
        this.children = value.childrenIds
            .map { valueMap[it] ?: error("Child value does not exist in supplied list of values") }
            .sortedBy { it.propertyId }
            .foldRight(mutableListOf()) { childValue, propertyGroups ->
                when {
                    propertyGroups.isEmpty() -> {
                        propertyGroups.add(AspectPropertyValueGroupEditModel(childValue.propertyId ?: error("Value received from server has no id")))
                        propertyGroups.last().values.add(existingValuesMap[childValue.id].mergeWith(childValue, valueMap))
                        propertyGroups
                    }
                    propertyGroups.last().propertyId == childValue.propertyId -> {
                        propertyGroups.last().values.add(existingValuesMap[childValue.id].mergeWith(childValue, valueMap))
                        propertyGroups
                    }
                    else -> {
                        propertyGroups.add(AspectPropertyValueGroupEditModel(childValue.propertyId ?: error("Value received from server has no id")))
                        propertyGroups.last().values.add(existingValuesMap[childValue.id].mergeWith(childValue, valueMap))
                        propertyGroups
                    }
                }
            }
        this
    }
