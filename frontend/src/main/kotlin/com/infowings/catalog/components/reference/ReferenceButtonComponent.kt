package com.infowings.catalog.components.reference

import com.infowings.catalog.utils.linkIcon
import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.Intent
import com.infowings.catalog.wrappers.blueprint.Tooltip
import react.*
import react.dom.span

class ReferenceButtonComponent() : RComponent<RProps, ReferenceButtonComponent.State>() {

    companion object {
        init {
            kotlinext.js.require("styles/reference-button.scss")
        }
    }

    override fun ReferenceButtonComponent.State.init(props: RProps) {
        opened = false
    }

    override fun RBuilder.render() {
        Tooltip {
            attrs.tooltipClassName = "reference-tooltip"
            attrs.content = buildElement {
                span {
                    +"Click to see linked entities"
                }
            }
            Button {
                attrs {
                    className = "pt-minimal"
                    intent = Intent.PRIMARY
                }
                linkIcon("link-button--icon") {}
            }

        }
    }

    interface State : RState {
        var opened: Boolean
    }
}

fun RBuilder.referenceButtonComponent() = child(ReferenceButtonComponent::class) {}