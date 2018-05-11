package com.infowings.catalog.objects.treeview.utils

import com.infowings.catalog.common.AspectData
import react.RBuilder
import react.dom.span

fun RBuilder.propertyAspectTypePrompt(aspect: AspectData) = with(aspect) {
    span(classes = "property-aspect-type") {
        +buildString {
            domain?.let { append(it) } ?: error("Aspect must have non-null domain")
            measure?.let { append(" ($it)") }
            append(" :")
        }
    }
}

fun RBuilder.propertyAspectTypeInfo(aspect: AspectData) = with(aspect) {
    span(classes = "property-aspect-type") {
        +buildString {
            domain?.let { append(it) } ?: error("Aspect must have non-null domain")
            measure?.let { append(" ($it)") }
        }
    }
}
