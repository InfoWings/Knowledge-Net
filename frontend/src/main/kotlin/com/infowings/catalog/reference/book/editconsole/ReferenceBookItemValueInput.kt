package com.infowings.catalog.reference.book.editconsole

import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.input

class ReferenceBookItemValueInput : RComponent<ReferenceBookItemValueInput.Props, RState>() {

    private fun handleInputFieldChanged(e: Event) {
        e.stopPropagation()
        e.preventDefault()
        props.onChange(e.target.unsafeCast<HTMLInputElement>().value)
    }

    override fun RBuilder.render() {
        div(classes = "book-edit-console--input-container") {
            div(classes = "book-edit-console--input-wrapper") {
                input(type = InputType.text, name = "book-item-value-input", classes = "book-edit-console--input") {
                    attrs {
                        id = "book-item-value-input"
                        value = props.value ?: ""
                        onChangeFunction = ::handleInputFieldChanged
                        placeholder = "Enter item value ..."
                        autoFocus = true
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

fun RBuilder.referenceBookItemValueInput(block: RHandler<ReferenceBookItemValueInput.Props>) =
    child(ReferenceBookItemValueInput::class, block)