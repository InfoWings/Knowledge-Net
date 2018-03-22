package com.infowings.catalog.reference.book.treeview

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.common.ReferenceBookItemData
import com.infowings.catalog.reference.book.editconsole.bookItemEditConsole
import com.infowings.catalog.wrappers.react.use
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.svg

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
            svg("book-tree-view--line-icon") {
                use("svg/sprite.svg#icon-dots-two-horizontal")
            }
            if (props.bookItem.children.isNotEmpty()) {
                svg("book-tree-view--line-icon book-tree-view--line-icon__clickable") {
                    attrs {
                        onClickFunction = ::handleExpanderClick
                    }
                    if (state.expanded) {
                        use("svg/sprite.svg#icon-squared-minus")
                    } else {
                        use("svg/sprite.svg#icon-squared-plus")
                    }
                }
            } else {
                svg("book-tree-view--line-icon") {
                    attrs {
                        onClickFunction = ::startAddingBookItem
                    }
                    use("svg/sprite.svg#icon-add-to-list")
                }
            }

            referenceBookItemLabel {
                attrs {
                    book = props.book
                    bookItem = props.bookItem
                    updateBookItem = props.updateBookItem
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