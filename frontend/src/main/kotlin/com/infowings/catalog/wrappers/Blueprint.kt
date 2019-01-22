package com.infowings.catalog.wrappers

import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.ButtonProps
import react.RBuilder


fun RBuilder.button(builder: ButtonProps.() -> Unit) = Button {
    attrs { builder.invoke(this) }
}