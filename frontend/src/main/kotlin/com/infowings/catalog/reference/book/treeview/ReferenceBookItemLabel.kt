package com.infowings.catalog.reference.book.treeview

import com.infowings.catalog.common.ReferenceBookItem
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.span

class ReferenceBookItemLabel : RComponent<ReferenceBookItemLabel.Props, RState>() {

    private fun handleAspectPropertyLabelClick(e: Event) {
        e.preventDefault()
        e.stopPropagation()
        props.onClick(props.bookItem)
    }

    override fun RBuilder.render() {
        div(classes = "book-tree-view--label") {
            attrs {
                onClickFunction = ::handleAspectPropertyLabelClick
            }
            span(classes = "book-tree-view--label-property") {
                +props.bookItem.value
            }
        }
    }

    interface Props : RProps {
        var bookItem: ReferenceBookItem
        var onClick: (ReferenceBookItem) -> Unit
    }
}

fun RBuilder.referenceBookItemLabel(block: RHandler<ReferenceBookItemLabel.Props>) =
    child(ReferenceBookItemLabel::class, block)