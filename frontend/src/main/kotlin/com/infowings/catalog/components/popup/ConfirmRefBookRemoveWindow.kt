package com.infowings.catalog.components.popup

import com.infowings.catalog.wrappers.blueprint.Alert
import com.infowings.catalog.wrappers.blueprint.Intent
import react.*
import react.dom.div
import react.dom.h3
import react.dom.p

class ConfirmRefBookRemovalWindow : RComponent<ConfirmRefBookRemovalWindow.Props, RState>() {

    override fun RBuilder.render() {
        Alert {
            attrs {
                onCancel = {
                    it.preventDefault()
                    it.stopPropagation()
                    props.onCancel()
                }
                intent = Intent.DANGER
                onConfirm = {
                    it.preventDefault()
                    it.stopPropagation()
                    props.onConfirm()
                }
                cancelButtonText = "Cancel"
                confirmButtonText = "Confirm"
                isOpen = props.isOpen
            }
            div {
                h3 { +props.message }
                p { +"Are you sure you want to delete it?" }
            }
        }
    }

    interface Props : RProps {
        var onConfirm: () -> Unit
        var onCancel: () -> Unit
        var isOpen: Boolean
        var message: String
    }
}

fun RBuilder.confirmRefBookRemovalWindow(block: RHandler<ConfirmRefBookRemovalWindow.Props>) =
    child(ConfirmRefBookRemovalWindow::class, block)