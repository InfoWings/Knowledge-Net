package com.infowings.catalog.objects.view.tree.format

import com.infowings.catalog.common.ObjectValueData
import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.components.description.descriptionComponent
import com.infowings.catalog.components.guid.copyGuidButton
import com.infowings.catalog.components.reference.refValueButtonComponent
import com.infowings.catalog.components.reference.referenceButtonComponent
import com.infowings.catalog.wrappers.RouteSuppliedProps
import react.RProps
import react.dom.div
import react.dom.span
import react.rFunction

val objectPropertyValueLineFormat = rFunction<ObjectPropertyValueLineFormatProps>("ObjectPropertyValueLineFormat") { props ->
    div("object-property-value-line") {
        props.propertyName?.let {
            if (it.isNotBlank()) {
                span(classes = "text-italic text-bold object-property-value-line__role-name") {
                    +it
                }
            }
        }
        span(classes = "text-bold object-property-value-line__aspect-name") {
            +props.aspectName
        }
        span(classes = "object-property-value-line__aspect-subject-name") {
            +"(${props.subjectName ?: "Global"})"
        }
        props.propertyDescription?.let {
            if (it.isNotBlank()) {
                descriptionComponent(
                    className = "object-property-value-line__property-description",
                    description = it
                )
            }
        }
        valueFormat(props.value, props.history)

        props.measureSymbol?.let {
            if (props.value != ObjectValueData.NullValue) {
                span(classes = "object-property-value-line__value-measure") {
                    +it
                }
            }
        }
        props.valueDescription?.let {
            if (it.isNotBlank()) {
                descriptionComponent(
                    className = "object-property-value-line__value-description",
                    description = it
                )
            }
        }
        copyGuidButton(props.valueGuid)
    }
}

interface ObjectPropertyValueLineFormatProps : /*RProps*/ RouteSuppliedProps {
    var propertyName: String?
    var propertyDescription: String?
    var aspectName: String
    var value: ObjectValueData
    var valueDescription: String?
    var valueGuid: String?
    var measureSymbol: String?
    var subjectName: String?
}

val aspectPropertyValueLineFormat = rFunction<AspectPropertyValueLineFormatProps>("AspectPropertyValueLineFormat") { props ->
    div("object-property-value-line") {
        props.propertyName?.let {
            if (it.isNotBlank()) {
                span(classes = "text-italic text-bold object-property-value-line__role-name") {
                    +it
                }
            }
        }
        span(classes = "text-bold object-property-value-line__aspect-name") {
            +props.aspectName
        }
        span(classes = "object-property-value-line__aspect-subject-name") {
            +"(${props.subjectName ?: "Global"})"
        }
        valueFormat(props.value, props.history)
        props.measureSymbol?.let {
            if (props.value != ObjectValueData.NullValue) {
                span(classes = "object-property-value-line__value-measure") {
                    +it
                }
            }
        }
        props.valueDescription?.let {
            if (it.isNotBlank()) {
                descriptionComponent(
                    className = "object-property-value-line__value-description",
                    description = props.valueDescription
                )
            }
        }
        copyGuidButton(props.valueGuid)
    }
}

interface AspectPropertyValueLineFormatProps : RouteSuppliedProps {
    var propertyName: String?
    var aspectName: String
    var value: ObjectValueData
    var valueDescription: String?
    var valueGuid: String?
    var measureSymbol: String?
    var subjectName: String?
}