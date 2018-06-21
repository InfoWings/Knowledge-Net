package com.infowings.catalog.objects.treeview.format

import com.infowings.catalog.common.GlobalMeasureMap
import react.RProps
import react.dom.div
import react.dom.span
import react.rFunction

val objectPropertyValueLineFormat = rFunction<ObjectPropertyValueLineFormatProps>("ObjectPropertyValueLineFormat") { props ->
    div("object-property-value-line") {
        props.propertyName?.let {
            span(classes = "text-italic text-bold object-property-value-line__role-name") {
                +it
            }
        }
        span(classes = "text-bold object-property-value-line__aspect-name") {
            +props.aspectName
        }
        props.valueRepresentation?.let {
            +": "
            span(classes = "object-property-value-line__value") {
                +it
            }
            props.measure?.let {
                span(classes = "object-property-value-line__value-measure") {
                    +(GlobalMeasureMap[it]?.symbol ?: error("No such measure")) // TODO: Fetch from server
                }
            }
        }
    }
}

interface ObjectPropertyValueLineFormatProps : RProps {
    var propertyName: String?
    var aspectName: String
    var valueRepresentation: String?
    var measure: String?
}