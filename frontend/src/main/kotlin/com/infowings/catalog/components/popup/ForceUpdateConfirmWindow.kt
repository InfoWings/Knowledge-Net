package com.infowings.catalog.components.popup

import com.infowings.catalog.wrappers.blueprint.Alert
import com.infowings.catalog.wrappers.blueprint.Intent
import react.*
import react.dom.div
import react.dom.h3
import react.dom.p

class ForceUpdateConfirmWindow : RComponent<ForceUpdateConfirmWindow.Props, RState>() {

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
                confirmButtonText = "Update"
                isOpen = props.isOpen
            }
            div {
                h3 { +props.message }
                p { +"Are you sure you want to update it?" }
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

fun RBuilder.forceUpdateConfirmWindow(block: RHandler<ForceUpdateConfirmWindow.Props>) =
    child(ForceUpdateConfirmWindow::class, block)