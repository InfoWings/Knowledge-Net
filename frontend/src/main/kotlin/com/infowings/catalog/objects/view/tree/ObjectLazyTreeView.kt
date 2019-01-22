package com.infowings.catalog.objects.view.tree

import com.infowings.catalog.objects.ObjectLazyViewModel
import com.infowings.catalog.objects.view.ObjectsLazyModel
import com.infowings.catalog.wrappers.RouteSuppliedProps
import react.RBuilder
import react.RComponent
import react.RHandler
import react.RState
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
                    val currProps = props
                    attrs {
                        objectIndex = index
                        objectView = props.objects[index]
                        objectTreeModel = props.objectTreeViewModel
                        history = currProps.history
                    }
                }
            }
        }
    }

    interface Props : RouteSuppliedProps {
        var objects: List<ObjectLazyViewModel>
        var indices: List<Int>
        var objectTreeViewModel: ObjectsLazyModel
    }
}

fun RBuilder.objectLazyTreeView(block: RHandler<ObjectLazyTreeView.Props>) = child(ObjectLazyTreeView::class, block)
