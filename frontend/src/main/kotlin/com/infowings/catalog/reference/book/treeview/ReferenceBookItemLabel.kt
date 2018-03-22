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

class ReferenceBookItemLabel : RComponent<ReferenceBookItemLabel.Props, ReferenceBookItemLabel.State>() {

    override fun State.init() {
        editing = false
    }

    private fun handleBookItemLabelClick(e: Event) {
        e.preventDefault()
        e.stopPropagation()
        setState {
            editing = true
        }
    }

    private fun cancelBookItemEditing() {
        setState {
            editing = false
        }
    }

    private fun updateBookItem(bookItemData: ReferenceBookItemData) {
        setState {
            editing = false
        }
        props.updateBookItem(bookItemData)
    }

    override fun RBuilder.render() {
        div(classes = "book-tree-view--label") {
            attrs {
                onClickFunction = ::handleBookItemLabelClick
            }
            if (state.editing) {
                bookItemEditConsole {
                    val bookItem = props.bookItem
                    attrs {
                        this.bookItemData = ReferenceBookItemData(bookItem.id, bookItem.value, null, props.book.name)
                        onCancel = ::cancelBookItemEditing
                        onSubmit = ::updateBookItem
                    }
                }
            } else {
                span(classes = "book-tree-view--label-item") {
                    +props.bookItem.value
                }
            }
        }
    }

    interface Props : RProps {
        var book: ReferenceBook
        var bookItem: ReferenceBookItem
        var updateBookItem: (ReferenceBookItemData) -> Unit
    }

    interface State : RState {
        var editing: Boolean
    }
}

fun RBuilder.referenceBookItemLabel(block: RHandler<ReferenceBookItemLabel.Props>) =
    child(ReferenceBookItemLabel::class, block)