//package com.infowings.catalog.objects.edit.tree.utils
//
//import com.infowings.catalog.common.AspectData
//import com.infowings.catalog.common.PropertyCardinality
//import com.infowings.catalog.objects.*
//
//fun AspectPropertyValueGroupEditModel.addValue(aspectsMap: Map<String, AspectData>, value: String? = null) {
//    val values = this.values
//    val property = this.property
//
//    when (property.cardinality) {
//        PropertyCardinality.ZERO -> if (values.isEmpty() && value == null) {
//            values.add(
//                AspectPropertyValueEditModel().apply { constructAspectTree(property, aspectsMap) }
//            )
//        } else error("Preconditions are not satisfied: ObjectPropertyEditModel.values is not empty with cardinality.ZERO")
//
//        PropertyCardinality.ONE -> if (values.isEmpty()) {
//            values.add(
//                AspectPropertyValueEditModel(
//                    value = value // TODO: What if value == null?
//                ).apply { constructAspectTree(property, aspectsMap) }
//            )
//        } else error("Preconditions are not satisfied: ObjectPropertyEditModel.values is not empty with cardinality.ONE")
//
//        PropertyCardinality.INFINITY -> values.add(
//            AspectPropertyValueEditModel(
//                value = value ?: error("When cardinality == Cardinality.INFINITY, value should not be null"),
//                expanded = true
//            ).apply { constructAspectTree(property, aspectsMap) }
//        )
//    }
//}
//
//fun AspectPropertyValueEditModel.constructAspectTree(
//    property: AspectPropertyViewModel,
//    aspectsMap: Map<String, AspectData>
//) {
//    val aspectData = aspectsMap[property.aspectId]
//            ?: TODO("Property ${property.propertyId} (${property.roleName}) references aspect that does not exist in context")
//
//    if (children.isEmpty()) {
//        aspectData.properties.forEach { propertyData ->
//            val associatedAspect = aspectsMap[propertyData.aspectId]
//                    ?: TODO("Property ${propertyData.id} (${propertyData.name}) references aspect that does not exist in context")
//
//            this.children.add(
//                AspectPropertyValueGroupEditModel(
//                    property = AspectPropertyViewModel(
//                        propertyId = propertyData.id,
//                        aspectId = propertyData.aspectId,
//                        cardinality = PropertyCardinality.valueOf(propertyData.cardinality),
//                        roleName = propertyData.name,
//                        aspectName = associatedAspect.name ?: error("Valid aspect should have non-null name"),
//                        measure = associatedAspect.measure,
//                        baseType = associatedAspect.baseType ?: error("Valid aspect should have non-null base type"),
//                        domain = associatedAspect.domain ?: error("Valid aspect should have non-null domain"),
//                        refBookName = associatedAspect.refBookName
//                    )
//                ).apply { constructSubtreeIfCardinalityGroup(aspectsMap) }
//            )
//        }
//    }
//}
//
//fun ObjectPropertyEditModel.addValue(aspectsMap: Map<String, AspectData>, value: String? = null) {
//    val values = this.values ?: error("Preconditions are not satisfied: ObjectPropertyEditModel.values == null")
//    val aspect = this.aspect ?: error("Preconditions are not satisfied: ObjectPropertyEditModel.aspect == null")
//    val cardinality =
//        this.cardinality ?: error("Preconditions are not satisfied: ObjectPropertyEditModel.cardinality == null")
//
//    when (cardinality) {
//        PropertyCardinality.ZERO -> if (values.isEmpty() && value == null) {
//            values.add(
//                ObjectPropertyValueEditModel().apply { constructAspectTree(aspect, aspectsMap) }
//            )
//        } else error("Preconditions are not satisfied: ObjectPropertyEditModel.values is not empty with cardinality.ZERO")
//
//        PropertyCardinality.ONE -> if (values.isEmpty()) {
//            values.add(
//                ObjectPropertyValueEditModel(
//                    value = value // TODO: What if value == null?
//                ).apply { constructAspectTree(aspect, aspectsMap) }
//            )
//        } else error("Preconditions are not satisfied: ObjectPropertyEditModel.values is not empty with cardinality.ONE")
//
//        PropertyCardinality.INFINITY -> values.add(
//            ObjectPropertyValueEditModel(
//                value = value ?: error("When cardinality == Cardinality.INFINITY, value should not be null"),
//                expanded = true
//            ).apply { constructAspectTree(aspect, aspectsMap) }
//        )
//    }
//}
//
//fun ObjectPropertyValueEditModel.constructAspectTree(
//    aspectData: AspectData,
//    aspectsMap: Map<String, AspectData>
//) {
//    if (valueGroups.isEmpty()) {
//        aspectData.properties.forEach { property ->
//            val associatedAspect = aspectsMap[property.aspectId]
//                    ?: TODO("property ${property.id} (${property.name}) references aspect that does not exist in context")
//
//            this.valueGroups.add(
//                AspectPropertyValueGroupEditModel(
//                    property = AspectPropertyViewModel(
//                        propertyId = property.id,
//                        aspectId = property.aspectId,
//                        cardinality = PropertyCardinality.valueOf(property.cardinality),
//                        roleName = property.name,
//                        aspectName = associatedAspect.name ?: error("Valid aspect should have non-null name"),
//                        measure = associatedAspect.measure,
//                        baseType = associatedAspect.baseType ?: error("Valid aspect should have non-null base type"),
//                        domain = associatedAspect.domain ?: error("Valid aspect should have non-null domain"),
//                        refBookName = associatedAspect.refBookName
//                    )
//                ).apply { constructSubtreeIfCardinalityGroup(aspectsMap) }
//            )
//        }
//    }
//}
//
//fun AspectPropertyValueGroupEditModel.constructSubtreeIfCardinalityGroup(aspectsMap: Map<String, AspectData>) {
//    if (property.cardinality == PropertyCardinality.ZERO) {
//        addValue(aspectsMap)
//    }
//}
//
