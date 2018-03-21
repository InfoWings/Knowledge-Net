package com.infowings.catalog.reference.book.treeview

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.common.ReferenceBookItemData
import com.infowings.catalog.wrappers.react.use
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.svg

class ReferenceBookTreeItem : RComponent<ReferenceBookTreeItem.Props, ReferenceBookTreeItem.State>() {

    private fun handleExpanderClick(e: Event) {
        e.stopPropagation()
        e.preventDefault()
        setState {
            expanded = !expanded
        }
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
                    use("svg/sprite.svg#icon-add-to-list")
                }
            }

            referenceBookItemLabel {
                attrs {
                    bookItem = props.bookItem
                    onClick = props.onBookItemClick
                }
            }
        }
        if (props.bookItem.children.isNotEmpty() && state.expanded) {
            referenceBookTreeItems {
                attrs {
                    book = props.book
                    bookItems = props.bookItem.children
                    onBookItemClick = props.onBookItemClick
                    createBookItem = props.createBookItem
                }
            }
        }
    }

    interface Props : RProps {
        var book: ReferenceBook
        var bookItem: ReferenceBookItem
        var onBookItemClick: (ReferenceBookItem) -> Unit
        var createBookItem: (ReferenceBookItemData) -> Unit
    }

    interface State : RState {
        var expanded: Boolean
    }
}

fun RBuilder.referenceBookTreeItem(block: RHandler<ReferenceBookTreeItem.Props>) =
    child(ReferenceBookTreeItem::class, block)