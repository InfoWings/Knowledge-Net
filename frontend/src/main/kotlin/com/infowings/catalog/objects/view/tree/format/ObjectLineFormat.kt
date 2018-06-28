package com.infowings.catalog.objects.view.tree.format

import com.infowings.catalog.components.description.descriptionComponent
import react.RProps
import react.dom.div
import react.dom.span
import react.rFunction

val objectLineFormat = rFunction<ObjectLineFormatProps>("ObjectLineFormat") { props ->
    div(classes = "object-line") {
        span(classes = "text-bold object-line__name") {
            +props.objectName
        }
        +"("
        +"Subject"
        +":"
        span(classes = "object-line__subject") {
            +props.subjectName
        }
        +")"
        props.objectDescription?.let {
            if (it.isNotBlank()) {
                descriptionComponent(
                    className = "object-line__description",
                    description = it
                )
            }
        }
    }
}

interface ObjectLineFormatProps : RProps {
    var objectName: String
    var objectDescription: String?
    var subjectName: String
}
