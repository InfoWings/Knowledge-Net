package com.infowings.catalog.objects.treeview

import com.infowings.catalog.components.additem.addPropertyButton
import com.infowings.catalog.components.delete.deleteButtonComponent
import com.infowings.catalog.components.submit.submitButtonComponent
import com.infowings.catalog.objects.ObjTreeView
import com.infowings.catalog.objects.treeview.inputs.name
import com.infowings.catalog.objects.treeview.inputs.objectSubject
import react.*
import react.dom.div

class ObjectTreeRoot : RComponent<ObjectTreeRoot.Props, RState>() {

    override fun RBuilder.render() {
        div(classes = "object-tree-view__object") {
            name(
                value = props.objectTreeView.name ?: "",
                onEdit = props.onStartEdit,
                onCancel = {
                    props.onUpdate {
                        name = it
                    }
                },
                onChange = {
                    props.onUpdate {
                        name = it
                    }
                }
            )
            +"(Subject: "
            objectSubject( // TODO: should not be available for editing, only for creating.
                value = props.objectTreeView.subject,
                onOpen = props.onStartEdit,
                onSelect = {
                    props.onUpdate {
                        subject = it
                    }
                }
            )
            +" )"
            addPropertyButton(onClick = props.onAddProperty)
            deleteButtonComponent(
                onDeleteClick = { TODO("Delete is not available yet") },
                entityName = "object"
            )
            if (props.isSelected) { // TODO: Don't show when data is not valid
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
        var onAddProperty: () -> Unit
        var isSelected: Boolean
    }
}

fun RBuilder.objectTreeRoot(block: RHandler<ObjectTreeRoot.Props>) = child(ObjectTreeRoot::class, block)

