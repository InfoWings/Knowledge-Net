package com.infowings.catalog.aspects.treeview.view

import kotlinx.html.js.onClickFunction
import react.RBuilder
import react.dom.div
import react.dom.span

fun RBuilder.aspectLabel(
    className: String?,
    aspectName: String,
    aspectMeasure: String,
    aspectDomain: String,
    aspectBaseType: String,
    aspectSubjectName: String,
    onClick: () -> Unit
) = div(classes = "aspect-tree-view--label${className?.let { " $it" } ?: ""}") {
    attrs {
        onClickFunction = {
            it.stopPropagation()
            it.preventDefault()
            onClick()
        }
    }
    span(classes = "aspect-tree-view--label-name") {
        +aspectName
    }
    +":"
    span(classes = "aspect-tree-view--label-measure") {
        +aspectMeasure
    }
    +":"
    span(classes = "aspect-tree-view--label-domain") {
        +aspectDomain
    }
    +":"
    span(classes = "aspect-tree-view--label-base-type") {
        +aspectBaseType
    }
    +"( Subject: "
    span(classes = "aspect-tree-view--label-subject") {
        +aspectSubjectName
    }
    +" )"
}

fun RBuilder.placeholderAspectLabel(className: String?) =
        div(classes = "aspect-tree-view--label${className?.let { " $it" } ?: ""}") {
            span(classes = "aspect-tree-view--empty") {
                +"(Enter New Aspect)"
            }
        }