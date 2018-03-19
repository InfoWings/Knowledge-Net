package com.infowings.catalog.reference.book

import com.infowings.catalog.aspects.AspectApiReceiverProps
import com.infowings.catalog.aspects.treeview.aspectTreeView
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.reference.book.treeview.ReferenceBookData
import com.infowings.catalog.reference.book.treeview.referenceBookTreeView
import kotlinext.js.invoke
import kotlinext.js.require
import react.RBuilder
import react.RComponent
import react.RState
import react.setState

class ReferenceBookControl(props: AspectApiReceiverProps) :
    RComponent<AspectApiReceiverProps, ReferenceBookControl.State>(props) {

    companion object {
        init {
            require("styles/book-edit-console.scss")
        }
    }

    override fun State.init(props: AspectApiReceiverProps) {
        selectedAspect = null
        selectedBook = null
        selectedBookItem = null
        addingNewBook = false
    }

    private fun handleClickAspect(aspect: AspectData) {
        setState {
            selectedAspect = aspect
        }
    }

    private fun handleClickBook(book: ReferenceBookData) {
        setState {
            selectedBook = book
            addingNewBook = false
        }
    }


    private fun handleClickBookItem(bookItem: ReferenceBookItem) {
        setState {
            selectedBookItem = bookItem
        }
    }

    private fun handleRequestNewBook() {
        setState {
            addingNewBook = true
            selectedBook = null
            selectedBookItem = null
        }
    }

    private fun handleCancelBookEditing() {
        setState {
            addingNewBook = false
        }
    }

    private fun closePopup() {
        setState {
            selectedAspect = null
        }
    }

    override fun RBuilder.render() {
        val selectedAspect = state.selectedAspect
        val selectedBook = state.selectedBook
        aspectTreeView {
            attrs {
                aspects = if (selectedAspect != null && selectedAspect.id == null)
                    props.data + selectedAspect
                else props.data
                aspectContext = props.aspectContext
                selectedId = when {
                    selectedAspect != null -> selectedAspect.id
                    else -> null
                }
                onAspectClick = ::handleClickAspect
            }
        }
        if (selectedAspect?.id != null) {
            child(Popup::class) {
                attrs {
                    closePopup = ::closePopup
                }
                referenceBookTreeView {
                    attrs {
                        val inner = listOf(
                            ReferenceBookItem("id00", "val01"),
                            ReferenceBookItem("id01", "val02")
                        )
                        val items = listOf(
                            ReferenceBookItem("id1", "val1", inner),
                            ReferenceBookItem("id2", "val2", inner),
                            ReferenceBookItem("id3", "val3", inner)
                        )
                        val list = listOf(
                            ReferenceBookData("id1", "book1", "aspId", items),
                            ReferenceBookData("id2", "book2", "aspId", items),
                            ReferenceBookData("id3", "book3", "aspId", items)
                        )

                        aspectId = selectedAspect.id!!
                        addingNewBook = state.addingNewBook
                        books = list
                        selectedId = selectedBook?.id
                        onBookClick = ::handleClickBook
                        onBookItemClick = ::handleClickBookItem
                        onNewBookRequest = ::handleRequestNewBook
                        cancelBookEditing = ::handleCancelBookEditing
                    }
                }
            }
        }
    }

    interface State : RState {
        var selectedAspect: AspectData?
        var selectedBook: ReferenceBookData?
        var selectedBookItem: ReferenceBookItem?
        var addingNewBook: Boolean
    }
}