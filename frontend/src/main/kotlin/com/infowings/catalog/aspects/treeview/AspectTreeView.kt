package com.infowings.catalog.aspects.treeview

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import kotlinext.js.invoke
import kotlinext.js.require
import react.*
import react.dom.div

class AspectTreeView(props: Props) : RComponent<AspectTreeView.Props, AspectTreeView.State>(props) {

    companion object {
        init {
            require("styles/aspect-tree-view.scss")
        }
    }

    override fun State.init(props: Props) {
        buildingNewAspect = props.aspects.isEmpty()
    }

    override fun RBuilder.render() {
        div(classes = "aspect-tree-view") {
            props.aspects.map { aspect ->
                aspectTreeRoot {
                    attrs {
                        key = aspect.id ?: ""
                        this.aspect = aspect
                        selectedId = props.selectedId
                        selectedPropertyIndex = props.selectedPropertyIndex
                        onAspectClick = props.onAspectClick
                        onAspectPropertyClick = props.onAspectPropertyClick
                        aspectContext = props.aspectContext
                        onAspectPropertyRequest = props.onNewAspectPropertyRequest
                    }
                }
            }
        }
    }

    interface State : RState {
        var buildingNewAspect: Boolean
    }

    interface Props : RProps {
        var aspects: List<AspectData>
        var onAspectClick: (AspectData) -> Unit
        var onAspectPropertyClick: (AspectPropertyData) -> Unit
        var aspectContext: Map<String, AspectData>
        var selectedId: String?
        var selectedPropertyIndex: Int?
        var onNewAspectPropertyRequest: (AspectData) -> Unit
    }
}

fun RBuilder.aspectTreeView(block: RHandler<AspectTreeView.Props>) = child(AspectTreeView::class, block)