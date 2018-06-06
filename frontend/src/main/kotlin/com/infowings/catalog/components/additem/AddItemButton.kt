package com.infowings.catalog.components.additem

import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.Intent
import react.RBuilder

fun RBuilder.addPropertyButton(className: String? = null, onClick: () -> Unit) =
    Button {
        attrs {
            this.className = "pt-minimal${className?.let { " $it" } ?: ""}"
            this.onClick = { onClick() }
            intent = Intent.SUCCESS
            icon = "new-link"
        }
    }
