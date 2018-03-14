package com.infowings.catalog.aspects.editconsole.aspectproperty

import com.infowings.catalog.wrappers.react.label
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.input

class AspectPropertyNameInput : RComponent<AspectPropertyNameInput.Props, RState>() {

    private fun handleInputFieldChanged(e: Event) {
        e.stopPropagation()
        e.preventDefault()
        console.log(e.target.unsafeCast<HTMLInputElement>().value)
    }

    override fun RBuilder.render() {
        div(classes = "aspect-edit-console--input-container") {
            label(classes = "aspect-edit-console--input-label", htmlFor = "aspect-property-name") {
                +"Property Name"
            }
            div(classes = "aspect-edit-console--input-wrapper") {
                input(type = InputType.text, name = "property-name", classes = "aspect-edit-console--input") {
                    attrs {
                        id = "aspect-property-name"
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

fun RBuilder.aspectPropertyNameInput(block: RHandler<AspectPropertyNameInput.Props>) = child(AspectPropertyNameInput::class, block)