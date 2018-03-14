package com.infowings.catalog.aspects.treeview

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import kotlinext.js.invoke
import kotlinext.js.require
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.div

class AspectTreeView : RComponent<AspectTreeView.Props, RState>() {

    companion object {
        init {
            require("styles/aspect-tree-view.scss")
        }
    }

    private fun createNewAspectHandler(e: Event) {
        e.stopPropagation()
        e.preventDefault()
        props.onNewAspectRequest()
    }

    override fun RBuilder.render() {
        div(classes = "aspect-tree-view") {
            if (props.aspects.isNotEmpty()) {
                props.aspects.map { aspect ->
                    aspectTreeRoot {
                        attrs {
                            key = aspect.id ?: ""
                            this.aspect = aspect
                            onAspectClick = props.onAspectClick
                            onAspectPropertyClick = props.onAspectPropertyClick
                            aspectContext = props.aspectContext
                        }
                    }
                }
            } else {
                div(classes = "aspect-tree-view--empty") {
                    attrs {
                        onClickFunction = ::createNewAspectHandler
                    }
                    +"Click here to create the first aspect"
                }
            }
        }
    }

    interface Props : RProps {
        var aspects: List<AspectData>
        var onAspectClick: (AspectData) -> Unit
        var onAspectPropertyClick: (AspectPropertyData) -> Unit
        var aspectContext: Map<String, AspectData>
        var onNewAspectRequest: () -> Unit
        var onNewAspectPropertyRequest: (AspectData) -> Unit
    }
}

fun RBuilder.aspectTreeView(block: RHandler<AspectTreeView.Props>) = child(AspectTreeView::class, block)