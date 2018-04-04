package com.infowings.catalog.aspects.editconsole.aspect

import com.infowings.catalog.utils.ripIcon
import com.infowings.catalog.wrappers.blueprint.Alert
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.div

class AspectRemove : RComponent<RProps, AspectRemove.State>() {

    override fun State.init() {
        confirmation = false
    }

    private fun onDeleteClick() {
        setState {
            confirmation = true
        }
    }

    override fun RBuilder.render() {
        div(classes = "aspect-edit-console--button-control") {
            attrs.onClickFunction = { e ->
                e.stopPropagation()
                e.preventDefault()
                onDeleteClick()
            }
            ripIcon("aspect-edit-console--button-icon aspect-edit-console--button-icon__red") {}
        }
        Alert {
            attrs {
                onCancel = {
                    it.preventDefault()
                    it.stopPropagation()
                    setState { confirmation = false }
                }
                cancelButtonText = "Cancel"
                isOpen = state.confirmation
            }
            +"Are you sure to delete this Aspect?"
        }
    }

    interface State : RState {
        var confirmation: Boolean
    }
}

fun RBuilder.aspectRemove(handler: RHandler<RProps>) = child(AspectRemove::class, handler)