package com.infowings.catalog.reference.book.treeview

import com.infowings.catalog.common.BadRequestCode
import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.components.popup.ConfirmWindow
import com.infowings.catalog.components.popup.confirmWindow
import com.infowings.catalog.components.popup.popup
import com.infowings.catalog.reference.book.RefBookBadRequestException
import com.infowings.catalog.reference.book.editconsole.bookItemEditConsole
import kotlinx.coroutines.experimental.launch
import kotlinx.html.js.onClickFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.span

class ReferenceBookItemLabel : RComponent<ReferenceBookItemLabel.Props, ReferenceBookItemLabel.State>() {

    private lateinit var forUpdate: ReferenceBookItem

    override fun State.init() {
        updatingBookItem = false
        confirmation = false
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

    private fun handleUpdateBookItem(bookItem: ReferenceBookItem, force: Boolean) {
        forUpdate = bookItem
        tryUpdate(force)
        setState {
            updatingBookItem = false
        }
    }

    private fun tryUpdate(force: Boolean) {
        launch {
            try {
                props.updateBookItem(forUpdate, force)
                setState { confirmation = false }
            } catch (e: RefBookBadRequestException) {
                when (e.exceptionInfo.code) {
                    BadRequestCode.NEED_CONFIRMATION -> setState { confirmation = true }
                    else -> throw e
                }
            }
        }
    }

    override fun RBuilder.render() {
        div(classes = "book-tree-view--label") {
            attrs {
                onClickFunction = ::startUpdatingBookItem
            }
            if (state.updatingBookItem) {
                bookItemEditConsole {
                    attrs {
                        this.bookItem = props.bookItem
                        onCancel = ::cancelUpdatingBookItem
                        onSubmit = { bookItem, force -> handleUpdateBookItem(bookItem, force) }
                    }
                }
            } else {
                span(classes = "book-tree-view--label-item") {
                    +props.bookItem.value
                }
            }
        }

        if (state.confirmation) {
            popup {
                attrs.closePopup = { setState { confirmation = false } }

                confirmWindow {
                    attrs {
                        action = ConfirmWindow.Action.UPDATE
                        message = "This reference book item has linked Object"
                        onCancel = { setState { confirmation = false } }
                        onConfirm = { tryUpdate(true) }
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var aspectId: String
        var bookItem: ReferenceBookItem
        var updateBookItem: suspend (ReferenceBookItem, force: Boolean) -> Unit
    }

    interface State : RState {
        var updatingBookItem: Boolean
        var confirmation: Boolean
    }
}

fun RBuilder.referenceBookItemLabel(block: RHandler<ReferenceBookItemLabel.Props>) =
    child(ReferenceBookItemLabel::class, block)