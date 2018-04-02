package com.infowings.catalog.aspects.treeview

import com.infowings.catalog.common.AspectData
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.span

class AspectRootLabel : RComponent<AspectRootLabel.Props, RState>() {

    private fun handleAspectRootLabelClick(e: Event) {
        e.preventDefault()
        e.stopPropagation()
        props.onClick(props.aspect)
    }

    override fun RBuilder.render() {
        div(classes = "aspect-tree-view--label${if (props.selected) " aspect-tree-view--label__selected" else ""}") {
            if (!props.aspect.name.isNullOrEmpty() || !props.aspect.measure.isNullOrEmpty()
                || !props.aspect.domain.isNullOrEmpty() || !props.aspect.baseType.isNullOrEmpty() || props.aspect.subject != null
            ) {
                attrs {
                    onClickFunction = ::handleAspectRootLabelClick
                }
                span(classes = "aspect-tree-view--label-name") {
                    +(props.aspect.name ?: "")
                }
                +":"
                span(classes = "aspect-tree-view--label-measure") {
                    +(props.aspect.measure ?: "")
                }
                +":"
                span(classes = "aspect-tree-view--label-domain") {
                    +(props.aspect.domain ?: "")
                }
                +":"
                span(classes = "aspect-tree-view--label-base-type") {
                    +(props.aspect.baseType ?: "")
                }
                +"Subject:"
                span(classes = "aspect-tree-view--label-subject") {
                    +(props.aspect.subject?.name ?: "Global")
                }
            } else {
                span(classes = "aspect-tree-view--empty") {
                    +"(Enter New Aspect)"
                }
            }
        }
    }

    interface Props : RProps {
        var aspect: AspectData
        var onClick: (AspectData) -> Unit
        var selected: Boolean
    }
}

fun RBuilder.aspectRootLabel(block: RHandler<AspectRootLabel.Props>) = child(AspectRootLabel::class, block)