package com.infowings.catalog.reference.book.treeview

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookData
import com.infowings.catalog.reference.book.editconsole.bookEditConsole
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.span

class ReferenceBookRootLabel : RComponent<ReferenceBookRootLabel.Props, ReferenceBookRootLabel.State>() {

    override fun State.init() {
        editing = false
    }

    private fun submitBookChanges(bookData: ReferenceBookData) {
        setState {
            editing = false
        }
        props.submitBookChanges(props.book.name, bookData)
    }

    private fun cancelBookCreating() {
        setState {
            editing = false
        }
        props.cancelBookCreating
    }

    private fun handleBookRootLabelClick(e: Event) {
        e.preventDefault()
        e.stopPropagation()
        val book = props.book
        setState {
            editing = true
        }
        props.onClick(props.aspectName, ReferenceBookData(book.id, book.name, book.aspectId))
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
            if (props.selected && state.editing) {
                val book = props.book
                bookEditConsole {
                    attrs {
                        this.book = ReferenceBookData(book.id, book.name, book.aspectId)
                        onCancel = ::cancelBookCreating
                        onSubmit = ::submitBookChanges
                    }
                }
            } else {
                span(classes = "aspect-tree-view--label-name") {
                    +props.book.name
                }
            }
        }
    }

    interface Props : RProps {
        var aspectName: String
        var book: ReferenceBook
        var onClick: (aspectName: String, bookData: ReferenceBookData) -> Unit
        var selected: Boolean
        var submitBookChanges: (name: String, ReferenceBookData) -> Unit
        var cancelBookCreating: () -> Unit
    }

    interface State : RState {
        var editing: Boolean
    }
}

fun RBuilder.referenceBookRootLabel(block: RHandler<ReferenceBookRootLabel.Props>) =
    child(ReferenceBookRootLabel::class, block)