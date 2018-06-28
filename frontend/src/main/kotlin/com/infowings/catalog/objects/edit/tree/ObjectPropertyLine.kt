//package com.infowings.catalog.objects.edit.tree
//
//import com.infowings.catalog.common.AspectData
//import com.infowings.catalog.common.PropertyCardinality
//import com.infowings.catalog.objects.ObjectPropertyEditModel
//import com.infowings.catalog.objects.edit.tree.inputs.propertyAspect
//import com.infowings.catalog.objects.edit.tree.inputs.propertyCardinality
//import com.infowings.catalog.objects.edit.tree.inputs.propertyName
//import com.infowings.catalog.objects.edit.tree.inputs.propertyValue
//import com.infowings.catalog.objects.edit.tree.utils.addValue
//import com.infowings.catalog.objects.edit.tree.utils.propertyAspectTypeInfo
//import com.infowings.catalog.objects.edit.tree.utils.propertyAspectTypePrompt
//import react.RBuilder
//import react.dom.div
//
//fun RBuilder.objectPropertyLine(
//    property: ObjectPropertyEditModel,
//    aspectsMap: Map<String, AspectData>,
//    onEdit: () -> Unit,
//    onUpdate: (ObjectPropertyEditModel.() -> Unit) -> Unit
//) =
//    div(classes = "object-tree-edit__object-property") {
//        propertyName(
//            value = property.name ?: "",
//            onEdit = onEdit,
//            onCancel = {
//                onUpdate {
//                    name = it
//                }
//            },
//            onChange = {
//                onUpdate {
//                    name = it
//                }
//            }
//        )
//        propertyAspect(
//            value = property.aspect,
//            onSelect = {
//                onUpdate {
//                    aspect = it
//                    updateValuesIfPossible(aspectsMap)
//                }
//            },
//            onOpen = onEdit
//        )
//        propertyCardinality(
//            value = property.cardinality,
//            onChange = {
//                onUpdate {
//                    cardinality = it
//                    updateValuesIfPossible(aspectsMap)
//                }
//            }
//        )
//        val aspect = property.aspect
//        if (aspect != null) {
//            when {
//                property.cardinality == PropertyCardinality.ONE -> {
//                    propertyAspectTypePrompt(aspect)
//                    propertyValue(
//                        value = property.values?.firstOrNull()?.value ?: "",
//                        baseType = aspect.baseType ?: error("Aspect must have base type"),
//                        onEdit = onEdit,
//                        onChange = {
//                            onUpdate {
//                                val values = values
//                                        ?: error("When editing object property value, its values List should be initialized")
//                                if (values.isEmpty()) {
//                                    addValue(aspectsMap, it)
//                                } else {
//                                    values[0].value = it
//                                }
//                            }
//                        },
//                        aspectRefBookId = if (aspect.refBookName == null) null else aspect.id
//                    )
//                }
//                property.cardinality == PropertyCardinality.INFINITY ->
//                    propertyAspectTypeInfo(property.aspect ?: error("Memory Model inconsistency"))
//            }
//        }
//    }
//
//fun ObjectPropertyEditModel.updateValuesIfPossible(aspectsMap: Map<String, AspectData>) {
//    when {
//        aspect != null && cardinality == PropertyCardinality.ZERO && values == null -> {
//            values = ArrayList()
//            addValue(aspectsMap)
//        }
//        aspect != null && cardinality != null && values == null ->
//            values = ArrayList()
//    }
//}
