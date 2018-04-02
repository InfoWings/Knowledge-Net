package com.infowings.catalog.aspects.treeview.view

import kotlinx.html.js.onClickFunction
import react.RBuilder
import react.dom.div
import react.dom.span

fun RBuilder.propertyLabel(
    className: String?,
    aspectPropertyName: String,
    aspectPropertyCardinality: String,
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
    if (aspectPropertyName.isNotEmpty()) {
        span(classes = "aspect-tree-view--label-property-name") {
            +aspectPropertyName
        }
    }
    span(classes = "aspect-tree-view--label-name") {
        +(aspectName)
    }
    +":"
    span(classes = "aspect-tree-view--label-property") {
        +"["
        span(classes = "aspect-tree-view--label-property-cardinality") {
            +cardinalityLabel(aspectPropertyCardinality)
        }
        +"]"
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

fun RBuilder.placeholderPropertyLabel(className: String?) =
        div(classes = "aspect-tree-view--label${className?.let { " $it" } ?: ""}") {
            +"(Enter new Aspect Property)"
        }

fun cardinalityLabel(cardinalityValue: String) = when (cardinalityValue) {
    "ZERO" -> "0"
    "ONE" -> "0..1"
    "INFINITY" -> "0..∞"
    else -> ""
}
