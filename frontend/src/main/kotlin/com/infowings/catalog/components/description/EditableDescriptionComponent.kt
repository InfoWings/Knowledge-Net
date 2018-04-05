package com.infowings.catalog.components.description

import com.infowings.catalog.wrappers.blueprint.EditableText
import com.infowings.catalog.wrappers.blueprint.Popover
import kotlinext.js.invoke
import kotlinext.js.require
import kotlinx.html.js.onKeyDownFunction
import react.*
import react.dom.div

class EditableDescriptionComponent(props: Props) :
    RComponent<EditableDescriptionComponent.Props, EditableDescriptionComponent.State>(props) {

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

    private val RBuilder.editableInput
        get() =
            div(classes = "description-input--container") {
                attrs {
                    onKeyDownFunction = { it.stopPropagation() }
                }
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
                                opened = false
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
            }

    override fun RBuilder.render() {
        Popover {
            attrs {
                popoverClassName = "description-popover"
                content = buildElement { editableInput }
                isOpen = state.opened
                onInteraction = {
                    setState {
                        editing = it
                        opened = it
                    }
                    if (it) props.onEditStarted?.invoke()
                }
            }
            descriptionTooltip(props.className, props.description)
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

