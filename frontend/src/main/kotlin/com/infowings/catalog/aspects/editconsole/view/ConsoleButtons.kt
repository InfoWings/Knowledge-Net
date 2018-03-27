package com.infowings.catalog.aspects.editconsole.view

import com.infowings.catalog.utils.addToListIcon
import com.infowings.catalog.utils.checkIcon
import com.infowings.catalog.utils.crossIcon
import kotlinx.html.js.onClickFunction
import react.RBuilder
import react.dom.div

fun RBuilder.consoleButtonsGroup(
        onSubmitClick: () -> Unit,
        onAddToListClick: () -> Unit,
        onCancelClick: () -> Unit
) {
    div(classes = "aspect-edit-console--button-control-tab") {
        div(classes = "aspect-edit-console--button-control") {
            attrs {
                onClickFunction = { e ->
                    e.stopPropagation()
                    e.preventDefault()
                    onSubmitClick()
                }
            }
            checkIcon("aspect-edit-console--button-icon aspect-edit-console--button-icon__green") {}
        }
        div(classes = "aspect-edit-console--button-control") {
            attrs {
                onClickFunction = { e ->
                    e.stopPropagation()
                    e.preventDefault()
                    onAddToListClick()
                }
            }
            addToListIcon("aspect-edit-console--button-icon aspect-edit-console--button-icon__green") {}
        }
        div(classes = "aspect-edit-console--button-control") {
            attrs {
                onClickFunction = { e ->
                    e.stopPropagation()
                    e.preventDefault()
                    onCancelClick()
                }
            }
            crossIcon("aspect-edit-console--button-icon aspect-edit-console--button-icon__red") {}
        }
    }
}