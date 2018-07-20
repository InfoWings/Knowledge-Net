package com.infowings.catalog.objects

import com.infowings.catalog.common.*

data class ObjectLazyViewModel(
    val id: String,
    val name: String,
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
    val aspect: AspectData,
    val values: List<ObjectPropertyValueViewModel>
) {
    constructor(objectProperty: DetailedObjectPropertyResponse) : this(
        objectProperty.id,
        objectProperty.name,
        PropertyCardinality.valueOf(objectProperty.cardinality),
        objectProperty.description,
        objectProperty.aspect,
        objectProperty.values.map(::ObjectPropertyValueViewModel)
    )
}

data class ObjectPropertyValueViewModel(
    val id: String,
    val value: ObjectValueData,
    val description: String?,
    val valueGroups: List<AspectPropertyValueGroupViewModel>,
    var expanded: Boolean = false
) {

    constructor(objectPropertyValue: RootValueResponse) : this(
        id = objectPropertyValue.id,
        value = objectPropertyValue.value.toData(),
        description = objectPropertyValue.description,
        valueGroups = objectPropertyValue.children.groupBy { it.aspectProperty }.toList().map {
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
    val description: String?,
    val children: List<AspectPropertyValueGroupViewModel>,
    var expanded: Boolean = false
) {

    constructor(propertyValue: ValueResponse) : this(
        id = propertyValue.id,
        value = propertyValue.value.toData(),
        description = propertyValue.description,
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
    val propertyId: String,
    val aspectId: String,
    val cardinality: PropertyCardinality,
    val roleName: String?,
    val aspectName: String,
    val measure: String?,
    val baseType: String,
    val domain: String,
    val refBookName: String?
) {
    constructor(aspectPropertyData: AspectPropertyDataExtended) : this(
        aspectPropertyData.id,
        aspectPropertyData.aspectId,
        PropertyCardinality.valueOf(aspectPropertyData.cardinality),
        aspectPropertyData.name,
        aspectPropertyData.aspectName,
        aspectPropertyData.aspectMeasure,
        aspectPropertyData.aspectBaseType,
        aspectPropertyData.aspectDomain,
        aspectPropertyData.refBookName
    )

    fun toExtendedPropertyData() = AspectPropertyDataExtended(
        id = propertyId,
        name = roleName ?: "",
        cardinality = cardinality.name,
        aspectId = aspectId,
        aspectName = aspectName,
        aspectMeasure = measure,
        aspectDomain = domain,
        aspectBaseType = baseType,
        refBookName = refBookName
    )
}

fun List<ObjectGetResponse>.toLazyView(detailedObjects: Map<String, DetailedObjectResponse>) =
    this.map {
        ObjectLazyViewModel(
            it.id,
            it.name,
            it.description,
            it.subjectName,
            it.propertiesCount,
            detailedObjects[it.id]?.objectProperties?.map(::ObjectPropertyViewModel)
        )
    }

fun List<ObjectLazyViewModel>.mergeDetails(detailedObjects: Map<String, DetailedObjectResponse>) =
    this.map {
        if (detailedObjects[it.id] == null) {
            ObjectLazyViewModel(
                it.id,
                it.name,
                it.description,
                it.subjectName,
                it.objectPropertiesCount,
                expanded = it.expanded
            )
        } else {
            val detailedObject = detailedObjects[it.id] ?: error("Should never happened")
            ObjectLazyViewModel(
                detailedObject.id,
                detailedObject.name,
                detailedObject.description,
                detailedObject.subjectName,
                detailedObject.propertiesCount,
                it.objectProperties ?: detailedObject.objectProperties.map(::ObjectPropertyViewModel),
                it.expanded
            ).also { newObject ->
                if (it.expandAllFlag) {
                    newObject.expandAll()
                }
            }
        }
    }

