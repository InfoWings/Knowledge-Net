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

class ReferenceBookTreeItem : RComponent<ReferenceBookTreeItem.Props, ReferenceBookTreeItem.State>() {

    override fun State.init() {
        creatingBookItem = false
        confirmation = false
    }

    private fun handleExpanderClick(e: Event) {
        e.stopPropagation()
        e.preventDefault()
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
                props.deleteBookItem(props.bookItem, force)
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
        div(classes = "book-tree-view--item") {
            if (props.bookItem.children.isNotEmpty()) {
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

            referenceBookItemLabel {
                attrs {
                    aspectId = props.aspectId
                    bookItem = props.bookItem
                    updateBookItem = props.updateBookItem
                }
            }

            if (props.bookItem.children.isEmpty()) {
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

        if (props.bookItem.children.isNotEmpty() && state.expanded) {
            referenceBookTreeItems {
                attrs {
                    aspectId = props.aspectId
                    book = props.book
                    bookItem = props.bookItem
                    bookItems = props.bookItem.children
                    createBookItem = props.createBookItem
                    updateBookItem = props.updateBookItem
                    deleteBookItem = props.deleteBookItem
                }
            }
        }
        div(classes = "book-tree-view--item") {
            if (state.creatingBookItem) {
                bookItemEditConsole {
                    attrs {
                        bookItem = ReferenceBookItem(props.aspectId, props.bookItem.id, "", "", emptyList(), false, 0)
                        onCancel = ::cancelCreatingBookItem
                        onSubmit = { handleCreateBookItem(it) }
                    }
                }
            }
        }
        if (state.confirmation) {
            popup {
                attrs.closePopup = { setState { confirmation = false } }

                removeConfirmWindow {
                    attrs {
                        message = "This reference book item is not free"
                        onCancel = { setState { confirmation = false } }
                        onConfirm = { tryDelete(true) }
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var aspectId: String
        var book: ReferenceBook
        var bookItem: ReferenceBookItem
        var createBookItem: suspend (ReferenceBookItem) -> Unit
        var updateBookItem: suspend (ReferenceBookItem) -> Unit
        var deleteBookItem: suspend (ReferenceBookItem, force: Boolean) -> Unit

    }

    interface State : RState {
        var expanded: Boolean
        var creatingBookItem: Boolean
        var confirmation: Boolean
    }
}

fun RBuilder.referenceBookTreeItem(block: RHandler<ReferenceBookTreeItem.Props>) =
    child(ReferenceBookTreeItem::class, block)