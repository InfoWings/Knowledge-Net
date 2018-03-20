package com.infowings.catalog.reference.book.treeview

import com.infowings.catalog.common.ReferenceBookData
import com.infowings.catalog.reference.book.AspectBookPair
import kotlinext.js.invoke
import kotlinext.js.require
import org.w3c.dom.events.Event
import react.*
import react.dom.div

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

    private fun submitBookChanges(aspectName: String, book: ReferenceBookData) {
        if (book.id == null) {
            setState {
                selectedBook = book
            }
            props.onReferenceBookCreate(aspectName, book)
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
            props.aspectBookPairs
                .map { pair ->
                    if (pair.book != null) {
                        referenceBookTreeRoot {
                            attrs {
                                key = pair.aspectName
                                aspectName = pair.aspectName
                                book = pair.book
                                selectedId = selectedBookId
                                onBookClick = ::onBookClick
                            }
                        }
                    } else {
                        referenceBookEmptyTreeRoot {
                            attrs {
                                key = pair.aspectName
                                aspectName = pair.aspectName
                                creatingNewBook = state.creatingNewBook
                                startCreatingNewBook = ::startCreatingNewBook
                                cancelBookCreating = ::cancelBookCreating
                                submitBookChanges = ::submitBookChanges
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
        var aspectBookPairs: List<AspectBookPair>
        var onReferenceBookCreate: (aspectName: String, bookData: ReferenceBookData) -> Unit
        var onReferenceBookUpdate: (bookData: ReferenceBookData) -> Unit
    }
}

fun RBuilder.referenceBookTreeView(block: RHandler<ReferenceBookTreeView.Props>) =
    child(ReferenceBookTreeView::class, block)