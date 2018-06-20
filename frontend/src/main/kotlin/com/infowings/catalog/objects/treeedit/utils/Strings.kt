package com.infowings.catalog.objects.treeedit.utils

import com.infowings.catalog.common.AspectData
import react.RBuilder
import react.dom.span

fun RBuilder.propertyAspectTypePrompt(aspect: AspectData) = with(aspect) {
    span(classes = "property-aspect-type") {
        +buildString {
            if (aspect.refBookName == null)
                domain?.let { append(it) } ?: error("Aspect must have non-null domain")
            else
                append("Reference book ${aspect.refBookName}")
            measure?.let { append(" ($it)") }
            append(" :")
        }
    }
}

fun RBuilder.propertyAspectTypeInfo(aspect: AspectData) = with(aspect) {
    span(classes = "property-aspect-type") {
        +buildString {
            if (aspect.refBookName == null)
                domain?.let { append(it) } ?: error("Aspect must have non-null domain")
            else
                append("Reference book ${aspect.refBookName}")
            measure?.let { append(" ($it)") }
        }
    }
}
