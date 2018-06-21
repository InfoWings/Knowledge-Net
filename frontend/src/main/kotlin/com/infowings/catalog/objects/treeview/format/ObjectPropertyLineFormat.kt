package com.infowings.catalog.objects.treeview.format

import com.infowings.catalog.common.PropertyCardinality
import react.RProps
import react.dom.div
import react.dom.span
import react.rFunction

val objectPropertyLineFormat = rFunction<ObjectPropertyLineFormatProps>("ObjectPropertyLineFormat") { props ->
    div(classes = "object-property-line") {
        props.name?.let {
            span(classes = "text-italic text-bold object-property-line__role-name") {
                +it
            }
        }
        span(classes = "text-bold object-property-line__aspect-name") {
            +props.aspectName
        }
        span(classes = "object-property-line__cardinality") {
            +"["
            +props.cardinality.label
            +"]"
        }
    }
}

interface ObjectPropertyLineFormatProps : RProps {
    var name: String?
    var aspectName: String
    var cardinality: PropertyCardinality
}
