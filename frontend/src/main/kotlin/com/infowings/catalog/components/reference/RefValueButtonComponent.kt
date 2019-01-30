package com.infowings.catalog.components.reference


import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.utils.linkIcon
import com.infowings.catalog.wrappers.History
import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.Intent
import com.infowings.catalog.wrappers.blueprint.Tooltip
import org.w3c.dom.events.MouseEvent
import react.*
import react.dom.div
import react.dom.span

class RefValueButtonComponent : RComponent<RefValueButtonComponent.Props, RefValueButtonComponent.State>() {

    companion object {
        init {
            kotlinext.js.require("styles/reference.scss")
        }
    }

    override fun RefValueButtonComponent.State.init(props: RefValueButtonComponent.Props) {
        opened = false
    }

    fun onClick(event: MouseEvent) {
        event.stopPropagation()
        event.preventDefault()
        props.history.push("http://www.rbc.ru")
    }

    override fun RBuilder.render() {
        div {
            Tooltip {
                attrs.tooltipClassName = "reference-tooltip"
                attrs.content = buildElement {
                    span {
                        +"Linked entities"
                    }
                }
                Button {
                    attrs {
                        className = "bp3-minimal"
                        intent = Intent.PRIMARY
                        onClick = ::onClick
                    }
                    linkIcon("link-button--icon") {}
                }
            }
        }
    }

    interface State : RState {
        var opened: Boolean
    }

    interface Props : RProps {
        var subject: SubjectData
        var history: History
    }
}

fun RBuilder.refValueButtonComponent(subject: SubjectData, history: History) = child(RefValueButtonComponent::class) {
    attrs {
        this.subject = subject
        this.history = history
    }
}