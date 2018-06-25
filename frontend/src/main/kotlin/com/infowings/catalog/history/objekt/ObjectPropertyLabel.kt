package com.infowings.catalog.history.objekt

import kotlinx.html.js.onClickFunction
import react.RBuilder
import react.dom.div
import react.dom.span

fun RBuilder.objectPropertyLabel(
    className: String?,
    name: String,
    cardinality: String,
    aspectName: String,
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
        +name
    }
    +"["
    span(classes = "text-grey") {
        +cardinality
    }
    +"]"
    +" Aspect name: "
    span(classes = "text-grey") {
        +aspectName
    }
}
