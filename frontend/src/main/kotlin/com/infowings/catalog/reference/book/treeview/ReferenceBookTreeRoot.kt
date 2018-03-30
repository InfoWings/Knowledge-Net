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
        creatingBookItem = false
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

    private suspend fun handleCreateBookItem(bookItemData: ReferenceBookItemData) {
        props.createBookItem(bookItemData)
        setState {
            creatingBookItem = false
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

            if(props.book.children.isEmpty()) {
                addToListIcon(classes = "book-tree-view--add-to-list-icon") {
                    attrs {
                        onClickFunction = ::startCreatingBookItem
                    }
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
                }
            }
        }
        if (state.creatingBookItem) {
            bookItemEditConsole {
                attrs {
                    bookItemData = ReferenceBookItemData("", "", props.book.id, props.aspectId, 0)
                    onCancel = ::cancelCreatingBookItem
                    onSubmit = { handleCreateBookItem(it)}
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
        var updateBook: suspend (ReferenceBookData) -> Unit
        var createBookItem: suspend (ReferenceBookItemData) -> Unit
        var updateBookItem: suspend (ReferenceBookItemData) -> Unit
    }

    interface State : RState {
        var expanded: Boolean
        var creatingBookItem: Boolean
    }
}

fun RBuilder.referenceBookTreeRoot(block: RHandler<ReferenceBookTreeRoot.Props>) =
    child(ReferenceBookTreeRoot::class, block)
