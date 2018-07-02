package com.infowings.catalog.objects.treeedit

import com.infowings.catalog.objects.ObjectTreeEditModelConsumerProps
import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.Intent
import kotlinext.js.require
import react.RBuilder
import react.RComponent
import react.RHandler
import react.RState
import react.dom.div

class ObjectTreeView : RComponent<ObjectTreeEditModelConsumerProps, RState>() {

    companion object {
        init {
            require("styles/object-tree-edit.scss")
        }
    }

    override fun RBuilder.render() {
        div(classes = "object-tree-edit") {
            props.objectForest.forEachIndexed { index, objView ->
                objectTreeRootNode {
                    attrs {
                        objectIndex = index
                        objTreeEdit = objView
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
                        this.onClick = { props.objectTreeViewModel.addObject() }
                    }
                    +"Add Object"
                }
            }
        }
    }
}

fun RBuilder.objectTreeView(block: RHandler<ObjectTreeEditModelConsumerProps>) = child(ObjectTreeView::class, block)

