package com.infowings.catalog.components.popup

import kotlinext.js.invoke
import kotlinext.js.require
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.button
import react.dom.div
import react.dom.h3
import react.dom.p

class ConfirmWindow : RComponent<ConfirmWindow.Props, RState>() {

    companion object {
        init {
            require("styles/confirm-window.scss")
        }
    }

    override fun RBuilder.render() {
        div("popup-container") {
            h3 { +props.message }
            p { +"Are you sure you want to ${props.action.name.toLowerCase()} it?" }

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
        var action: Action
        var message: String
        var onConfirm: () -> Unit
        var onCancel: () -> Unit
    }

    enum class Action {
        DELETE,
        UPDATE
    }
}

fun RBuilder.confirmWindow(block: RHandler<ConfirmWindow.Props>) = child(ConfirmWindow::class, block)

