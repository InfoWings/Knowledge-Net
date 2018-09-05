package com.infowings.catalog.components.guid

import com.infowings.catalog.wrappers.blueprint.Tooltip
import com.infowings.catalog.wrappers.react.asReactElement
import kotlinx.html.ButtonType
import react.RBuilder
import react.dom.button

fun RBuilder.copyGuidButton(text: String) {
    Tooltip {
        attrs {
            content = text.asReactElement()
        }
        button(type = ButtonType.button, classes = "pt-button pt-minimal pt-small pt-icon-clipboard copy-entity-guid-btn") {
            setProp("data-clipboard-text", text)
        }
    }
}

