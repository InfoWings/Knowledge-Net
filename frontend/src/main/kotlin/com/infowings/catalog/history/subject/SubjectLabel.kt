package com.infowings.catalog.history.subject

import kotlinx.html.js.onClickFunction
import react.RBuilder
import react.dom.div
import react.dom.span

fun RBuilder.subjectLabel(
    className: String?,
    name: String,
    description: String,
    onClick: () -> Unit
) = div(classes = "subject-view--label${className?.let { " $it" } ?: ""}") {
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
}
