package com.infowings.catalog.aspects.editconsole.popup

import com.infowings.catalog.wrappers.blueprint.Button
import kotlinext.js.require
import react.*
import react.dom.div
import react.dom.h5
import react.dom.p

class RemoveConfirmationWindow : RComponent<RemoveConfirmationWindow.Props, RState>() {

    companion object {
        init {
            require("styles/remove-confirm-window.scss")
        }
    }

    override fun RBuilder.render() {
        div("remove-confirm-window") {
            h5 {
                +"Confirm deletion"
            }
            p {
                +"Are you sure you want to delete it?"
            }
            div("remove-confirm-window--buttons") {
                Button {
                    attrs {
                        className = "pt-small pt-popover-dismiss"
                    }
                    +"Cancel"
                }
                Button {
                    attrs {
                        onClick = {
                            props.onConfirm()
                        }
                        className = "pt-small pt-intent-danger pt-popover-dismiss"
                    }
                    +"Delete"
                }
            }
        }
    }

    interface Props : RProps {
        var onConfirm: () -> Unit
    }
}

fun RBuilder.removeConfirmationWindow(block: RHandler<RemoveConfirmationWindow.Props>) =
    child(RemoveConfirmationWindow::class, block)