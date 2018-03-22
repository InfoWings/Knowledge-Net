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
        updatingBook = false
    }

    private fun updateBook(bookData: ReferenceBookData) {
        setState {
            updatingBook = false
        }
        props.updateBook(props.book.name, bookData)
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
                val book = props.book
                bookEditConsole {
                    attrs {
                        this.bookData = ReferenceBookData(book.id, book.name, book.aspectId)
                        onCancel = ::cancelUpdatingBook
                        onSubmit = ::updateBook
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
        var startUpdatingBook: (aspectName: String) -> Unit
        var selected: Boolean
        var updateBook: (bookName: String, ReferenceBookData) -> Unit
    }

    interface State : RState {
        var updatingBook: Boolean
    }
}

fun RBuilder.referenceBookRootLabel(block: RHandler<ReferenceBookRootLabel.Props>) =
    child(ReferenceBookRootLabel::class, block)