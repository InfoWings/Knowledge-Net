package com.infowings.catalog.reference.book.treeview

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.common.ReferenceBookItemData
import com.infowings.catalog.reference.book.editconsole.bookItemEditConsole
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.span

class ReferenceBookTreeItems : RComponent<ReferenceBookTreeItems.Props, ReferenceBookTreeItems.State>() {

    override fun State.init() {
        creatingBookItem = false
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

    private suspend fun handleCreateBookItem(bookItemData: ReferenceBookItemData) {
        props.createBookItem(bookItemData)
        setState {
            creatingBookItem = false
        }
    }

    override fun RBuilder.render() {
        div(classes = "book-tree-view--items-block") {
            props.bookItems.map {
                referenceBookTreeItem {
                    attrs {
                        aspectId = props.aspectId
                        book = props.book
                        key = it.id
                        bookItem = it
                        createBookItem = props.createBookItem
                        updateBookItem = props.updateBookItem
                    }
                }
            }
            if (state.creatingBookItem) {
                bookItemEditConsole {
                    val parentId = props.bookItem?.id ?: props.book.id
                    attrs {
                        bookItemData = ReferenceBookItemData("", "", parentId, props.aspectId, 0)
                        onCancel = ::cancelCreatingBookItem
                        onSubmit = { handleCreateBookItem(it) }
                    }
                }
            } else {
                div(classes = "book-tree-view--item") {
                    div(classes = "book-tree-view--label book-tree-view--label__selected") {
                        attrs {
                            onClickFunction = ::startCreatingBookItem
                        }
                        span(classes = "book-tree-view--empty") {
                            +"(Add Item ...)"
                        }
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var aspectId: String
        var book: ReferenceBook
        var bookItem: ReferenceBookItem?
        var bookItems: List<ReferenceBookItem>
        var createBookItem: suspend (ReferenceBookItemData) -> Unit
        var updateBookItem: suspend (ReferenceBookItemData) -> Unit
    }

    interface State : RState {
        var creatingBookItem: Boolean
    }
}

fun RBuilder.referenceBookTreeItems(block: RHandler<ReferenceBookTreeItems.Props>) =
    child(ReferenceBookTreeItems::class, block)