package com.infowings.catalog.aspects.treeview

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.span

class AspectPropertyLabel : RComponent<AspectPropertyLabel.Props, RState>() {

    private fun handleAspectPropertyLabelClick(e: Event) {
        e.preventDefault()
        e.stopPropagation()
        props.onClick(props.aspectProperty)
    }

    override fun RBuilder.render() {
        val selectedClass = when {
            props.aspectSelected -> " aspect-tree-view--label__aspect-selected"
            props.propertySelected -> " aspect-tree-view--label__property-selected"
            else -> ""
        }
        div(classes = "aspect-tree-view--label$selectedClass") {
            if (props.aspectProperty.id != "") {
                attrs {
                    onClickFunction = ::handleAspectPropertyLabelClick
                }
                span(classes = "aspect-tree-view--label-name") {
                    +(props.aspect?.name ?: "")
                }
                +":"
                span(classes = "aspect-tree-view--label-property") {
                    +"("
                    span(classes = "aspect-tree-view--label-property-name") {
                        +props.aspectProperty.name
                    }
                    +":"
                    span(classes = "aspect-tree-view--label-property-cardinality") {
                        +props.aspectProperty.cardinality
                    }
                    +")"
                }
                +":"
                span(classes = "aspect-tree-view--label-measure") {
                    +(props.aspect?.measure ?: "")
                }
                +":"
                span(classes = "aspect-tree-view--label-domain") {
                    +(props.aspect?.domain ?: "")
                }
                +":"
                span(classes = "aspect-tree-view--label-base-type") {
                    +(props.aspect?.baseType ?: "")
                }
            } else {
                span(classes = "aspect-tree-view--empty") {
                    +"(Aspect Property Placeholder)"
                }
            }
        }
    }

    interface Props : RProps {
        var aspectProperty: AspectPropertyData
        var aspect: AspectData?
        var onClick: (AspectPropertyData) -> Unit
        var propertySelected: Boolean
        var aspectSelected: Boolean
    }
}

fun RBuilder.aspectPropertyLabel(block: RHandler<AspectPropertyLabel.Props>) = child(AspectPropertyLabel::class, block)