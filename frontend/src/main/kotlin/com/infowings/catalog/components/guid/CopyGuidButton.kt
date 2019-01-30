package com.infowings.catalog.components.guid

import com.infowings.catalog.wrappers.blueprint.Icon
import com.infowings.catalog.wrappers.blueprint.Tooltip
import com.infowings.catalog.wrappers.react.asReactElement
import react.RBuilder
import react.dom.span

private const val GUID_NOT_PROVIDED_PLACEHOLDER = "Guid is not provided"

fun RBuilder.copyGuidButton(text: String?) {
    Tooltip {
        text?.let {
            attrs {
                content = text.asReactElement()
            }
            span(classes = "bp3-button bp3-minimal bp3-small copy-entity-guid-btn") {
                Icon {
                    attrs {
                        icon = "clipboard"
                    }
                }

                setProp("data-clipboard-text", text)
            }
        } ?: run {
            attrs {
                content = GUID_NOT_PROVIDED_PLACEHOLDER.asReactElement()
            }
/*            button(type = ButtonType.button, classes = "bp3-dark-button bp3-minimal bp3-small bp3-icon-clipboard copy-entity-guid-btn") {
              //  setProp("data-clipboard-text", text)
            }
*/
            span(classes = "bp3-dark-button bp3-minimal bp3-small copy-entity-guid-btn") {
                Icon {
                    attrs {
                        icon = "clipboard"
                    }
                }
            }
        }
    }
}

