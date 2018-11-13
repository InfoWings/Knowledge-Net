package com.infowings.catalog.objects.view.tree.format

import com.infowings.catalog.common.tableFormat
import com.infowings.catalog.components.description.descriptionComponent
import com.infowings.catalog.components.guid.copyGuidButton
import com.infowings.catalog.components.submit.expandTreeButtonComponent
import com.infowings.catalog.objects.ObjectLazyViewModel
import com.infowings.catalog.utils.encodeURIComponent
import com.infowings.catalog.wrappers.reactRouter
import react.RProps
import react.dom.div
import react.dom.span
import react.rFunction
import kotlin.js.Date

val objectLineFormat = rFunction<ObjectLineFormatProps>("ObjectLineFormat") { props ->
    div(classes = "object-line") {
        span(classes = "text-bold object-line__name") {
            +props.objectView.name
        }
        +"("
        +"Subject"
        +":"
        span(classes = "object-line__subject") {
            +props.objectView.subjectName
        }
        +")"

        props.objectView.lastUpdated?.let {
            span(classes = "object-line__timestamp") {
                val date = Date(it * 1000)
                +date.tableFormat()
            }
        }

        props.objectView.description?.let {
            if (it.isNotBlank()) {
                descriptionComponent(
                    className = "object-line__description",
                    description = it
                )
            }
        }

        if (props.objectView.objectPropertiesCount > 0) {
            expandTreeButtonComponent(props.expandTree, "pt-small")
        }

        copyGuidButton(props.objectView.guid)

        reactRouter.Link {
            attrs {
                className = "object-line__edit-link pt-button pt-intent-primary pt-minimal pt-icon-edit pt-small"
                role = "button"
                to = "/objects/${encodeURIComponent(props.objectView.id)}"
            }
        }
    }
}

interface ObjectLineFormatProps : RProps {
    var objectView: ObjectLazyViewModel
    var expandTree: () -> Unit
}
