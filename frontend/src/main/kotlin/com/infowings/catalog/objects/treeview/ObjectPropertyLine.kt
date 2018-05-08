package com.infowings.catalog.objects.treeview

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.objects.*
import com.infowings.catalog.objects.treeview.inputs.propertyAspect
import com.infowings.catalog.objects.treeview.inputs.propertyCardinality
import com.infowings.catalog.objects.treeview.inputs.propertyName
import com.infowings.catalog.objects.treeview.inputs.propertyValue
import com.infowings.catalog.objects.treeview.utils.propertyAspectTypeInfo
import react.RBuilder
import react.dom.div

fun RBuilder.objectPropertyLine(
    property: ObjectPropertyViewModel,
    aspectsMap: Map<String, AspectData>,
    onEdit: () -> Unit,
    onUpdate: (ObjectPropertyViewModel.() -> Unit) -> Unit
) =
    div(classes = "object-tree-view__object-property") {
        propertyName(
            value = property.name ?: "",
            onEdit = onEdit,
            onCancel = {
                onUpdate {
                    name = it
                }
            },
            onChange = {
                onUpdate {
                    name = it
                }
            }
        )
        propertyAspect(
            value = property.aspect,
            onSelect = {
                onUpdate {
                    aspect = it
                    updateValuesIfPossible(aspectsMap)
                }
            },
            onOpen = onEdit
        )
        propertyCardinality(
            value = property.cardinality,
            onChange = {
                onUpdate {
                    cardinality = it
                    updateValuesIfPossible(aspectsMap)
                }
            }
        )
        propertyAspectTypeInfo(property.aspect)
        if (property.aspect != null && property.cardinality == Cardinality.ONE) {
            propertyValue(
                value = property.values?.firstOrNull()?.value ?: "",
                onEdit = {
                    onEdit()
                    val values = property.values
                    if (values == null) {
                        onUpdate {
                            this.values = ArrayList<ObjectPropertyValueViewModel>().apply {
                                add(ObjectPropertyValueViewModel(expanded = true))
                            }
                            addGroupsToFirstValue(aspectsMap)
                        }
                    } else if (values.isEmpty()) {
                        onUpdate {
                            this.values?.add(ObjectPropertyValueViewModel(expanded = true))
                                    ?: error("Inconsistent State")
                            addGroupsToFirstValue(aspectsMap)
                        }
                    }
                },
                onChange = {
                    onUpdate {
                        (values ?: error("Inconsistent State"))[0].value = it
                    }
                },
                onCancel = {
                    onUpdate {
                        (values ?: error("Inconsistent State"))[0].value = it
                    }
                }
            )
        }
    }

// TODO: Create whole subtree (skip value if GROUP)

fun ObjectPropertyViewModel.updateValuesIfPossible(aspectsMap: Map<String, AspectData>) {
    if (aspect != null && cardinality == Cardinality.ZERO && values == null) {
        values = ArrayList<ObjectPropertyValueViewModel>().apply {
            add(ObjectPropertyValueViewModel().apply {
                expanded = true
                val aspectProperties = aspect?.properties ?: error("Inconsistent Memory model behaviour")
                aspectProperties.forEach { property ->
                    val associatedAspect = aspectsMap[property.aspectId] ?: error("Inconsistent State")
                    valueGroups.add(
                        AspectPropertyValueGroupViewModel(
                            property = AspectPropertyViewModel(
                                propertyId = property.id,
                                aspectId = property.aspectId,
                                cardinality = Cardinality.valueOf(property.cardinality),
                                roleName = property.name,
                                aspectName = associatedAspect.name ?: error("Inconsistent State"),
                                baseType = associatedAspect.baseType ?: error("Inconsistent State"),
                                domain = associatedAspect.domain ?: error("Inconsistent State")
                            )
                        )
                    )
                }
            })
        }
    } else if (aspect != null && cardinality == Cardinality.INFINITY && values == null) {
        values = ArrayList()
    }
}

fun ObjectPropertyViewModel.addGroupsToFirstValue(aspectsMap: Map<String, AspectData>) {
    val valueGroups = values?.get(0)?.valueGroups ?: error("Precondition: values should be instantiated")
    val aspectProperties = aspect?.properties ?: error("Aspect should be selected already")
    aspectProperties.forEach { property ->
        val associatedAspect = aspectsMap[property.aspectId] ?: error("Inconsistent State")
        valueGroups.add(
            AspectPropertyValueGroupViewModel(
                property = AspectPropertyViewModel(
                    propertyId = property.id,
                    aspectId = property.aspectId,
                    cardinality = Cardinality.valueOf(property.cardinality),
                    roleName = property.name,
                    aspectName = associatedAspect.name ?: error("Inconsistent State"),
                    baseType = associatedAspect.baseType ?: error("Inconsistent State"),
                    domain = associatedAspect.domain ?: error("Inconsistent State")
                )
            )
        )
    }
}

