package com.infowings.catalog.objects

import com.infowings.catalog.common.*

data class ObjectViewModel(
    val id: String?,
    var name: String?,
    var subject: SubjectData?,
    var properties: MutableList<ObjectPropertyViewModel>,
    var expanded: Boolean = false
) {
    constructor(objectData: ObjectData) : this(
        objectData.id,
        objectData.name,
        objectData.subject,
        objectData.properties.map(::ObjectPropertyViewModel).toMutableList()
    )

    fun toObjectData() =
        ObjectData(
            id = id, name = name,
            subject = subject ?: error("Inconsistent State"),
            properties = properties.map { it.toObjectPropertyData() }, description = ""
        )
}

data class ObjectPropertyViewModel(
    val id: String? = null,
    var name: String? = null,
    var cardinality: PropertyCardinality? = null,
    var aspect: AspectData? = null,
    var values: MutableList<ObjectPropertyValueViewModel>? = null,
    var expanded: Boolean = true
) {
    constructor(objectPropertyData: ObjectPropertyData) : this(
        objectPropertyData.id,
        objectPropertyData.name,
        PropertyCardinality.valueOf(objectPropertyData.cardinality),
        objectPropertyData.aspect,
        objectPropertyData.values.map(::ObjectPropertyValueViewModel).toMutableList()
    )

    constructor(objectProperty: DetailedObjectPropertyResponse) : this(
        objectProperty.id,
        objectProperty.name,
        PropertyCardinality.valueOf(objectProperty.cardinality),
        objectProperty.aspect,
        objectProperty.values.map(::ObjectPropertyValueViewModel).toMutableList()
    )

    fun toObjectPropertyData() = ObjectPropertyData(
        id,
        name,
        cardinality?.name ?: error("Inconsistent State"),
        aspect ?: error("Inconsistent State"),
        values?.map {
            ObjectPropertyValueData(
                it.id,
                it.value,
                it.valueGroups.flatMap { group ->
                    group.values.map { groupValue ->
                        AspectPropertyValueData(
                            groupValue.id,
                            groupValue.value,
                            group.property.toExtendedPropertyData(),
                            groupValue.children.toAspectPropertyValueData()
                        )
                    }
                }
            )
        } ?: emptyList()
    )

}

data class ObjectPropertyValueViewModel(
    val id: String? = null,
    var value: String? = null,
    var expanded: Boolean = false,
    var valueGroups: MutableList<AspectPropertyValueGroupViewModel> = ArrayList()
) {

    constructor(objectPropertyValueData: ObjectPropertyValueData) : this(
        id = objectPropertyValueData.id,
        value = objectPropertyValueData.scalarValue,
        valueGroups = objectPropertyValueData.children.groupBy { it.aspectProperty }.toList().map {
            AspectPropertyValueGroupViewModel(
                AspectPropertyViewModel(it.first),
                it.second.map(::AspectPropertyValueViewModel).toMutableList()
            )
        }.toMutableList()
    )

    constructor(objectPropertyValue: RootValueResponse) : this(
        id = objectPropertyValue.id,
        value = objectPropertyValue.value.toData().let {
            when (it) {
                is ObjectValueData.IntegerValue -> it.value.toString()
                is ObjectValueData.NullValue -> null
                is ObjectValueData.DecimalValue -> it.valueRepr
                is ObjectValueData.BooleanValue -> it.value.toString()
                is ObjectValueData.StringValue -> it.value
                is ObjectValueData.Link -> when (it.value) {
                    is LinkValueData.DomainElement -> it.value.id
                    else -> TODO("Link type $it is not yet supported")
                }
                else -> TODO("Object value type $it is not yet supported")
            }
        },
        valueGroups = objectPropertyValue.children.groupBy { it.aspectProperty }.toList().map {
            AspectPropertyValueGroupViewModel(
                AspectPropertyViewModel(it.first),
                it.second.map(::AspectPropertyValueViewModel).toMutableList()
            )
        }.toMutableList()
    )

}

data class AspectPropertyValueGroupViewModel(
    val property: AspectPropertyViewModel,
    var values: MutableList<AspectPropertyValueViewModel> = ArrayList()
)

