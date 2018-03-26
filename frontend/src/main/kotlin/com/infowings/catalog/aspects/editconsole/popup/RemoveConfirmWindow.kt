package com.infowings.catalog.aspects.editconsole.popup

import kotlinext.js.invoke
import kotlinext.js.require
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.button
import react.dom.div
import react.dom.h3
import react.dom.p

class RemoveConfirmWindow : RComponent<RemoveConfirmWindow.Props, RState>() {

    companion object {
        init {
            require("styles/RemoveConfirmWindow.scss")
        }
    }

    override fun RBuilder.render() {
        div("popup-container") {
            h3 { +"This aspect is not free" }
            p { +"Are you sure you want to delete it?" }

            div("button-area") {
                button {
                    attrs {
                        onClickFunction = { props.onConfirm() }
                    }
                    +"Yes"
                }

                button {
                    attrs {
                        onClickFunction = { props.onCancel() }
                    }
                    +"No"
                }
            }

        }
    }

    interface Props : RProps {
        var onConfirm: () -> Unit
        var onCancel: () -> Unit
    }
}

fun RBuilder.removeConfirmWindow(block: RHandler<RemoveConfirmWindow.Props>) = child(RemoveConfirmWindow::class, block)

