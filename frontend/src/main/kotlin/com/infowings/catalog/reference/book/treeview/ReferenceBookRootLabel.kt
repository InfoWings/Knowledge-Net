package com.infowings.catalog.reference.book.treeview

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookData
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.span

class ReferenceBookRootLabel : RComponent<ReferenceBookRootLabel.Props, RState>() {

    private fun handleBookRootLabelClick(e: Event) {
        e.preventDefault()
        e.stopPropagation()
        val book = props.book
        props.onClick(ReferenceBookData(book.id, book.name, book.aspectId))
    }

    override fun RBuilder.render() {
        div(classes = "aspect-tree-view--label${if (props.selected) " aspect-tree-view--label__selected" else ""}") {
            attrs {
                onClickFunction = ::handleBookRootLabelClick
            }
            span(classes = "aspect-tree-view--label-name") {
                +props.aspectName
            }
            +":"
            span(classes = "aspect-tree-view--label-name") {
                +props.book.name
            }
        }
    }

    interface Props : RProps {
        var aspectName: String
        var book: ReferenceBook
        var onClick: (ReferenceBookData) -> Unit
        var selected: Boolean
    }
}

fun RBuilder.referenceBookRootLabel(block: RHandler<ReferenceBookRootLabel.Props>) =
    child(ReferenceBookRootLabel::class, block)