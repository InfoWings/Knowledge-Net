package com.infowings.catalog.aspects.treeview.view

import com.infowings.catalog.common.AspectPropertyCardinality
import com.infowings.catalog.utils.ripIcon
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
    isSubjectDeleted: Boolean,
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
        span(classes = "text-bold text-italic aspect-tree-view--label-property-name") {
            +aspectPropertyName
        }
    }
    span(classes = "text-bold") {
        +aspectName
    }
    +":"
    span(classes = "text-grey") {
        +"["
        span(classes = "aspect-tree-view--label-property-cardinality") {
            +AspectPropertyCardinality.valueOf(aspectPropertyCardinality).label
        }
        +"]"
    }
    +":"
    span(classes = "text-grey") {
        +aspectMeasure
    }
    +":"
    span(classes = "text-grey") {
        +aspectDomain
    }
    +":"
    span(classes = "text-grey") {
        +aspectBaseType
    }
    +"( Subject: "
    span(classes = "text-grey") {
        +aspectSubjectName
    }
    if (isSubjectDeleted) {
        ripIcon("aspect-tree-view--rip-icon") {}
    }
    +" )"
}

fun RBuilder.placeholderPropertyLabel(className: String?) =
    div(classes = "aspect-tree-view--label${className?.let { " $it" } ?: ""}") {
        +"(Enter new Aspect Property)"
    }
