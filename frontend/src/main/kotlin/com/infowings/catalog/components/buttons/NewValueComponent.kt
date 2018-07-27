package com.infowings.catalog.components.buttons

import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.Intent
import com.infowings.catalog.wrappers.react.asReactElement
import react.RBuilder

fun RBuilder.newValueButtonComponent(onSubmit: () -> Unit, className: String? = null) =
    Button {
        attrs {
            this.className = "pt-minimal${className?.let { " $it" } ?: ""}"
            onClick = { onSubmit() }
            intent = Intent.SUCCESS
            text = "New Value".asReactElement()
        }
    }