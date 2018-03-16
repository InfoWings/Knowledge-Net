package com.infowings.catalog.aspects.editconsole.aspect

import com.infowings.catalog.wrappers.react.label
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.input

class AspectNameInput : RComponent<AspectNameInput.Props, RState>() {

    private fun handleInputFieldChanged(e: Event) {
        e.stopPropagation()
        e.preventDefault()
        props.onChange(e.target.unsafeCast<HTMLInputElement>().value)
    }

    override fun RBuilder.render() {
        val inputRef = props.inputRef
        div(classes = "aspect-edit-console--aspect-input-container") {
            label(classes = "aspect-edit-console--input-label", htmlFor = "aspect-name") {
                +"Name"
            }
            div(classes = "aspect-edit-console--input-wrapper") {
                input(type = InputType.text, name = "name", classes = "aspect-edit-console--input") {
                    attrs {
                        id = "aspect-name"
                        value = props.value ?: ""
                        onChangeFunction = ::handleInputFieldChanged
                        if (inputRef != null) {
                            ref { inputRef(it as HTMLInputElement?) }
                        }
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var value: String?
        var onChange: (String) -> Unit
        var inputRef: ((HTMLInputElement?) -> Unit)?
    }

}

fun RBuilder.aspectNameInput(block: RHandler<AspectNameInput.Props>) = child(AspectNameInput::class, block)