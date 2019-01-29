package com.infowings.catalog.wrappers

import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.ButtonProps
import com.infowings.catalog.wrappers.blueprint.NumericInput
import com.infowings.catalog.wrappers.blueprint.NumericInputProps
import react.RBuilder


fun RBuilder.button(builder: ButtonProps.() -> Unit) = Button {
    attrs { builder.invoke(this) }
}

fun RBuilder.numericInput(builder: NumericInputProps.() -> Unit) = NumericInput {
    attrs { builder.invoke(this) }
}
