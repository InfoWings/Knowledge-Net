package com.infowings.catalog.aspects.editconsole.popup

import com.infowings.catalog.wrappers.blueprint.Alert
import react.RBuilder
import react.dom.div
import react.dom.h3
import react.dom.p

fun RBuilder.unsafeChangesWindow(isOpen: Boolean, onClick: () -> Unit) {
    Alert {
        attrs {
            this.isOpen = isOpen
            onConfirm = {
                it.preventDefault()
                it.stopPropagation()
                onClick()
            }
        }
        div {
            h3 {
                +"Unsaved changes."
            }
            p {
                +"Please save or reject current changes."
            }
        }
    }
}