package com.infowings.catalog.reference.book.treeview

import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.span

class ReferenceBookRootLabel : RComponent<ReferenceBookRootLabel.Props, RState>() {

    private fun handleBookRootLabelClick(e: Event) {
        e.preventDefault()
        e.stopPropagation()
        props.onClick(props.book)
    }

    override fun RBuilder.render() {
        div(classes = "aspect-tree-view--label${if (props.selected) " aspect-tree-view--label__selected" else ""}") {
            attrs {
                onClickFunction = ::handleBookRootLabelClick
            }
            span(classes = "aspect-tree-view--label-name") {
                +props.book.name!!
            }
        }
    }

    interface Props : RProps {
        var book: ReferenceBookData
        var onClick: (ReferenceBookData) -> Unit
        var selected: Boolean
    }
}

fun RBuilder.referenceBookRootLabel(block: RHandler<ReferenceBookRootLabel.Props>) =
    child(ReferenceBookRootLabel::class, block)