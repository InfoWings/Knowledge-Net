package com.infowings.catalog.reference.book.treeview

import com.infowings.catalog.reference.book.ReferenceBookApiReceiverProps
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
    }

    private fun startCreatingBook(selectedAspectName: String) {
        setState {
            this.selectedAspectName = selectedAspectName
        }
    }

    private fun startUpdatingBook(aspectName: String) {
        setState {
            selectedAspectName = aspectName
        }
    }

    override fun RBuilder.render() {
        div(classes = "book-tree-view") {
            props.rowDataList
                .map { (aspectId, aspectName, book) ->
                    if (book != null) {
                        referenceBookTreeNode {
                            attrs {
                                this.aspectId = aspectId
                                this.aspectName = aspectName
                                this.book = book
                                selected = aspectName == state.selectedAspectName
                                startUpdatingBook = ::startUpdatingBook
                                updateBook = props.updateBook
                                deleteBook = props.deleteBook
                                createBookItem = props.createBookItem
                                updateBookItem = props.updateBookItem
                                deleteBookItem = props.deleteBookItem
                            }
                        }
                    } else {
                        referenceBookEmptyTreeNode {
                            attrs {
                                selected = aspectName == state.selectedAspectName
                                this.aspectId = aspectId
                                this.aspectName = aspectName
                                startCreatingBook = ::startCreatingBook
                                createBook = props.createBook
                            }
                        }
                    }
                }
        }
    }

    interface State : RState {
        var selectedAspectName: String?
    }
}