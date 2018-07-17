package com.infowings.catalog.components.submit

import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.Intent
import react.RBuilder

fun RBuilder.expandTreeButtonComponent(onSubmit: () -> Unit, className: String? = null) =
    Button {
        attrs {
            this.className = "pt-minimal${className?.let { " $it" } ?: ""}"
            onClick = { onSubmit() }
            intent = Intent.NONE
            icon = "chevron-down"
        }
    }
