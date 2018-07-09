package com.infowings.catalog.objects.view.tree.format

import com.infowings.catalog.components.description.descriptionComponent
import com.infowings.catalog.utils.encodeURIComponent
import com.infowings.catalog.wrappers.reactRouter
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
        reactRouter.Link {
            attrs {
                className = "object-line__edit-link pt-button pt-intent-primary pt-minimal pt-icon-edit pt-small"
                role = "button"
                to = "/objects/${encodeURIComponent(props.objectId)}"
            }
        }
    }
}

interface ObjectLineFormatProps : RProps {
    var objectId: String
    var objectName: String
    var objectDescription: String?
    var subjectName: String
}
