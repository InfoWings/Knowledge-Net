package com.infowings.catalog.reference.book.treeview

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookData
import com.infowings.catalog.common.ReferenceBookItemData
import com.infowings.catalog.reference.book.editconsole.bookItemEditConsole
import com.infowings.catalog.utils.addToListIcon
import com.infowings.catalog.utils.squareMinusIcon
import com.infowings.catalog.utils.squarePlusIcon
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.div

class ReferenceBookTreeRoot : RComponent<ReferenceBookTreeRoot.Props, ReferenceBookTreeRoot.State>() {

    override fun State.init() {
        addingBookItem = false
    }

    private fun handleExpanderClick(e: Event) {
        e.preventDefault()
        e.stopPropagation()
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
                    onClick = props.onBookClick
                    submitBookChanges = props.submitBookChanges
                    selected = props.selected
                }
            }

            if(props.book.children.isEmpty()) {
                addToListIcon(classes = "book-tree-view--add-to-list-icon") {
                    attrs {
                        onClickFunction = ::startAddingBookItem
                    }
                }
            }
        }
        if (props.book.children.isNotEmpty() && state.expanded) {
            referenceBookTreeItems {
                attrs {
                    book = props.book
                    bookItems = props.book.children
                    createBookItem = props.createBookItem
                    updateBookItem = props.updateBookItem
                }
            }
        }
        if (state.addingBookItem) {
            bookItemEditConsole {
                attrs {
                    bookItemData = ReferenceBookItemData(null, "", props.book.id, props.book.name)
                    onCancel = ::cancelBookItemCreating
                    onSubmit = ::createBookItem
                }
            }
        }
    }

    interface Props : RProps {
        var book: ReferenceBook
        var aspectName: String
        var onBookClick: (aspectName: String, bookData: ReferenceBookData) -> Unit
        var selected: Boolean
        var submitBookChanges: (name: String, ReferenceBookData) -> Unit
        var createBookItem: (ReferenceBookItemData) -> Unit
        var updateBookItem: (ReferenceBookItemData) -> Unit
    }

    interface State : RState {
        var expanded: Boolean
        var addingBookItem: Boolean
    }
}

fun RBuilder.referenceBookTreeRoot(block: RHandler<ReferenceBookTreeRoot.Props>) =
    child(ReferenceBookTreeRoot::class, block)
