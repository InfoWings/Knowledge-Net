package com.infowings.catalog.objects.treeview.format

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
        descriptionComponent(
            className = "object-line__description",
            description = props.objectDescription
        )
        +"("
        +"Subject"
        +":"
        span(classes = "object-line__subject") {
            +props.subjectName
        }
        descriptionComponent(
            className = "object-line__subject-description",
            description = props.subjectDescription
        )
        +")"
    }
}

interface ObjectLineFormatProps : RProps {
    var objectName: String
    var objectDescription: String?
    var subjectName: String
    var subjectDescription: String?
}
