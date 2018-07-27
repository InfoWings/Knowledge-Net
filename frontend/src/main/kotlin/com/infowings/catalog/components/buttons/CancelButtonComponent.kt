package com.infowings.catalog.components.buttons

import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.Intent
import react.RBuilder

fun RBuilder.cancelButtonComponent(onSubmit: () -> Unit, className: String? = null) =
    Button {
        attrs {
            this.className = listOfNotNull("pt-minimal", className).joinToString(" ")
            onClick = { onSubmit() }
            intent = Intent.DANGER
            icon = "cross"
        }
    }
