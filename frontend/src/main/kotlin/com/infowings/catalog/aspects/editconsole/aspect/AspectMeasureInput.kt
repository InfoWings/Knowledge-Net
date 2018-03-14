package com.infowings.catalog.aspects.editconsole.aspect

import com.infowings.catalog.wrappers.react.label
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.input

class AspectMeasureInput : RComponent<AspectMeasureInput.Props, RState>() {

    private fun handleInputFieldChanged(e: Event) {
        e.stopPropagation()
        e.preventDefault()
    }

    override fun RBuilder.render() {
        div(classes = "aspect-edit-console--input-container") {
            label(classes = "aspect-edit-console--input-label", htmlFor = "aspect-measure") {
                +"Measure"
            }
            div(classes = "aspect-edit-console--input-wrapper") {
                input(type = InputType.text, name = "measure", classes = "aspect-edit-console--input") {
                    attrs {
                        id = "aspect-measure"
                        value = props.initialValue ?: ""
                        onChangeFunction = ::handleInputFieldChanged
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var initialValue: String?
    }

}

fun RBuilder.aspectMeasureInput(block: RHandler<AspectMeasureInput.Props>) = child(AspectMeasureInput::class, block)