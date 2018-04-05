package com.infowings.catalog.reference.book.treeview

import com.infowings.catalog.common.BadRequestCode.NEED_CONFIRMATION
import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.components.popup.popup
import com.infowings.catalog.components.popup.removeConfirmWindow
import com.infowings.catalog.reference.book.RefBookBadRequestException
import com.infowings.catalog.reference.book.editconsole.bookItemEditConsole
import com.infowings.catalog.utils.addToListIcon
import com.infowings.catalog.utils.ripIcon
import com.infowings.catalog.utils.squareMinusIcon
import com.infowings.catalog.utils.squarePlusIcon
import kotlinx.coroutines.experimental.launch
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.div

class ReferenceBookTreeRoot : RComponent<ReferenceBookTreeRoot.Props, ReferenceBookTreeRoot.State>() {

    override fun State.init() {
        creatingBookItem = false
        confirmation = false
    }

    private fun handleExpanderClick(e: Event) {
        e.preventDefault()
        e.stopPropagation()
        setState {
            expanded = !expanded
        }
    }

    private fun startCreatingBookItem(e: Event) {
        e.preventDefault()
        e.stopPropagation()
        setState {
            creatingBookItem = true
        }
    }

    private fun cancelCreatingBookItem() {
        setState {
            creatingBookItem = false
        }
    }

    private suspend fun handleCreateBookItem(bookItem: ReferenceBookItem) {
        props.createBookItem(bookItem)
        setState {
            creatingBookItem = false
        }
    }

    private fun handleDeleteClick(e: Event) {
        e.preventDefault()
        e.stopPropagation()
        tryDelete(false)
    }

    private fun tryDelete(force: Boolean) {
        launch {
            try {
                props.deleteBook(props.book, force)
                setState {
                    confirmation = false
                }
            } catch (e: RefBookBadRequestException) {
                when (e.exceptionInfo.code) {
                    NEED_CONFIRMATION -> setState {
                        confirmation = true
                    }
                    else -> throw e
                }
            }
        }
    }

    override fun RBuilder.render() {
        div(classes = "book-tree-view--root") {
            if (props.book.children.isNotEmpty()) {
                if (state.expanded) {
                    squareMinusIcon(classes = "book-tree-view--line-icon book-tree-view--line-icon__clickable") {
                        attrs {
                            onClickFunction = ::handleExpanderClick
                        }
                    }
                } else {
                    squarePlusIcon(classes = "book-tree-view--line-icon book-tree-view--line-icon__clickable") {
                        attrs {
                            onClickFunction = ::handleExpanderClick
                        }
                    }
                }
            }

            referenceBookRootLabel {
                attrs {
                    aspectName = props.aspectName
                    book = props.book
                    startUpdatingBook = props.startUpdatingBook
                    updateBook = props.updateBook
                    selected = props.selected
                }
            }

            if (props.book.children.isEmpty()) {
                addToListIcon(classes = "book-tree-view--add-to-list-icon") {
                    attrs {
                        onClickFunction = ::startCreatingBookItem
                    }
                }
            }

            ripIcon("book-tree-view--delete-icon book-tree-view--delete-icon__red") {
                attrs {
                    onClickFunction = ::handleDeleteClick
                }
            }
        }

        if (props.book.children.isNotEmpty() && state.expanded) {
            referenceBookTreeItems {
                attrs {
                    aspectId = props.aspectId
                    book = props.book
                    bookItems = props.book.children
                    createBookItem = props.createBookItem
                    updateBookItem = props.updateBookItem
                    deleteBookItem = props.deleteBookItem
                }
            }
        }

        if (state.creatingBookItem) {
            bookItemEditConsole {
                attrs {
                    bookItem = ReferenceBookItem(props.aspectId, props.book.id, "", "", emptyList(), false, 0)
                    onCancel = ::cancelCreatingBookItem
                    onSubmit = { handleCreateBookItem(it) }
                }
            }
        }

        if (state.confirmation) {
            popup {
                attrs.closePopup = { setState { confirmation = false } }

                removeConfirmWindow {
                    attrs {
                        message = "This reference book is not free"
                        onCancel = { setState { confirmation = false } }
                        onConfirm = { tryDelete(true) }
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var aspectId: String
        var aspectName: String
        var book: ReferenceBook
        var selected: Boolean
        var startUpdatingBook: (aspectName: String) -> Unit
        var updateBook: suspend (ReferenceBook) -> Unit
        var deleteBook: suspend (ReferenceBook, force: Boolean) -> Unit
        var createBookItem: suspend (ReferenceBookItem) -> Unit
        var updateBookItem: suspend (ReferenceBookItem, force: Boolean) -> Unit
        var deleteBookItem: suspend (ReferenceBookItem, force: Boolean) -> Unit
    }

    interface State : RState {
        var expanded: Boolean
        var creatingBookItem: Boolean
        var confirmation: Boolean
    }
}

fun RBuilder.referenceBookTreeRoot(block: RHandler<ReferenceBookTreeRoot.Props>) =
    child(ReferenceBookTreeRoot::class, block)
