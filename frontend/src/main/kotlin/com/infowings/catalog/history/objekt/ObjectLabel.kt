package com.infowings.catalog.history.objekt

import kotlinx.html.js.onClickFunction
import react.RBuilder
import react.dom.div
import react.dom.span

fun RBuilder.objectLabel(
    className: String?,
    name: String,
    description: String,
    subjectName: String,
    onClick: () -> Unit
) = div(classes = "refbook-view--label${className?.let { " $it" } ?: ""}") {
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
        +description
    }
    +"]"
    +" Aspect name: "
    span(classes = "text-grey") {
        +subjectName
    }
}
