package com.infowings.catalog.objects.treeview

import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.components.additem.addPropertyButton
import com.infowings.catalog.components.delete.deleteButtonComponent
import com.infowings.catalog.objects.ObjectLazyViewModel
import com.infowings.catalog.objects.treeview.inputs.name
import com.infowings.catalog.objects.treeview.inputs.objectSubject
import react.*
import react.dom.div

class ObjectLazyTreeRoot : RComponent<ObjectLazyTreeRoot.Props, RState>() {

    override fun RBuilder.render() {
        div(classes = "object-tree-view__object") {
            name(
                value = props.objectTreeView.name,
                onEdit = {},
                onCancel = {},
                onChange = {},
                disabled = true
            )
            +"(Subject: "
            objectSubject(
                value = SubjectData(
                    id = props.objectTreeView.subjectId,
                    name = props.objectTreeView.subjectName,
                    description = props.objectTreeView.subjectDescription
                ),
                onOpen = {},
                onSelect = {},
                disabled = true
            )
            +" )"
            addPropertyButton(onClick = {})
            deleteButtonComponent(
                onDeleteClick = { TODO("Delete is not available yet") },
                entityName = "object"
            )
        }
    }

    interface Props : RProps {
        var objectTreeView: ObjectLazyViewModel
    }
}

fun RBuilder.objectLazyTreeRoot(block: RHandler<ObjectLazyTreeRoot.Props>) = child(ObjectLazyTreeRoot::class, block)

