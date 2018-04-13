package com.infowings.catalog.aspects.editconsole.view

import com.infowings.catalog.aspects.editconsole.popup.removeConfirmationWindow
import com.infowings.catalog.utils.ripIcon
import com.infowings.catalog.wrappers.blueprint.Popover
import com.infowings.catalog.wrappers.blueprint.PopoverInteractionKind
import com.infowings.catalog.wrappers.blueprint.Position
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.div

class DeleteButton : RComponent<DeleteButton.Props, DeleteButton.State>() {

    private fun showConfirmationPopup(nextOpenState: Boolean) {
        setState {
            confirmation = nextOpenState
        }
    }

    override fun RBuilder.render() {
        Popover {
            attrs {
                interactionKind = PopoverInteractionKind.CLICK
                isOpen = state.confirmation
                position = Position.TOP
                onInteraction = { showConfirmationPopup(it) }
                content = buildElement {
                    removeConfirmationWindow {
                        attrs {
                            onConfirm = {
                                props.onDeleteClick()
                                setState { confirmation = false }
                            }
                            onCancel = { setState { confirmation = false } }
                        }
                    }
                }!!
            }
            div(classes = "aspect-edit-console--button-control") {
                attrs.onClickFunction = { e ->
                    e.stopPropagation()
                    e.preventDefault()
                    showConfirmationPopup(true)
                }
                ripIcon("aspect-edit-console--button-icon aspect-edit-console--button-icon__red") {}
            }
        }
    }

    interface Props : RProps {
        var onDeleteClick: () -> Unit
    }

    interface State : RState {
        var confirmation: Boolean
    }
}

fun RBuilder.deleteButton(handler: RHandler<DeleteButton.Props>) = child(DeleteButton::class, handler)