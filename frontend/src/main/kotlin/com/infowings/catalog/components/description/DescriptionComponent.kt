package com.infowings.catalog.components.description

import com.infowings.catalog.utils.descriptionIcon
import com.infowings.catalog.wrappers.blueprint.*
import kotlinext.js.invoke
import kotlinext.js.require
import react.*
import react.dom.div
import react.dom.p
import react.dom.span

class DescriptionComponent(props: Props) : RComponent<DescriptionComponent.Props, DescriptionComponent.State>(props) {

    companion object {
        init {
            require("styles/description-popover.scss")
        }
    }

    override fun State.init(props: Props) {
        value = props.description ?: ""
        editing = false
        opened = false
    }

    override fun componentWillReceiveProps(nextProps: Props) {
        setState {
            value = nextProps.description ?: ""
        }
    }

    private fun RBuilder.editableInput() =
        div(classes = "description-input--container") {
            EditableText {
                attrs {
                    className = "description-input--editable-text"
                    multiline = true
                    minLines = 2
                    isEditing = state.editing
                    value = state.value
                    onEdit = {
                        setState {
                            editing = true
                        }
                    }
                    onChange = {
                        setState {
                            value = it
                        }
                    }
                    onCancel = {
                        setState {
                            value = props.description ?: ""
                            editing = false
                        }
                    }
                    onConfirm = {
                        props.onNewDescriptionConfirmed(it)
                        setState {
                            editing = false
                            opened = false
                        }
                    }
                }
            }
            Button {
                attrs {
                    className = "pt-small description-input--confirm"
                    intent = Intent.SUCCESS
                    icon = "tick"
                    onClick = {
                        props.onNewDescriptionConfirmed(state.value)
                        setState {
                            editing = false
                            opened = false
                        }
                    }
                }
            }
        }

    override fun RBuilder.render() {
        Popover {
            attrs {
                popoverClassName = "description-popover"
                content = buildElement { editableInput() }
                isOpen = state.opened
                onInteraction = {
                    setState {
                        editing = it
                        opened = it
                    }
                    props.onEditStarted?.invoke()
                }
            }
            Tooltip {
                attrs.tooltipClassName = "description-tooltip"
                attrs.content = buildElement {
                    span {
                        (props.description?.let {
                            if (it.isEmpty())
                                listOf(span { +"Description is not provided" })
                            else
                                it.split("\n").map { p { +it } }
                        } ?: listOf(span { +"Description is not provided" }))
                    }

                }
                descriptionIcon(classes = props.className)
            }
        }
    }

    interface Props : RProps {
        var className: String?
        var description: String?
        var onNewDescriptionConfirmed: (String) -> Unit
        var onEditStarted: (() -> Unit)?
    }

    interface State : RState {
        var value: String
        var editing: Boolean
        var opened: Boolean
    }
}

fun RBuilder.descriptionComponent(block: RHandler<DescriptionComponent.Props>) =
    child(DescriptionComponent::class, block)