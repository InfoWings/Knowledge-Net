package com.infowings.catalog.reference.book.treeview

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.common.ReferenceBookItemData
import com.infowings.catalog.reference.book.editconsole.bookItemEditConsole
import com.infowings.catalog.utils.addToListIcon
import com.infowings.catalog.utils.squareMinusIcon
import com.infowings.catalog.utils.squarePlusIcon
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.div

class ReferenceBookTreeItem : RComponent<ReferenceBookTreeItem.Props, ReferenceBookTreeItem.State>() {

    override fun State.init() {
        addingBookItem = false
    }

    private fun handleExpanderClick(e: Event) {
        e.stopPropagation()
        e.preventDefault()
        setState {
            expanded = !expanded
        }
    }

    private fun startAddingBookItem(e: Event) {
        e.preventDefault()
        e.stopPropagation()
        setState {
            addingBookItem = true
        }
    }

    private fun cancelBookItemCreating() {
        setState {
            addingBookItem = false
        }
    }

    private fun createBookItem(bookItemData: ReferenceBookItemData) {
        setState {
            addingBookItem = false
        }
        props.createBookItem(bookItemData)
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
                    book = props.book
                    bookItem = props.bookItem
                    updateBookItem = props.updateBookItem
                }
            }

            if (props.bookItem.children.isEmpty()) {
                addToListIcon(classes = "book-tree-view--add-to-list-icon") {
                    attrs {
                        onClickFunction = ::startAddingBookItem
                    }
                }
            }
        }

        if (props.bookItem.children.isNotEmpty() && state.expanded) {
            referenceBookTreeItems {
                attrs {
                    book = props.book
                    bookItem = props.bookItem
                    bookItems = props.bookItem.children
                    createBookItem = props.createBookItem
                    updateBookItem = props.updateBookItem
                }
            }
        }

        if (state.addingBookItem) {
            bookItemEditConsole {
                attrs {
                    bookItemData = ReferenceBookItemData(null, "", props.bookItem.id, props.book.name)
                    onCancel = ::cancelBookItemCreating
                    onSubmit = ::createBookItem
                }
            }
        }
    }

    interface Props : RProps {
        var book: ReferenceBook
        var bookItem: ReferenceBookItem
        var createBookItem: (ReferenceBookItemData) -> Unit
        var updateBookItem: (ReferenceBookItemData) -> Unit
    }

    interface State : RState {
        var expanded: Boolean
        var addingBookItem: Boolean
    }
}

fun RBuilder.referenceBookTreeItem(block: RHandler<ReferenceBookTreeItem.Props>) =
    child(ReferenceBookTreeItem::class, block)