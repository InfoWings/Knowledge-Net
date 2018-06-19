package com.infowings.catalog.history.objekt

import kotlinx.html.js.onClickFunction
import react.RBuilder
import react.dom.div
import react.dom.span

fun RBuilder.objectValueLabel(
    className: String?,
    repr: String,
    typeTag: String,
    aspectPropertyName: String,
    measureName: String,
    onClick: () -> Unit
) = div(classes = "object-view--label${className?.let { " $it" } ?: ""}") {
    attrs {
        onClickFunction = {
            it.stopPropagation()
            it.preventDefault()
            onClick()
        }
    }
    span(classes = "text-bold") {
        +repr
    }
    +"[["
    span(classes = "text-grey") {
        +typeTag
    }
    +"]]"
    +", Aspect Property:"
    span(classes = "text-grey") {
        +aspectPropertyName
    }
    +", Measure: "
    span(classes = "text-grey") {
        +measureName
    }
}
