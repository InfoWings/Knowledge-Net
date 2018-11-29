package com.infowings.catalog.aspects.editconsole.aspect

import com.infowings.catalog.aspects.getAspectHints
import com.infowings.catalog.common.AspectsHints
import com.infowings.catalog.components.popup.existingAspectWindow
import com.infowings.catalog.wrappers.react.label
import kotlinx.coroutines.experimental.launch
import kotlinx.html.InputType
import kotlinx.html.id
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import react.*
import react.dom.div
import react.dom.input

class AspectNameInput(props: AspectNameInput.Props) : RComponent<AspectNameInput.Props, AspectNameInput.State>(props) {
    override fun State.init(props: Props) {
        value = "init"
        hints = AspectsHints.empty()
    }


    private fun handleInputFieldChanged(e: Event) {
        e.stopPropagation()
        e.preventDefault()
        val current = e.target.unsafeCast<HTMLInputElement>().value
        launch {
            val frashHints = if (current.length > 2) getAspectHints(current) else AspectsHints.empty()

            setState {
                value += "1"
                hints = frashHints
            }
        }
        props.onChange(e.target.unsafeCast<HTMLInputElement>().value)
    }

    override fun RBuilder.render() {
        val inputRef = props.inputRef

        div(classes = "aspect-edit-console--aspect-input-container") {
            existingAspectWindow {
                attrs {
                    message = "qqq" + state.value
                    hints = state.hints
                }
            }

            label(classes = "aspect-edit-console--input-label", htmlFor = "aspect-name") {
                +"Name"
            }
            div(classes = "aspect-edit-console--input-wrapper") {
                input(type = InputType.text, name = "aspect-name", classes = "aspect-edit-console--input") {
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

    interface State : RState {
        var value: String
        var hints: AspectsHints
    }

    interface Props : RProps {
        var value: String?
        var onChange: (String) -> Unit
        var inputRef: ((HTMLInputElement?) -> Unit)?
    }

}

fun RBuilder.aspectNameInput(block: RHandler<AspectNameInput.Props>) = child(AspectNameInput::class, block)