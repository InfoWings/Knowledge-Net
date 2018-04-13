package com.infowings.catalog.reference.book.treeview

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.reference.book.editconsole.bookEditConsole
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.span

class ReferenceBookLabel : RComponent<ReferenceBookLabel.Props, ReferenceBookLabel.State>() {

    override fun State.init() {
        updatingBook = false
    }

    private suspend fun handleUpdateBook(book: ReferenceBook) {
        props.updateBook(book)
        setState {
            updatingBook = false
        }
    }

    private fun cancelUpdatingBook() {
        setState {
            updatingBook = false
        }
    }

    private fun startUpdatingBook(e: Event) {
        e.preventDefault()
        e.stopPropagation()
        setState {
            updatingBook = true
        }
        props.startUpdatingBook(props.aspectName)
    }

    override fun RBuilder.render() {
        div(classes = "book-tree-view--label${if (props.selected) " book-tree-view--label__selected" else ""}") {
            attrs {
                onClickFunction = ::startUpdatingBook
            }
            span(classes = "book-tree-view--label-name") {
                +props.aspectName
            }
            +":"
            if (props.selected && state.updatingBook) {
                bookEditConsole {
                    attrs {
                        this.book = props.book
                        onCancel = ::cancelUpdatingBook
                        onSubmit = { handleUpdateBook(it) }
                    }
                }
            } else {
                span(classes = "book-tree-view--label-name") {
                    +props.book.name
                }
            }
        }
    }

    interface Props : RProps {
        var aspectName: String
        var book: ReferenceBook
        var selected: Boolean
        var startUpdatingBook: (aspectName: String) -> Unit
        var updateBook: suspend (ReferenceBook) -> Unit
    }

    interface State : RState {
        var updatingBook: Boolean
    }
}

fun RBuilder.referenceBookLabel(block: RHandler<ReferenceBookLabel.Props>) =
    child(ReferenceBookLabel::class, block)