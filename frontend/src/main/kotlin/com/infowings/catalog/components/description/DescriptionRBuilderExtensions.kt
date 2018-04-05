package com.infowings.catalog.components.description

import com.infowings.catalog.utils.descriptionIcon
import com.infowings.catalog.wrappers.blueprint.Tooltip
import react.RBuilder
import react.buildElement
import react.dom.p
import react.dom.span

fun RBuilder.descriptionTooltip(className: String?, description: String?) =
    Tooltip {
        attrs.tooltipClassName = "description-tooltip"
        attrs.content = buildElement {
            span {
                (description?.let {
                    if (it.isEmpty())
                        listOf(span { +"Description is not provided" })
                    else
                        it.split("\n").map { p { +it } }
                } ?: listOf(span { +"Description is not provided" }))
            }

        }
        descriptionIcon(classes = className)
    }