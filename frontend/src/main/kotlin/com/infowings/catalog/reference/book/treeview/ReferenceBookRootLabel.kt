package com.infowings.catalog.reference.book.treeview

import com.infowings.catalog.common.ReferenceBook
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
            if (props.book.id != null) {
                attrs {
                    onClickFunction = ::handleBookRootLabelClick
                }
                span(classes = "aspect-tree-view--label-name") {
                    +props.book.name
                }
            } else {
                span(classes = "aspect-tree-view--empty") {
                    +"(Add Reference Book ...)"
                }
            }
        }
    }

    interface Props : RProps {
        var book: ReferenceBook
        var onClick: (ReferenceBook) -> Unit
        var selected: Boolean
    }
}

fun RBuilder.referenceBookRootLabel(block: RHandler<ReferenceBookRootLabel.Props>) =
    child(ReferenceBookRootLabel::class, block)