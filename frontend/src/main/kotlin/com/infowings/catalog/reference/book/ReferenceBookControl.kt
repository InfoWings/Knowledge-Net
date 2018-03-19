package com.infowings.catalog.reference.book

import com.infowings.catalog.common.ReferenceBookData
import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.reference.book.treeview.referenceBookTreeView
import kotlinext.js.invoke
import kotlinext.js.require
import react.RBuilder
import react.RComponent
import react.RState
import react.setState

class ReferenceBookControl(props: ReferenceBookApiReceiverProps) :
    RComponent<ReferenceBookApiReceiverProps, ReferenceBookControl.State>(props) {

    companion object {
        init {
            require("styles/book-edit-console.scss")
        }
    }

    override fun State.init(props: ReferenceBookApiReceiverProps) {
        selectedBook = null
        selectedBookItem = null
        addingNewBook = false
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

    override fun RBuilder.render() {
        val selectedBook = state.selectedBook
        val selectedAspectId = props.aspectId

        child(Popup::class) {
            attrs {
                closePopup = props.onClosePopup
            }
            referenceBookTreeView {
                attrs {
                    aspectId = selectedAspectId
                    addingNewBook = state.addingNewBook
                    books = props.books
                    selectedId = selectedBook?.id
                    onBookClick = ::handleClickBook
                    onBookItemClick = ::handleClickBookItem
                    onNewBookRequest = ::handleRequestNewBook
                    cancelBookEditing = ::handleCancelBookEditing
                    onReferenceBookCreate = props.onReferenceBookCreate
                    onReferenceBookUpdate = props.onReferenceBookUpdate
                }
            }
        }
    }

    interface State : RState {
        var selectedBook: ReferenceBookData?
        var selectedBookItem: ReferenceBookItem?
        var addingNewBook: Boolean
    }
}