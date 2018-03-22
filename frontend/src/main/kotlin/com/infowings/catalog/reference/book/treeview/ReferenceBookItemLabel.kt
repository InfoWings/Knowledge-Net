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
        updatingBookItem = false
    }

    private fun startUpdatingBookItem(e: Event) {
        e.preventDefault()
        e.stopPropagation()
        setState {
            updatingBookItem = true
        }
    }

    private fun cancelBookItemUpdating() {
        setState {
            updatingBookItem = false
        }
    }

    private fun updateBookItem(bookItemData: ReferenceBookItemData) {
        setState {
            updatingBookItem = false
        }
        props.updateBookItem(bookItemData)
    }

    override fun RBuilder.render() {
        div(classes = "book-tree-view--label") {
            attrs {
                onClickFunction = ::startUpdatingBookItem
            }
            if (state.updatingBookItem) {
                bookItemEditConsole {
                    val bookItem = props.bookItem
                    attrs {
                        this.bookItemData = ReferenceBookItemData(bookItem.id, bookItem.value, null, props.book.name)
                        onCancel = ::cancelBookItemUpdating
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
        var updatingBookItem: Boolean
    }
}

fun RBuilder.referenceBookItemLabel(block: RHandler<ReferenceBookItemLabel.Props>) =
    child(ReferenceBookItemLabel::class, block)