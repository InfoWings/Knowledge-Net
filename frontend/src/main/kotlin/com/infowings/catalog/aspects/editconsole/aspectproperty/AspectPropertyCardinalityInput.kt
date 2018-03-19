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

class AspectPropertyCardinalityInput : RComponent<AspectPropertyCardinalityInput.Props, RState>() {

    private fun handleInputFieldChanged(e: Event) {
        e.stopPropagation()
        e.preventDefault()
        props.onChange(e.target.unsafeCast<HTMLInputElement>().value)
    }

    override fun RBuilder.render() {
        div(classes = "aspect-edit-console--aspect-property-input-container") {
            label(classes = "aspect-edit-console--input-label", htmlFor = "aspect-property-cardinality") {
                +"Cardinality"
            }
            div(classes = "aspect-edit-console--input-wrapper") {
                input(type = InputType.text, name = "property-cardinality", classes = "aspect-edit-console--input") {
                    attrs {
                        id = "aspect-property-cardinality"
                        value = props.value ?: ""
                        onChangeFunction = ::handleInputFieldChanged
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var value: String?
        var onChange: (String) -> Unit
    }

}

fun RBuilder.aspectPropertyCardinality(block: RHandler<AspectPropertyCardinalityInput.Props>) = child(AspectPropertyCardinalityInput::class, block)