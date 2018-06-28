package com.infowings.catalog.objects.edit.tree

//import com.infowings.catalog.components.additem.addPropertyButton
//import com.infowings.catalog.components.delete.deleteButtonComponent
//import com.infowings.catalog.components.submit.submitButtonComponent
//import com.infowings.catalog.objects.ObjectEditModel
//import com.infowings.catalog.objects.edit.tree.inputs.name
//import com.infowings.catalog.objects.edit.tree.inputs.objectSubject
//import react.*
//import react.dom.div
//
//class ObjectTreeRoot : RComponent<ObjectTreeRoot.Props, RState>() {
//
//    override fun RBuilder.render() {
//        div(classes = "object-tree-edit__object") {
//            name(
//                value = props.objectTreeEdit.name ?: "",
//                onEdit = props.onStartEdit,
//                onCancel = {
//                    props.onUpdate {
//                        name = it
//                    }
//                },
//                onChange = {
//                    props.onUpdate {
//                        name = it
//                    }
//                }
//            )
//            +"(Subject: "
//            objectSubject( // TODO: should not be available for editing, only for creating.
//                value = props.objectTreeEdit.subject,
//                onOpen = props.onStartEdit,
//                onSelect = {
//                    props.onUpdate {
//                        subject = it
//                    }
//                }
//            )
//            +" )"
//            addPropertyButton(onClick = props.onAddProperty)
//            deleteButtonComponent(
//                onDeleteClick = { TODO("Delete is not available yet") },
//                entityName = "object"
//            )
//            if (props.isSelected) { // TODO: Don't show when data is not valid
//                props.onSubmit?.let {
//                    submitButtonComponent(
//                        onSubmit = it
//                    )
//                }
//            }
//        }
//    }
//
//    interface Props : RProps {
//        var objectTreeEdit: ObjectEditModel
//        var onStartEdit: () -> Unit
//        var onUpdate: (ObjectEditModel.() -> Unit) -> Unit
//        var onSubmit: (() -> Unit)?
//        var onAddProperty: () -> Unit
//        var isSelected: Boolean
//    }
//}
//
//fun RBuilder.objectTreeRoot(block: RHandler<ObjectTreeRoot.Props>) = child(ObjectTreeRoot::class, block)
//
