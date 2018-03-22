package com.infowings.catalog.reference.book.treeview

import com.infowings.catalog.common.ReferenceBookData
import com.infowings.catalog.reference.book.ReferenceBookApiReceiverProps
import kotlinext.js.invoke
import kotlinext.js.require
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
        selectedBookData = null
    }

    private fun startBookUpdating(aspectName: String, bookData: ReferenceBookData) {
        setState {
            selectedAspectName = aspectName
            selectedBookData = bookData
        }
    }

    private fun startCreatingBook(selectedAspectName: String) {
        setState {
            this.selectedAspectName = selectedAspectName
            selectedBookData = null
        }
    }

    private fun createBook(bookData: ReferenceBookData) {
        setState {
            selectedBookData = bookData
        }
        props.createBook(bookData)
    }

    private fun updateBook(bookName: String, bookData: ReferenceBookData) {
        setState {
            selectedBookData = bookData
        }
        props.updateBook(bookName, bookData)

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
                                selected = rowData.aspectName == state.selectedAspectName
                                startBookUpdating = ::startBookUpdating
                                updateBook = ::updateBook
                                createBookItem = props.createBookItem
                                updateBookItem = props.updateBookItem
                            }
                        }
                    } else {
                        referenceBookEmptyTreeRoot {
                            attrs {
                                selected = rowData.aspectName == state.selectedAspectName
                                aspectId = rowData.aspectId
                                aspectName = rowData.aspectName
                                startCreatingBook = ::startCreatingBook
                                createBook = ::createBook
                            }
                        }
                    }
                }
        }
    }

    interface State : RState {
        var selectedBookData: ReferenceBookData?
        var selectedAspectName: String?
    }
}