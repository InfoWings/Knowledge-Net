package com.infowings.catalog.objects.treeview

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.components.treeview.controlledTreeNode
import com.infowings.catalog.objects.Cardinality
import com.infowings.catalog.objects.ObjectPropertyValueViewModel
import com.infowings.catalog.objects.ObjectPropertyViewModel
import react.RBuilder
import react.buildElement

fun RBuilder.objectPropertyNode(
    property: ObjectPropertyViewModel,
    aspectsMap: Map<String, AspectData>,
    onEdit: () -> Unit,
    onUpdate: (ObjectPropertyViewModel.() -> Unit) -> Unit,
    onUpdateWithoutSelect: (ObjectPropertyViewModel.() -> Unit) -> Unit
) =
    controlledTreeNode {
        attrs {
            className = "object-tree-view__property"
            expanded = property.expanded
            onExpanded = {
                onUpdateWithoutSelect {
                    expanded = it
                }
            }
            treeNodeContent = buildElement {
                objectPropertyLine(
                    property = property,
                    aspectsMap = aspectsMap,
                    onEdit = onEdit,
                    onUpdate = onUpdate
                )
            }!!
        }
        when {
            property.cardinality == Cardinality.ZERO && property.aspect != null -> aspectPropertyValues(
                groups = property.values?.get(0)?.valueGroups
                        ?: error("ObjectProperty with Cardinality.ZERO and assigned aspect should have one fake value"),
                aspectsMap = aspectsMap,
                onEdit = onEdit,
                onUpdate = { index, block ->
                    onUpdate {
                        values?.get(0)?.valueGroups?.get(index)?.block()
                                ?: error("ObjectProperty with Cardinality.ZERO and assigned aspect should have one fake value")
                    }
                },
                onNonSelectedUpdate = { index, block ->
                    onUpdateWithoutSelect {
                        values?.get(0)?.valueGroups?.get(index)?.block()
                                ?: error("ObjectProperty with Cardinality.ZERO and assigned aspect should have one fake value")
                    }
                }
            )
            property.cardinality == Cardinality.ONE && property.aspect != null && property.values?.firstOrNull() != null -> aspectPropertyValues(
                groups = property.values?.get(0)?.valueGroups ?: error("Memory Model inconsistency"),
                aspectsMap = aspectsMap,
                onEdit = onEdit,
                onUpdate = { index, block ->
                    onUpdate {
                        values?.get(0)?.valueGroups?.get(index)?.block() ?: error("Memory Model inconsistency")
                    }
                },
                onNonSelectedUpdate = { index, block ->
                    onUpdateWithoutSelect {
                        values?.get(0)?.valueGroups?.get(index)?.block() ?: error("Memory Model inconsistency")
                    }
                }
            )
            property.cardinality == Cardinality.INFINITY && property.aspect != null -> objectPropertyValues(
                values = property.values ?: error("Memory Model inconsistency"),
                aspectsMap = aspectsMap,
                aspect = property.aspect ?: error("Memory Model inconsistency"),
                onEdit = onEdit,
                onUpdate = { index, block ->
                    onUpdate {
                        values?.get(index)?.block() ?: error("Inconsistent State")
                    }
                },
                onNonSelectedUpdate = { index, block ->
                    onUpdateWithoutSelect {
                        values?.get(index)?.block() ?: error("Inconsistent State")
                    }
                },
                onAddValue = {
                    onEdit()
                    onUpdate {
                        values?.add(ObjectPropertyValueViewModel())
                    }
                }
            )
        }
    }

