package com.infowings.catalog.aspects.editconsole.view

import com.infowings.catalog.aspects.editconsole.popup.removeConfirmationWindow
import com.infowings.catalog.utils.ripIcon
import com.infowings.catalog.wrappers.blueprint.Popover
import com.infowings.catalog.wrappers.blueprint.PopoverInteractionKind
import com.infowings.catalog.wrappers.blueprint.Position
import react.*
import react.dom.div

class DeleteButton : RComponent<DeleteButton.Props, RState>() {

    override fun RBuilder.render() {
        Popover {
            attrs {
                interactionKind = PopoverInteractionKind.CLICK
                position = Position.TOP
                content = buildElement {
                    removeConfirmationWindow {
                        attrs {
                            onConfirm = {
                                props.onDeleteClick()
                            }
                        }
                    }
                }!!
            }
            div(classes = "aspect-edit-console--button-control") {
                ripIcon("aspect-edit-console--button-icon aspect-edit-console--button-icon__red") {}
            }
        }
    }

    interface Props : RProps {
        var onDeleteClick: () -> Unit
    }

}

fun RBuilder.deleteButton(handler: RHandler<DeleteButton.Props>) = child(DeleteButton::class, handler)