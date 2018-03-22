package com.infowings.catalog.reference.book.treeview

import com.infowings.catalog.common.ReferenceBookData
import com.infowings.catalog.reference.book.ReferenceBookApiReceiverProps
import kotlinext.js.invoke
import kotlinext.js.require
import org.w3c.dom.events.Event
import react.RBuilder
import react.RComponent
import react.RState
import react.dom.div
import react.setState

class ReferenceBookTreeView(props: ReferenceBookApiReceiverProps) :
    RComponent<ReferenceBookApiReceiverProps, ReferenceBookTreeView.State>(props) {

    companion object {
        init {
            require("styles/book-tree-view.scss")
            require("styles/book-edit-console.scss")
        }
    }

    override fun State.init(props: ReferenceBookApiReceiverProps) {
        selectedAspectName = null
        selectedBook = null
        creatingNewBook = false
    }

    private fun onBookClick(aspectName: String, book: ReferenceBookData) {
        setState {
            selectedAspectName = aspectName
            selectedBook = book
            creatingNewBook = false
        }
    }

    private fun startCreatingNewBook(aspectName: String, e: Event) {
        e.stopPropagation()
        e.preventDefault()
        setState {
            selectedAspectName = aspectName
            selectedBook = null
            creatingNewBook = true
        }
    }

    private fun cancelBookCreating() {
        setState {
            creatingNewBook = false
        }
    }

    private fun submitBookCreating(book: ReferenceBookData) {
        setState {
            selectedBook = book
        }
        props.onReferenceBookCreate(book)

    }

    private fun submitBookChanges(name: String, book: ReferenceBookData) {
        setState {
            selectedBook = book
        }
        props.onReferenceBookUpdate(name, book)

    }

    override fun RBuilder.render() {
        div(classes = "book-tree-view") {
            props.rowDataList
                .map { rowData ->
                    if (rowData.book != null) {
                        referenceBookTreeRoot {
                            attrs {
                                aspectName = rowData.aspectName
                                book = rowData.book
                                selectedAspectName = state.selectedAspectName
                                onBookClick = ::onBookClick
                                submitBookChanges = ::submitBookChanges
                                cancelBookCreating = ::cancelBookCreating
                                createBookItem = props.createBookItem
                                updateBookItem = props.updateBookItem
                            }
                        }
                    } else {
                        referenceBookEmptyTreeRoot {
                            attrs {
                                selectedAspectName = state.selectedAspectName
                                aspectId = rowData.aspectId
                                aspectName = rowData.aspectName
                                creatingNewBook = state.creatingNewBook
                                startCreatingNewBook = ::startCreatingNewBook
                                cancelBookCreating = ::cancelBookCreating
                                submitBookChanges = ::submitBookCreating
                            }
                        }
                    }
                }
        }
    }

    interface State : RState {
        var creatingNewBook: Boolean
        var selectedBook: ReferenceBookData?
        var selectedAspectName: String?
    }
}