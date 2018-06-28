package com.infowings.catalog.objects

//data class ObjectEditModel(
//    val id: String?,
//    var name: String?,
//    var subject: SubjectData?,
//    var properties: MutableList<ObjectPropertyEditModel>,
//    var expanded: Boolean = false
//) {
//    constructor(objectData: ObjectData) : this(
//        objectData.id,
//        objectData.name,
//        objectData.subject,
//        objectData.properties.map(::ObjectPropertyEditModel).toMutableList()
//    )
//
//    fun toObjectData() =
//        ObjectData(
//            id = id, name = name,
//            subject = subject ?: error("Inconsistent State"),
//            properties = properties.map { it.toObjectPropertyData() }, description = ""
//        )
//}
//
//data class ObjectPropertyEditModel(
//    val id: String? = null,
//    var name: String? = null,
//    var cardinality: PropertyCardinality? = null,
//    var aspect: AspectData? = null,
//    var values: MutableList<ObjectPropertyValueEditModel>? = null,
//    var expanded: Boolean = true
//) {
//    constructor(objectPropertyData: ObjectPropertyData) : this(
//        objectPropertyData.id,
//        objectPropertyData.name,
//        PropertyCardinality.valueOf(objectPropertyData.cardinality),
//        objectPropertyData.aspect,
//        objectPropertyData.values.map(::ObjectPropertyValueEditModel).toMutableList()
//    )
//
//    fun toObjectPropertyData() = ObjectPropertyData(
//        id,
//        name,
//        cardinality?.name ?: error("Inconsistent State"),
//        aspect ?: error("Inconsistent State"),
//        values?.map {
//            ObjectPropertyValueData(
//                it.id,
//                it.value,
//                it.valueGroups.flatMap { group ->
//                    group.values.map { groupValue ->
//                        AspectPropertyValueData(
//                            groupValue.id,
//                            groupValue.value,
//                            group.property.toExtendedPropertyData(),
//                            groupValue.children.toAspectPropertyValueData()
//                        )
//                    }
//                }
//            )
//        } ?: emptyList()
//    )
//
//}
//
//data class ObjectPropertyValueEditModel(
//    val id: String? = null,
//    var value: String? = null,
//    var expanded: Boolean = false,
//    var valueGroups: MutableList<AspectPropertyValueGroupEditModel> = ArrayList()
//) {
//
//    constructor(objectPropertyValueData: ObjectPropertyValueData) : this(
//        id = objectPropertyValueData.id,
//        value = objectPropertyValueData.scalarValue,
//        valueGroups = objectPropertyValueData.children.groupBy { it.aspectProperty }.toList().map {
//            AspectPropertyValueGroupEditModel(
//                AspectPropertyViewModel(it.first),
//                it.second.map(::AspectPropertyValueEditModel).toMutableList()
//            )
//        }.toMutableList()
//    )
//
//}
//
//data class AspectPropertyValueGroupEditModel(
//    val property: AspectPropertyViewModel,
//    var values: MutableList<AspectPropertyValueEditModel> = ArrayList()
//)
//
//data class AspectPropertyValueEditModel(
//    val id: String? = null,
//    var value: String? = null,
//    var expanded: Boolean = false,
//    var children: MutableList<AspectPropertyValueGroupEditModel> = ArrayList()
//) {
//    constructor(propertyValue: AspectPropertyValueData) : this(
//        id = propertyValue.id,
//        value = propertyValue.scalarValue,
//        children = propertyValue.children.groupBy { it.aspectProperty }.toList().map {
//            AspectPropertyValueGroupEditModel(
//                AspectPropertyViewModel(it.first),
//                it.second.map(::AspectPropertyValueEditModel).toMutableList()
//            )
//        }.toMutableList()
//    )
//
//}
//
//data class AspectPropertyViewModel(
//    val propertyId: String,
//    val aspectId: String,
//    val cardinality: PropertyCardinality,
//    val roleName: String?,
//    val aspectName: String,
//    val measure: String?,
//    val baseType: String,
//    val domain: String,
//    val refBookName: String?
//) {
//    constructor(aspectPropertyData: AspectPropertyDataExtended) : this(
//        aspectPropertyData.id,
//        aspectPropertyData.aspectId,
//        PropertyCardinality.valueOf(aspectPropertyData.cardinality),
//        aspectPropertyData.name,
//        aspectPropertyData.aspectName,
//        aspectPropertyData.aspectMeasure,
//        aspectPropertyData.aspectBaseType,
//        aspectPropertyData.aspectDomain,
//        aspectPropertyData.refBookName
//    )
//
//    fun toExtendedPropertyData() = AspectPropertyDataExtended(
//        id = propertyId,
//        name = roleName ?: "",
//        cardinality = cardinality.name,
//        aspectId = aspectId,
//        aspectName = aspectName,
//        aspectMeasure = measure,
//        aspectDomain = domain,
//        aspectBaseType = baseType,
//        refBookName = refBookName
//    )
//}
//
//fun MutableList<AspectPropertyValueGroupEditModel>.toAspectPropertyValueData(): List<AspectPropertyValueData> =
//    if (isEmpty()) {
//        emptyList()
//    } else {
//        flatMap { group ->
//            group.values.map { groupValue ->
//                AspectPropertyValueData(
//                    groupValue.id,
//                    groupValue.value,
//                    group.property.toExtendedPropertyData(),
//                    groupValue.children.toAspectPropertyValueData()
//                )
//            }
//        }
//    }

