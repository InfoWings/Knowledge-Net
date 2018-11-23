package com.infowings.catalog.objects.view.tree

import com.infowings.catalog.objects.ObjectLazyViewModel
import com.infowings.catalog.objects.view.ObjectsLazyModel
import com.infowings.catalog.utils.encodeURIComponent
import com.infowings.catalog.wrappers.reactRouter
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
            props.indices.forEach { index ->
                objectLazyTreeRootNode {
                    attrs {
                        objectIndex = index
                        objectView = props.objects[index]
                        objectTreeModel = props.objectTreeViewModel
                    }
                }
            }
            reactRouter.Link {
                attrs {
                    className = "new-object-link pt-button pt-intent-primary pt-minimal pt-icon-plus pt-small"
                    role = "button"
                    to = "/objects/new"
                }
                +"New Object"
            }
        }
    }

    interface Props : RProps {
        var objects: List<ObjectLazyViewModel>
        var indices: List<Int>
        var objectTreeViewModel: ObjectsLazyModel
    }
}

fun RBuilder.objectLazyTreeView(block: RHandler<ObjectLazyTreeView.Props>) = child(ObjectLazyTreeView::class, block)
