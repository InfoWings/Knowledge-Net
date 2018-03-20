package com.infowings.catalog.reference.book.treeview

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookData
import com.infowings.catalog.reference.book.editconsole.bookEditConsole
import kotlinext.js.invoke
import kotlinext.js.require
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.span

class ReferenceBookTreeView(props: Props) :
    RComponent<ReferenceBookTreeView.Props, ReferenceBookTreeView.State>(props) {

    companion object {
        init {
            require("styles/aspect-tree-view.scss")
            require("styles/book-edit-console.scss")
        }
    }

    override fun State.init(props: ReferenceBookTreeView.Props) {
        selectedBook = null
        creatingNewBook = false
    }

    private fun onBookClick(book: ReferenceBookData) {
        setState {
            selectedBook = book
            creatingNewBook = false
        }
    }

    private fun startCreatingNewBook(e: Event) {
        e.stopPropagation()
        e.preventDefault()
        setState {
            selectedBook = null
            creatingNewBook = true
        }
    }

    private fun cancelBookCreating() {
        setState {
            creatingNewBook = false
        }
    }

    private fun submitBookChanges(book: ReferenceBookData) {
        if (book.id == null) {
            setState {
                selectedBook = book
            }
            props.onReferenceBookCreate(book)
        } else {
            setState {
                selectedBook = book
            }
            props.onReferenceBookUpdate(book)
        }
    }

    override fun RBuilder.render() {
        val selectedBookId = state.selectedBook?.id
        div(classes = "aspect-tree-view") {
            props.books.map { book ->
                referenceBookTreeRoot {
                    attrs {
                        key = book.id
                        this.book = book
                        selectedId = selectedBookId
                        onBookClick = ::onBookClick
                    }
                }
            }
            div(classes = "aspect-tree-view--root") {
                if (state.creatingNewBook) {
                    bookEditConsole {
                        attrs {
                            book = ReferenceBookData(null, "", props.aspectId)
                            onCancel = ::cancelBookCreating
                            onSubmit = ::submitBookChanges
                        }
                    }
                } else {
                    div(classes = "aspect-tree-view--label${if (selectedBookId == null) " aspect-tree-view--label__selected" else ""}") {
                        attrs {
                            onClickFunction = ::startCreatingNewBook
                        }
                        span(classes = "aspect-tree-view--empty") {
                            +"Add Reference Book ..."
                        }
                    }
                }
            }
        }
    }

    interface State : RState {
        var creatingNewBook: Boolean
        var selectedBook: ReferenceBookData?
    }

    interface Props : RProps {
        var aspectId: String
        var books: List<ReferenceBook>
        var onReferenceBookCreate: (ReferenceBookData) -> Unit
        var onReferenceBookUpdate: (ReferenceBookData) -> Unit
    }
}

fun RBuilder.referenceBookTreeView(block: RHandler<ReferenceBookTreeView.Props>) =
    child(ReferenceBookTreeView::class, block)