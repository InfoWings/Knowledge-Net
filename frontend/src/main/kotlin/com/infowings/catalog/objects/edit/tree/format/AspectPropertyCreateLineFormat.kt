package com.infowings.catalog.objects.edit.tree.format

import com.infowings.catalog.common.PropertyCardinality
import com.infowings.catalog.components.buttons.newValueButtonComponent
import react.RProps
import react.dom.div
import react.dom.span
import react.rFunction

val aspectPropertyCreateLineFormat = rFunction<AspectPropertyCreateLineFormatProps>("AspectPropertyCreateLineFormat") { props ->
    div(classes = "object-tree-edit__aspect-property-empty") {
        props.propertyName?.let {
            if (it.isNotBlank()) {
                span(classes = "aspect-property-empty__property-name text-grey text-bold text-italic") {
                    +it
                }
            }
        }
        span(classes = "aspect-property-empty__aspect-name text-grey text-bold") {
            +props.aspectName
        }
        span(classes = "aspect-property-empty__aspect-subject text-grey") {
            +"("
            +(props.subjectName ?: "Global")
            +")"
        }
        props.onCreateValue?.let {
            if (props.editMode) {
                newValueButtonComponent(it, "pt-small")
            }
        }
    }
}

interface AspectPropertyCreateLineFormatProps : RProps {
    var propertyName: String?
    var aspectName: String
    var subjectName: String?
    var cardinality: PropertyCardinality
    var onCreateValue: (() -> Unit)?
    var editMode: Boolean
}
