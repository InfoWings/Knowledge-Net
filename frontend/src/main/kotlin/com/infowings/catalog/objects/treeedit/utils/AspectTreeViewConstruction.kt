package com.infowings.catalog.objects.treeedit.utils

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.PropertyCardinality
import com.infowings.catalog.objects.*

fun AspectPropertyValueGroupViewModel.addValue(aspectsMap: Map<String, AspectData>, value: String? = null) {
    val values = this.values
    val property = this.property

    when (property.cardinality) {
        PropertyCardinality.ZERO -> if (values.isEmpty() && value == null) {
            values.add(
                AspectPropertyValueViewModel().apply { constructAspectTree(property, aspectsMap) }
            )
        } else error("Preconditions are not satisfied: ObjectPropertyViewModel.values is not empty with cardinality.ZERO")

        PropertyCardinality.ONE -> if (values.isEmpty()) {
            values.add(
                AspectPropertyValueViewModel(
                    value = value // TODO: What if value == null?
                ).apply { constructAspectTree(property, aspectsMap) }
            )
        } else error("Preconditions are not satisfied: ObjectPropertyViewModel.values is not empty with cardinality.ONE")

        PropertyCardinality.INFINITY -> values.add(
            AspectPropertyValueViewModel(
                value = value ?: error("When cardinality == Cardinality.INFINITY, value should not be null"),
                expanded = true
            ).apply { constructAspectTree(property, aspectsMap) }
        )
    }
}

fun AspectPropertyValueViewModel.constructAspectTree(
    property: AspectPropertyViewModel,
    aspectsMap: Map<String, AspectData>
) {
    val aspectData = aspectsMap[property.aspectId]
            ?: TODO("Property ${property.propertyId} (${property.roleName}) references aspect that does not exist in context")

    if (children.isEmpty()) {
        aspectData.properties.forEach { propertyData ->
            val associatedAspect = aspectsMap[propertyData.aspectId]
                    ?: TODO("Property ${propertyData.id} (${propertyData.name}) references aspect that does not exist in context")

            this.children.add(
                AspectPropertyValueGroupViewModel(
                    property = AspectPropertyViewModel(
                        propertyId = propertyData.id,
                        aspectId = propertyData.aspectId,
                        cardinality = PropertyCardinality.valueOf(propertyData.cardinality),
                        roleName = propertyData.name,
                        aspectName = associatedAspect.name ?: error("Valid aspect should have non-null name"),
                        measure = associatedAspect.measure,
                        baseType = associatedAspect.baseType ?: error("Valid aspect should have non-null base type"),
                        domain = associatedAspect.domain ?: error("Valid aspect should have non-null domain"),
                        refBookName = associatedAspect.refBookName
                    )
                ).apply { constructSubtreeIfCardinalityGroup(aspectsMap) }
            )
        }
    }
}

fun ObjectPropertyViewModel.addValue(aspectsMap: Map<String, AspectData>, value: String? = null) {
    val values = this.values ?: error("Preconditions are not satisfied: ObjectPropertyViewModel.values == null")
    val aspect = this.aspect ?: error("Preconditions are not satisfied: ObjectPropertyViewModel.aspect == null")
    val cardinality =
        this.cardinality ?: error("Preconditions are not satisfied: ObjectPropertyViewModel.cardinality == null")

    when (cardinality) {
        PropertyCardinality.ZERO -> if (values.isEmpty() && value == null) {
            values.add(
                ObjectPropertyValueViewModel().apply { constructAspectTree(aspect, aspectsMap) }
            )
        } else error("Preconditions are not satisfied: ObjectPropertyViewModel.values is not empty with cardinality.ZERO")

        PropertyCardinality.ONE -> if (values.isEmpty()) {
            values.add(
                ObjectPropertyValueViewModel(
                    value = value // TODO: What if value == null?
                ).apply { constructAspectTree(aspect, aspectsMap) }
            )
        } else error("Preconditions are not satisfied: ObjectPropertyViewModel.values is not empty with cardinality.ONE")

        PropertyCardinality.INFINITY -> values.add(
            ObjectPropertyValueViewModel(
                value = value ?: error("When cardinality == Cardinality.INFINITY, value should not be null"),
                expanded = true
            ).apply { constructAspectTree(aspect, aspectsMap) }
        )
    }
}

fun ObjectPropertyValueViewModel.constructAspectTree(
    aspectData: AspectData,
    aspectsMap: Map<String, AspectData>
) {
    if (valueGroups.isEmpty()) {
        aspectData.properties.forEach { property ->
            val associatedAspect = aspectsMap[property.aspectId]
                    ?: TODO("property ${property.id} (${property.name}) references aspect that does not exist in context")

            this.valueGroups.add(
                AspectPropertyValueGroupViewModel(
                    property = AspectPropertyViewModel(
                        propertyId = property.id,
                        aspectId = property.aspectId,
                        cardinality = PropertyCardinality.valueOf(property.cardinality),
                        roleName = property.name,
                        aspectName = associatedAspect.name ?: error("Valid aspect should have non-null name"),
                        measure = associatedAspect.measure,
                        baseType = associatedAspect.baseType ?: error("Valid aspect should have non-null base type"),
                        domain = associatedAspect.domain ?: error("Valid aspect should have non-null domain"),
                        refBookName = associatedAspect.refBookName
                    )
                ).apply { constructSubtreeIfCardinalityGroup(aspectsMap) }
            )
        }
    }
}

fun AspectPropertyValueGroupViewModel.constructSubtreeIfCardinalityGroup(aspectsMap: Map<String, AspectData>) {
    if (property.cardinality == PropertyCardinality.ZERO) {
        addValue(aspectsMap)
    }
}

