package com.infowings.catalog.reference.book.treeview

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

    private fun cancelUpdatingBookItem() {
        setState {
            updatingBookItem = false
        }
    }

    private suspend fun handleUpdateBookItem(bookItemData: ReferenceBookItemData) {
        props.updateBookItem(bookItemData)
        setState {
            updatingBookItem = false
        }
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
                        this.bookItemData = ReferenceBookItemData(bookItem.id, bookItem.value, "", props.aspectId)
                        onCancel = ::cancelUpdatingBookItem
                        onSubmit = { handleUpdateBookItem(it) }
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
        var aspectId: String
        var bookItem: ReferenceBookItem
        var updateBookItem: suspend (ReferenceBookItemData) -> Unit
    }

    interface State : RState {
        var updatingBookItem: Boolean
    }
}

fun RBuilder.referenceBookItemLabel(block: RHandler<ReferenceBookItemLabel.Props>) =
    child(ReferenceBookItemLabel::class, block)