data class AspectPropertyValueViewModel(
    val id: String? = null,
    var value: String? = null,
    var expanded: Boolean = false,
    var children: MutableList<AspectPropertyValueGroupViewModel> = ArrayList()
) {
    constructor(propertyValue: AspectPropertyValueData) : this(
        id = propertyValue.id,
        value = propertyValue.scalarValue,
        children = propertyValue.children.groupBy { it.aspectProperty }.toList().map {
            AspectPropertyValueGroupViewModel(
                AspectPropertyViewModel(it.first),
                it.second.map(::AspectPropertyValueViewModel).toMutableList()
            )
        }.toMutableList()
    )

    constructor(propertyValue: ValueResponse) : this(
        id = propertyValue.id,
        value = propertyValue.value.toData().let {
            when (it) {
                is ObjectValueData.IntegerValue -> it.value.toString()
                is ObjectValueData.NullValue -> null
                is ObjectValueData.DecimalValue -> it.valueRepr
                is ObjectValueData.BooleanValue -> it.value.toString()
                is ObjectValueData.StringValue -> it.value
                is ObjectValueData.Link -> when (it.value) {
                    is LinkValueData.DomainElement -> it.value.id
                    else -> TODO("Link type $it is not yet supported")
                }
                else -> TODO("Object value type $it is not yet supported")
            }
        },
        children = propertyValue.children.groupBy { it.aspectProperty }.toList().map {
            AspectPropertyValueGroupViewModel(
                AspectPropertyViewModel(it.first),
                it.second.map(::AspectPropertyValueViewModel).toMutableList()
            )
        }.toMutableList()
    )
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

fun MutableList<AspectPropertyValueGroupViewModel>.toAspectPropertyValueData(): List<AspectPropertyValueData> =
    if (isEmpty()) {
        emptyList()
    } else {
        flatMap { group ->
            group.values.map { groupValue ->
                AspectPropertyValueData(
                    groupValue.id,
                    groupValue.value,
                    group.property.toExtendedPropertyData(),
                    groupValue.children.toAspectPropertyValueData()
                )
            }
        }
    }

// -----------------

data class ObjectLazyViewModel(
    val id: String,
    val name: String,
    val description: String?,
    val subjectId: String,
    val subjectName: String,
    val subjectDescription: String?,
    val objectPropertiesCount: Int,
    val objectProperties: List<ObjectPropertyViewModel2>? = null,
    var expanded: Boolean = false
)

data class ObjectPropertyViewModel2(
    val id: String,
    val name: String?,
    val cardinality: PropertyCardinality,
    val aspect: AspectData,
    val values: List<ObjectPropertyValueViewModel>,
    var expanded: Boolean = true
) {
    constructor(objectProperty: DetailedObjectPropertyResponse) : this(
        objectProperty.id,
        objectProperty.name,
        PropertyCardinality.valueOf(objectProperty.cardinality),
        objectProperty.aspect,
        objectProperty.values.map(::ObjectPropertyValueViewModel)
    )
}

fun List<ObjectGetResponse>.toLazyView(detailedObjects: Map<String, DetailedObjectResponse>) =
    this.map {
        ObjectLazyViewModel(
            it.id,
            it.name,
            it.description,
            it.subjectId,
            it.subjectName,
            it.subjectDescription,
            it.propertiesCount,
            detailedObjects[it.id]?.let { it.objectProperties.map { ObjectPropertyViewModel2(it) } }
        )
    }

fun List<ObjectLazyViewModel>.mergeDetails(detailedObjects: Map<String, DetailedObjectResponse>) =
    this.map {
        if (detailedObjects[it.id] == null) {
            ObjectLazyViewModel(
                it.id,
                it.name,
                it.description,
                it.subjectId,
                it.subjectName,
                it.subjectDescription,
                it.objectPropertiesCount,
                expanded = it.expanded
            )
        } else {
            val detailedObject = detailedObjects[it.id] ?: error("Should never happened")
            ObjectLazyViewModel(
                detailedObject.id,
                detailedObject.name,
                detailedObject.description,
                detailedObject.subjectId,
                detailedObject.subjectName,
                detailedObject.subjectDescription,
                detailedObject.propertiesCount,
                detailedObject.objectProperties.map { ObjectPropertyViewModel2(it) },
                it.expanded
            )
        }
    }

