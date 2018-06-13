package com.infowings.catalog.history.refbook

import kotlinx.html.js.onClickFunction
import react.RBuilder
import react.dom.div
import react.dom.span

fun RBuilder.refBookItemLabel(
    className: String?,
    name: String,
    description: String,
    onClick: () -> Unit
) = div(classes = "refbook-view--label${className?.let { " $it" } ?: ""}") {
    attrs {
        onClickFunction = {
            it.stopPropagation()
            it.preventDefault()
            onClick()
        }
    }
    "Item: " +
            span(classes = "text-bold") {
                +name
            }
    +"["
    span(classes = "text-grey") {
        +description
    }
    +"]"
}
