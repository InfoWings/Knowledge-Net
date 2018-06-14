package com.infowings.catalog.objects.treeview

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.PropertyCardinality
import com.infowings.catalog.objects.ObjectPropertyViewModel
import com.infowings.catalog.objects.treeview.inputs.propertyAspect
import com.infowings.catalog.objects.treeview.inputs.propertyCardinality
import com.infowings.catalog.objects.treeview.inputs.propertyName
import com.infowings.catalog.objects.treeview.inputs.propertyValue
import com.infowings.catalog.objects.treeview.utils.addValue
import com.infowings.catalog.objects.treeview.utils.propertyAspectTypeInfo
import com.infowings.catalog.objects.treeview.utils.propertyAspectTypePrompt
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
        if (property.aspect != null) {
            when {
                property.cardinality == PropertyCardinality.ONE -> {
                    propertyAspectTypePrompt(property.aspect ?: error("Memory Model inconsistency"))
                    propertyValue(
                        value = property.values?.firstOrNull()?.value ?: "",
                        baseType = property.aspect?.baseType ?: error("Memory Model inconsistency"),
                        onEdit = onEdit,
                        onChange = {
                            onUpdate {
                                val values = values
                                        ?: error("When editing object property value, its values List should be initialized")
                                if (values.isEmpty()) {
                                    addValue(aspectsMap, it)
                                } else {
                                    values[0].value = it
                                }
                            }
                        }
                    )
                }
                property.cardinality == PropertyCardinality.INFINITY ->
                    propertyAspectTypeInfo(property.aspect ?: error("Memory Model inconsistency"))
            }
        }
    }

fun ObjectPropertyViewModel.updateValuesIfPossible(aspectsMap: Map<String, AspectData>) {
    when {
        aspect != null && cardinality == PropertyCardinality.ZERO && values == null -> {
            values = ArrayList()
            addValue(aspectsMap)
        }
        aspect != null && cardinality != null && values == null ->
            values = ArrayList()
    }
}
