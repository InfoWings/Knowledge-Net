package com.infowings.catalog.reference.book.treeview

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.ReferenceBookItem
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
        }
    }

    private fun createNewBookHandler(e: Event) {
        e.stopPropagation()
        e.preventDefault()
        props.onNewBookRequest()
    }

    private fun handleCancelChanges() {
        props.cancelBookEditing()
    }


    private fun handleSubmitBookChanges(book: ReferenceBookData) {
        if (book.id == null) {
            setState {
                selectedBook = book
            }
//            props.onReferenceBookCreate(book)
        } else {
            setState {
                selectedBook = book
            }
//            props.onReferenceBookUpdate(book)
        }
    }

    override fun RBuilder.render() {
        div(classes = "aspect-tree-view") {
            props.books.map { book ->
                referenceBookTreeRoot {
                    attrs {
                        key = book.id!!
                        this.book = book
                        selectedId = props.selectedId
                        onBookClick = props.onBookClick
                        onBookItemClick = props.onBookItemClick
                        bookContext = props.bookContext
                    }
                }
            }
            div(classes = "aspect-tree-view--root") {
                if (props.addingNewBook) {
                    bookEditConsole {
                        attrs {
                            book = ReferenceBookData(null, null, props.aspectId)
                            onCancel = ::handleCancelChanges
                            onSubmit = ::handleSubmitBookChanges
                        }
                    }
                } else {
                    div(classes = "aspect-tree-view--label${if (props.selectedId == null) " aspect-tree-view--label__selected" else ""}") {
                        attrs {
                            onClickFunction = ::createNewBookHandler
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
        var selectedBook: ReferenceBookData?
}

    interface Props : RProps {
        var aspectId: String
        var books: List<ReferenceBookData>
        var onBookClick: (ReferenceBookData) -> Unit
        var onBookItemClick: (ReferenceBookItem) -> Unit
        var bookContext: Map<String, ReferenceBookData>
        var onNewBookRequest: () -> Unit
        var selectedId: String?
        var addingNewBook: Boolean
        var onNewBookItemRequest: (AspectData) -> Unit
        var cancelBookEditing: () -> Unit
    }
}

fun RBuilder.referenceBookTreeView(block: RHandler<ReferenceBookTreeView.Props>) =
    child(ReferenceBookTreeView::class, block)