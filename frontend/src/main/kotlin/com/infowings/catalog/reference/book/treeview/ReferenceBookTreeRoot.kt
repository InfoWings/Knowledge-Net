package com.infowings.catalog.reference.book.treeview

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.common.ReferenceBookData
import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.common.ReferenceBookItemData
import com.infowings.catalog.reference.book.editconsole.bookItemEditConsole
import com.infowings.catalog.wrappers.react.use
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.svg

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
            referenceBookRootLabel {
                attrs {
                    aspectName = props.aspectName
                    book = props.book
                    onClick = props.onBookClick
                    submitBookChanges = props.submitBookChanges
                    cancelBookCreating = props.cancelBookCreating
                    selected = props.selectedAspectName == props.aspectName
                }
            }
        }
        if (props.book.children.isNotEmpty() && state.expanded) {
            referenceBookTreeItems {
                attrs {
                    book = props.book
                    bookItems = props.book.children
                    onBookItemClick = props.onBookItemClick
                    createBookItem = props.createBookItem
                }
            }
        }
        if (state.addingBookItem) {
            bookItemEditConsole {
                attrs {
                    bookItem = ReferenceBookItemData(null, "", props.book.id, props.book.name)
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
        var onBookItemClick: (ReferenceBookItem) -> Unit
        var selectedAspectName: String?
        var submitBookChanges: (name: String, ReferenceBookData) -> Unit
        var cancelBookCreating: () -> Unit
        var createBookItem: (ReferenceBookItemData) -> Unit
    }

    interface State : RState {
        var expanded: Boolean
        var addingBookItem: Boolean
    }
}

fun RBuilder.referenceBookTreeRoot(block: RHandler<ReferenceBookTreeRoot.Props>) =
    child(ReferenceBookTreeRoot::class, block)
