package com.infowings.catalog.objects

import com.infowings.catalog.common.*

data class ObjectLazyViewModel(
    val id: String,
    val name: String,
    val guid: String?,
    val description: String?,
    val subjectName: String,
    val objectPropertiesCount: Int,
    val objectProperties: List<ObjectPropertyViewModel>? = null,
    var expanded: Boolean = false,
    var expandAllFlag: Boolean = false
) {
    fun expandAll() {
        expanded = true
        objectProperties?.forEach { property ->
            property.values.forEach { it.expandAll() }
        }
    }
}

data class ObjectPropertyViewModel(
    val id: String,
    val name: String?,
    val cardinality: PropertyCardinality,
    val description: String?,
    val aspect: AspectTruncated,
    val values: List<ObjectPropertyValueViewModel>
) {
    constructor(objectPropertyView: DetailedObjectPropertyViewResponse) : this(
        objectPropertyView.id,
        objectPropertyView.name,
        PropertyCardinality.valueOf(objectPropertyView.cardinality),
        objectPropertyView.description,
        objectPropertyView.aspect,
        objectPropertyView.values.map(::ObjectPropertyValueViewModel)
    )
}

data class ObjectPropertyValueViewModel(
    val id: String,
    val value: ObjectValueData,
    val measureSymbol: String?,
    val description: String?,
    val guid: String?,
    val valueGroups: List<AspectPropertyValueGroupViewModel>,
    var expanded: Boolean = false
) {

    constructor(objectPropertyValueDetailed: DetailedRootValueViewResponse) : this(
        id = objectPropertyValueDetailed.id,
        value = objectPropertyValueDetailed.value.toData(),
        measureSymbol = objectPropertyValueDetailed.measureSymbol,
        description = objectPropertyValueDetailed.description,
        guid = objectPropertyValueDetailed.guid,
        valueGroups = objectPropertyValueDetailed.children.groupBy { it.aspectProperty }.toList().map {
            AspectPropertyValueGroupViewModel(
                AspectPropertyViewModel(it.first),
                it.second.map(::AspectPropertyValueViewModel)
            )
        }
    )

    fun expandAll() {
        expanded = true
        valueGroups.forEach { valueGroup ->
            valueGroup.values.forEach { it.expandAll() }
        }
    }

}

data class AspectPropertyValueGroupViewModel(
    val property: AspectPropertyViewModel,
    val values: List<AspectPropertyValueViewModel>
)

data class AspectPropertyValueViewModel(
    val id: String,
    val value: ObjectValueData,
    val measureSymbol: String?,
    val description: String?,
    val guid: String?,
    val children: List<AspectPropertyValueGroupViewModel>,
    var expanded: Boolean = false
) {

    constructor(propertyValue: DetailedValueViewResponse) : this(
        id = propertyValue.id,
        value = propertyValue.value.toData(),
        measureSymbol = propertyValue.measureSymbol,
        description = propertyValue.description,
        guid = propertyValue.guid,
        children = propertyValue.children.groupBy { it.aspectProperty }.toList().map {
            AspectPropertyValueGroupViewModel(
                AspectPropertyViewModel(it.first),
                it.second.map(::AspectPropertyValueViewModel)
            )
        }
    )

    fun expandAll() {
        expanded = true
        children.forEach { valueGroup ->
            valueGroup.values.forEach { it.expandAll() }
        }
    }
}

data class AspectPropertyViewModel(
    val roleName: String?,
    val aspectName: String,
    val baseType: String,
    val refBookName: String?,
    val subjectName: String?
) {
    constructor(aspectPropertyData: AspectPropertyDataExtended) : this(
        aspectPropertyData.name,
        aspectPropertyData.aspectName,
        aspectPropertyData.aspectBaseType,
        aspectPropertyData.refBookName,
        aspectPropertyData.aspectSubjectName
    )

    fun toExtendedPropertyData() = AspectPropertyDataExtended(
        name = roleName ?: "",
        aspectName = aspectName,
        aspectBaseType = baseType,
        aspectSubjectName = subjectName,
        refBookName = refBookName
    )
}

fun List<ObjectGetResponse>.toLazyView(detailedObjectsView: Map<String, DetailedObjectViewResponse>) =
    this.map {
        ObjectLazyViewModel(
            it.id,
            it.name,
            it.guid,
            it.description,
            it.subjectName,
            it.propertiesCount,
            detailedObjectsView[it.id]?.objectPropertyViews?.map(::ObjectPropertyViewModel)
        )
    }

fun List<ObjectLazyViewModel>.mergeDetails(detailedObjectsView: Map<String, DetailedObjectViewResponse>) =
    this.map {
        if (detailedObjectsView[it.id] == null) {
            ObjectLazyViewModel(
                it.id,
                it.name,
                it.guid,
                it.description,
                it.subjectName,
                it.objectPropertiesCount,
                expanded = it.expanded
            )
        } else {
            val detailedObject = detailedObjectsView[it.id] ?: error("Should never happened")
            ObjectLazyViewModel(
                detailedObject.id,
                detailedObject.name,
                detailedObject.guid,
                detailedObject.description,
                detailedObject.subjectName,
                detailedObject.propertiesCount,
                it.objectProperties ?: detailedObject.objectPropertyViews.map(::ObjectPropertyViewModel),
                it.expanded
            ).also { newObject ->
                if (it.expandAllFlag) {
                    newObject.expandAll()
                }
            }
        }
    }

