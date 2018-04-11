package com.infowings.catalog.components.delete

import com.infowings.catalog.wrappers.blueprint.Button
import react.*
import react.dom.div
import react.dom.h5
import react.dom.p


class DeletePopoverWindow : RComponent<DeletePopoverWindow.Props, RState>() {

    companion object {
        init {
            kotlinext.js.require("styles/remove-confirm-window.scss")
        }
    }

    override fun RBuilder.render() {
        div("delete-popover") {
            h5 {
                +"Confirm deletion"
            }
            p {
                +"Are you sure you want to delete it?"
            }
            div("delete-popover--buttons") {
                Button {
                    attrs {
                        onClick = {
                            props.onConfirm()
                        }
                        className = "pt-small pt-intent-danger pt-popover-dismiss"
                    }
                    +"Delete"
                }
                Button {
                    attrs {
                        className = "pt-small pt-popover-dismiss"
                    }
                    +"Cancel"
                }
            }
        }
    }

    interface Props : RProps {
        var onConfirm: () -> Unit
    }
}

fun RBuilder.deletePopoverWindow(block: RHandler<DeletePopoverWindow.Props>) =
    child(DeletePopoverWindow::class, block)
