package com.infowings.catalog.components.buttons

import com.infowings.catalog.components.delete.deletePopoverWindow
import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.Intent
import com.infowings.catalog.wrappers.blueprint.Popover
import react.RBuilder
import react.buildElement

fun RBuilder.minusButtonComponent(onSubmit: () -> Unit, confirmation: Boolean = false, className: String? = null) =
    if (!confirmation) {
        Button {
            attrs {
                this.className = "pt-minimal${className?.let { " $it" } ?: ""}"
                onClick = { onSubmit() }
                intent = Intent.DANGER
                icon = "minus"
            }
        }
    } else {
        Popover {
            attrs {
                content = buildElement {
                    deletePopoverWindow {
                        attrs {
                            onConfirm = onSubmit
                        }
                    }
                }!!
            }
            Button {
                attrs {
                    this.className = "pt-minimal${className?.let { " $it" } ?: ""}"
                    intent = Intent.DANGER
                    icon = "minus"
                }
            }
        }
    }
