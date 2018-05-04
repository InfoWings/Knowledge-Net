package com.infowings.catalog.reference.book.treeview

import com.infowings.catalog.common.BadRequestCode.NEED_CONFIRMATION
import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.common.ReferenceBookItemData
import com.infowings.catalog.components.popup.forceRemoveConfirmWindow
import com.infowings.catalog.components.treeview.treeNode
import com.infowings.catalog.reference.book.RefBookBadRequestException
import com.infowings.catalog.reference.book.editconsole.bookItemEditConsole
import com.infowings.catalog.utils.addToListIcon
import com.infowings.catalog.utils.ripIcon
import kotlinx.coroutines.experimental.launch
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.div

class ReferenceBookItemNode : RComponent<ReferenceBookItemNode.Props, ReferenceBookItemNode.State>() {

    override fun State.init() {
        creatingBookItem = false
        confirmation = false
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
        props.createBookItem(props.aspectId, ReferenceBookItemData(props.bookItem.id, bookItem))
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
                props.deleteBookItem(props.aspectId, props.bookItem, force)
                setState { confirmation = false }
            } catch (e: RefBookBadRequestException) {
                when (e.exceptionInfo.code) {
                    NEED_CONFIRMATION -> setState { confirmation = true }
                    else -> throw e
                }
            }
        }
    }

    override fun RBuilder.render() {

        val notDeletedBookItems = props.bookItem.children.filter { !it.deleted }

        treeNode {
            attrs {
                expanded = state.creatingBookItem
                treeNodeContent = buildElement {
                    div(classes = "book-tree-view--item") {
                        referenceBookItemLabel {
                            attrs {
                                aspectId = props.aspectId
                                bookItem = props.bookItem
                                updateBookItem = props.updateBookItem
                            }
                        }

                        addToListIcon(classes = "book-tree-view--add-to-list-icon") {
                            attrs {
                                onClickFunction = ::startCreatingBookItem
                            }
                        }

                        ripIcon("book-tree-view--delete-icon book-tree-view--delete-icon__red") {
                            attrs {
                                onClickFunction = ::handleDeleteClick
                            }
                        }

                        if (state.confirmation) {
                            forceRemoveConfirmWindow {
                                attrs {
                                    onConfirm = { tryDelete(true) }
                                    onCancel = { setState { confirmation = false } }
                                    isOpen = state.confirmation
                                    message = "Reference book item has linked entities."
                                }
                            }
                        }
                    }
                }!!
            }

            if (notDeletedBookItems.isNotEmpty()) {
                notDeletedBookItems.forEach { bookItem ->
                    referenceBookItemNode {
                        attrs {
                            key = bookItem.id
                            aspectId = props.aspectId
                            book = props.book
                            this.bookItem = bookItem
                            createBookItem = props.createBookItem
                            updateBookItem = props.updateBookItem
                            deleteBookItem = props.deleteBookItem
                        }
                    }
                }
            }

            if (state.creatingBookItem) {
                bookItemEditConsole {
                    attrs {
                        bookItem = ReferenceBookItem("", "", emptyList(), false, 0)
                        onCancel = ::cancelCreatingBookItem
                        onSubmit = { bookItem, _ -> handleCreateBookItem(bookItem) }
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var aspectId: String
        var book: ReferenceBook
        var bookItem: ReferenceBookItem
        var createBookItem: suspend (aspectId: String, ReferenceBookItemData) -> Unit
        var updateBookItem: suspend (aspectId: String, ReferenceBookItem, force: Boolean) -> Unit
        var deleteBookItem: suspend (aspectId: String, ReferenceBookItem, force: Boolean) -> Unit

    }

    interface State : RState {
        var creatingBookItem: Boolean
        var confirmation: Boolean
    }
}

fun RBuilder.referenceBookItemNode(block: RHandler<ReferenceBookItemNode.Props>) =
    child(ReferenceBookItemNode::class, block)