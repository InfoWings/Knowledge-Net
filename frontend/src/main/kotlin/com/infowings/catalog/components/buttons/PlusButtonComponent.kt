package com.infowings.catalog.components.buttons

import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.Intent
import react.RBuilder

fun RBuilder.plusButtonComponent(onSubmit: () -> Unit, className: String? = null) =
    Button {
        attrs {
            this.className = listOfNotNull("bp3-minimal", className).joinToString(" ")
            onClick = { onSubmit() }
            intent = Intent.SUCCESS
            icon = "plus"
        }
    }