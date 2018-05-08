package com.infowings.catalog.objects.treeview

import com.infowings.catalog.objects.ObjectTreeViewModelConsumerProps
import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.Intent
import kotlinext.js.require
import react.RBuilder
import react.RComponent
import react.RHandler
import react.RState
import react.dom.div

class ObjectTreeView : RComponent<ObjectTreeViewModelConsumerProps, RState>() {

    companion object {
        init {
            require("styles/object-tree-view.scss")
        }
    }

    override fun RBuilder.render() {
        div(classes = "object-tree-view") {
            props.objectForest.forEachIndexed { index, objView ->
                objectTreeRootNode {
                    attrs {
                        objectIndex = index
                        objTreeView = objView
                        editedObject = props.editedObject
                        aspectsMap = props.aspectsMap
                        objectTreeViewModel = props.objectTreeViewModel
                    }
                }
            }
            if (props.editedObject == null || (props.editedObject != null && props.editedObject?.id != null)) {
                Button {
                    attrs {
                        icon = "add"
                        intent = Intent.PRIMARY
                        className = "pt-minimal"
                        this.onClick = { props.objectTreeViewModel.addNewObjTree() }
                    }
                    +"Add Object"
                }
            }
        }
    }
}

fun RBuilder.objectTreeView(block: RHandler<ObjectTreeViewModelConsumerProps>) = child(ObjectTreeView::class, block)

