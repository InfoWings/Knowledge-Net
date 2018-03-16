package com.infowings.catalog.reference.book

import com.infowings.catalog.aspects.AspectApiReceiverProps
import com.infowings.catalog.aspects.treeview.aspectTreeView
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookItem
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
            require("styles/aspect-edit-console.scss")
        }
    }

    override fun State.init(props: AspectApiReceiverProps) {
        selectedAspect = null
        selectedBook = ReferenceBook("", "", null)
        selectedBookItem = null
    }

    private fun handleClickAspect(aspect: AspectData) {
        setState {
            selectedAspect = aspect
        }
    }

    private fun handleClickBook(book: ReferenceBook) {
        setState {
            selectedBook = book
        }
    }


    private fun handleClickBookItem(bookItem: ReferenceBookItem) {
        setState {
            selectedBookItem = bookItem
        }
    }

    private fun handleRequestNewBook() {
        setState {
            selectedBookItem = null
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
                            ReferenceBook("book1", "aspId", ReferenceBookItem("id1", "", items)),
                            ReferenceBook("book2", "aspId", ReferenceBookItem("id2", "", items)),
                            ReferenceBook("book3", "aspId", ReferenceBookItem("id3", "", items))
                        ) + selectedBook
                        books = list

                        selectedId = selectedBook.id
                        onBookClick = ::handleClickBook
                        onBookItemClick = ::handleClickBookItem
                        onNewBookRequest = ::handleRequestNewBook
                    }
                }
            }
        }
    }

    interface State : RState {
        var selectedAspect: AspectData?
        var selectedBook: ReferenceBook
        var selectedBookItem: ReferenceBookItem?
    }
}