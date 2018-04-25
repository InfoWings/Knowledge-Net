package com.infowings.catalog.objects.treeview

import com.infowings.catalog.components.delete.deleteButtonComponent
import com.infowings.catalog.components.submit.submitButtonComponent
import com.infowings.catalog.objects.ObjTreeView
import com.infowings.catalog.wrappers.blueprint.EditableText
import react.*
import react.dom.div

class ObjectTreeRoot : RComponent<ObjectTreeRoot.Props, RState>() {

    override fun RBuilder.render() {
        div(classes = "object-tree-view__root") {
            EditableText {
                attrs {
                    className = "object-input-name"
                    placeholder = "Enter name"
                    value = props.objectTreeView.name ?: ""
                    onEdit = props.onStartEdit
                    onCancel = {
                        props.onUpdate {
                            name = it
                        }
                    }
                    onChange = {
                        props.onUpdate {
                            name = it
                        }
                    }
                }
            }
            +"(Subject: "
            EditableText {
                attrs {
                    className = "object-input-subject"
                    placeholder = "Enter subject"
                    value = props.objectTreeView.subject ?: ""
                    onEdit = props.onStartEdit
                    onCancel = {
                        props.onUpdate {
                            subject = it
                        }
                    }
                    onChange = {
                        props.onUpdate {
                            subject = it
                        }
                    }
                }
            }
            +" )"
            deleteButtonComponent(
                onDeleteClick = { TODO("Delete is not available yet") },
                entityName = "object"
            )
            if (props.isSelected) {
                props.onSubmit?.let {
                    submitButtonComponent(
                        onSubmit = it
                    )
                }
            }
        }
    }

    interface Props : RProps {
        var objectTreeView: ObjTreeView
        var onStartEdit: () -> Unit
        var onUpdate: (ObjTreeView.() -> Unit) -> Unit
        var onSubmit: (() -> Unit)?
        var isSelected: Boolean
    }
}

fun RBuilder.objectTreeRoot(block: RHandler<ObjectTreeRoot.Props>) = child(ObjectTreeRoot::class, block)

