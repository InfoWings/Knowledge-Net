package com.infowings.catalog.objects.edit.tree.format

import com.infowings.catalog.common.ObjectValueData
import com.infowings.catalog.common.PropertyCardinality
import com.infowings.catalog.components.buttons.cancelButtonComponent
import com.infowings.catalog.components.buttons.minusButtonComponent
import com.infowings.catalog.components.buttons.plusButtonComponent
import com.infowings.catalog.components.submit.submitButtonComponent
import react.RProps
import react.dom.div
import react.dom.span
import react.rFunction

val aspectPropertyEditLineFormat = rFunction<AspectPropertyEditLineFormatProps>("AspectPropertyEditLineFormat") { props ->
    div("object-tree-edit__aspect-property") {
        props.propertyName?.let {
            span(classes = "aspect-property__property-name text-bold text-italic") {
                +it
            }
        }
        span(classes = "aspect-property__aspect-name text-bold") {
            +props.aspectName
        }
        span(classes = "aspect-property__aspect-subject") {
            +"("
            +(props.subjectName ?: "Global")
            +")"
        }
        span(classes = "aspect-property__property-cardinality--${if (props.conformsToCardinality) "conforms" else "conforms-not"}") {
            +"["
            +props.recommendedCardinality.label
            +"]"
        }
        props.onSubmit?.let {
            submitButtonComponent(it, "pt-small")
        }
        props.onCancel?.let {
            cancelButtonComponent(it, "pt-small")
        }
        props.onAddValue?.let {
            plusButtonComponent(it, "pt-small")
        }
        props.onRemoveValue?.let {
            minusButtonComponent(it, "pt-small")
        }
    }
}

interface AspectPropertyEditLineFormatProps : RProps {
    var propertyName: String?
    var aspectName: String
    var subjectName: String?
    var recommendedCardinality: PropertyCardinality
    var value: ObjectValueData?
    var onChange: (ObjectValueData) -> Unit
    var conformsToCardinality: Boolean
    var onSubmit: (() -> Unit)?
    var onCancel: (() -> Unit)?
    var onAddValue: (() -> Unit)?
    var onRemoveValue: (() -> Unit)?
}
