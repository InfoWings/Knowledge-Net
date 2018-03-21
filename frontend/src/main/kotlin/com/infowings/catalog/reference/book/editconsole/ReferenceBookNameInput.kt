package com.infowings.catalog.reference.book.editconsole

import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.input

class ReferenceBookNameInput : RComponent<ReferenceBookNameInput.Props, RState>() {

    private fun handleInputFieldChanged(e: Event) {
        e.stopPropagation()
        e.preventDefault()
        props.onChange(e.target.unsafeCast<HTMLInputElement>().value)
    }

    override fun RBuilder.render() {
        div(classes = "book-edit-console--input-container") {
            div(classes = "book-edit-console--input-wrapper") {
                input(type = InputType.text, name = "book-name-input", classes = "book-edit-console--input") {
                    attrs {
                        id = "book-name-input"
                        value = props.value ?: ""
                        onChangeFunction = ::handleInputFieldChanged
                        placeholder = "Enter book name ..."
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

fun RBuilder.referenceBookNameInput(block: RHandler<ReferenceBookNameInput.Props>) =
    child(ReferenceBookNameInput::class, block)