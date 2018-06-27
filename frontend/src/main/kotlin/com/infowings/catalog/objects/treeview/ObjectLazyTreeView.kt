package com.infowings.catalog.objects.treeview

import com.infowings.catalog.objects.ObjectLazyViewModel
import com.infowings.catalog.objects.ObjectsLazyModel
import react.*
import react.dom.div

class ObjectLazyTreeView : RComponent<ObjectLazyTreeView.Props, RState>() {

    companion object {
        init {
            kotlinext.js.require("styles/object-tree-view.scss")
        }
    }

    override fun RBuilder.render() {
        div(classes = "object-tree-view") {
            props.objects.forEachIndexed { index, obj ->
                objectLazyTreeRootNode {
                    attrs {
                        objectIndex = index
                        objectView = obj
                        objectTreeModel = props.objectTreeViewModel
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var objects: List<ObjectLazyViewModel>
        var objectTreeViewModel: ObjectsLazyModel
    }
}

fun RBuilder.objectLazyTreeView(block: RHandler<ObjectLazyTreeView.Props>) = child(ObjectLazyTreeView::class, block)
