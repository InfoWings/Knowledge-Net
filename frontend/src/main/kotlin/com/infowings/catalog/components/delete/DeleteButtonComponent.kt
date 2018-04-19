package com.infowings.catalog.components.delete

import com.infowings.catalog.utils.ripIcon
import com.infowings.catalog.wrappers.blueprint.*
import com.infowings.catalog.wrappers.react.asReactElement
import kotlinext.js.require
import react.*

class DeleteButtonComponent : RComponent<DeleteButtonComponent.Props, RState>() {

    companion object {
        init {
            require("styles/delete-button.scss")
        }
    }

    override fun RBuilder.render() {
        Popover {
            attrs {
                props.position?.let { position = it }
                content = buildElement {
                    deletePopoverWindow {
                        attrs {
                            onConfirm = {
                                props.onDeleteClick()
                            }
                        }
                    }
                }!!
            }
            Tooltip {
                attrs {
                    content = "Delete ${props.entityName}".asReactElement()
                }
                Button {
                    attrs {
                        className = "pt-minimal${props.className?.let { " $it" } ?: ""}"
                        intent = Intent.DANGER
                    }
                    ripIcon("delete-button--icon") {}
                }
            }
        }
    }

    interface Props : RProps {
        var className: String?
        var entityName: String
        var position: Position?
        var onDeleteClick: () -> Unit
    }
}

fun RBuilder.deleteButtonComponent(
    onDeleteClick: () -> Unit,
    entityName: String,
    className: String? = null,
    position: Position? = null
) =
    child(DeleteButtonComponent::class) {
        attrs.entityName = entityName
        attrs.onDeleteClick = onDeleteClick
        attrs.className = className
        attrs.position = position
    }
