package com.infowings.catalog.components.guid

import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.Intent
import com.infowings.catalog.wrappers.blueprint.Tooltip
import com.infowings.catalog.wrappers.react.asReactElement
import kotlinx.html.ButtonType
import react.RBuilder
import react.dom.button

private const val GUID_NOT_PROVIDED_PLACEHOLDER = "Guid is not provided"

fun RBuilder.copyGuidButton(text: String?) {
    Tooltip {
        text?.let {
            attrs {
                content = text.asReactElement()
            }
            button(type = ButtonType.button, classes = "pt-button pt-minimal pt-small pt-icon-clipboard copy-entity-guid-btn") {
                setProp("data-clipboard-text", text)
            }
        } ?: run {
            attrs {
                content = GUID_NOT_PROVIDED_PLACEHOLDER.asReactElement()
            }
            Button {
                attrs {
                    className = "pt-minimal"
                    icon = "clipboard"
                    intent = Intent.DANGER
                    disabled = true
                }
            }
        }
    }
}

