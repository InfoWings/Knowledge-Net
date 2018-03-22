package com.infowings.catalog.aspects.treeview

import com.infowings.catalog.common.AspectData
import kotlinext.js.invoke
import kotlinext.js.require
import react.*
import react.dom.div

class AspectTreeView : RComponent<AspectTreeView.Props, RState>() {

    companion object {
        init {
            require("styles/aspect-tree-view.scss")
        }
    }

    override fun RBuilder.render() {
        div(classes = "aspect-tree-view") {
            props.aspects.map { aspect ->
                aspectTreeRoot {
                    attrs {
                        key = aspect.id ?: ""
                        this.aspect = aspect
                        selectedAspect = props.selectedAspect
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

    interface Props : RProps {
        var aspects: List<AspectData>
        var onAspectClick: (AspectData) -> Unit
        var onAspectPropertyClick: (AspectData, propertyIndex: Int) -> Unit
        var aspectContext: Map<String, AspectData>
        var selectedAspect: AspectData?
        var selectedPropertyIndex: Int?
        var onNewAspectPropertyRequest: (AspectData) -> Unit
    }
}

fun RBuilder.aspectTreeView(block: RHandler<AspectTreeView.Props>) = child(AspectTreeView::class, block)