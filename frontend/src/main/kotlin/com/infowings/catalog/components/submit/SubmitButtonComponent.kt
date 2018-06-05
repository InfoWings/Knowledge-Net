package com.infowings.catalog.components.submit

import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.Intent
import react.RBuilder

fun RBuilder.submitButtonComponent(onSubmit: () -> Unit, className: String? = null) =
    Button {
        attrs {
            this.className = "pt-minimal${className?.let { " $it" } ?: ""}"
            onClick = { onSubmit() }
            intent = Intent.SUCCESS
            icon = "tick"
        }
    }