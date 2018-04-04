package com.infowings.catalog.aspects.editconsole.popup

import com.infowings.catalog.wrappers.blueprint.Alert
import react.*
import react.dom.div
import react.dom.h3
import react.dom.p

class RemoveConfirmWindow : RComponent<RemoveConfirmWindow.Props, RState>() {

    override fun RBuilder.render() {
        Alert {
            attrs {
                onCancel = {
                    it.preventDefault()
                    it.stopPropagation()
                    props.onCancel()
                }
                onConfirm = {
                    it.preventDefault()
                    it.stopPropagation()
                    props.onConfirm()
                }
                cancelButtonText = "Cancel"
                isOpen = props.isOpen
            }
            div {
                h3 { +"Aspect has linked entities." }
                p { +"Are you sure you want to delete it?" }
            }
        }
    }

    interface Props : RProps {
        var onConfirm: () -> Unit
        var onCancel: () -> Unit
        var isOpen: Boolean
    }
}

fun RBuilder.removeConfirmWindow(block: RHandler<RemoveConfirmWindow.Props>) = child(RemoveConfirmWindow::class, block)

