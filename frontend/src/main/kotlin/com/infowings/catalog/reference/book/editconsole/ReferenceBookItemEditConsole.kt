package com.infowings.catalog.reference.book.editconsole

import com.infowings.catalog.common.ReferenceBookItemData
import com.infowings.catalog.utils.BadRequestException
import kotlinx.coroutines.experimental.launch
import kotlinx.html.js.onBlurFunction
import kotlinx.html.js.onKeyDownFunction
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import react.*
import react.dom.div
import react.dom.span

class ReferenceBookItemEditConsole(props: Props) :
    RComponent<ReferenceBookItemEditConsole.Props, ReferenceBookItemEditConsole.State>(props) {

    override fun State.init(props: Props) {
        value = props.bookItemData.value
        errorMessage = null
    }

    override fun componentWillReceiveProps(nextProps: Props) {
        if (props.bookItemData.id != nextProps.bookItemData.id) {
            setState {
                value = nextProps.bookItemData.value
            }
        }
    }

    private fun handleKeyDown(e: Event) {
        e.stopPropagation()
        val keyCode = e.unsafeCast<KeyboardEvent>().keyCode
        when (keyCode) {
            27 -> {
                props.onCancel()
                setState {
                    errorMessage = null
                }
            } //esc
            13 -> {
                if (state.value.isEmpty()) {
                    setState {
                        errorMessage = "Must not be empty!"
                    }
                    return
                }
                submit()
            } //Enter
        }
    }

    private fun submit() {
        launch {
            try {
                props.onSubmit(props.bookItemData.copy(value = state.value))
            } catch (e: BadRequestException) {
                setState {
                    errorMessage = e.message
                }
            }
        }
    }

    private fun handleBlur(e: Event) {
        e.preventDefault()
        e.stopPropagation()
        props.onCancel()
        setState {
            errorMessage = null
        }
    }

    private fun handleBookItemValueChanged(name: String) {
        setState {
            value = name
        }
    }

    override fun RBuilder.render() {
        div {
            div(classes = "book-edit-console") {
                attrs {
                    onKeyDownFunction = ::handleKeyDown
                    onBlurFunction = ::handleBlur
                }
                div(classes = "book-edit-console--input-group") {
                    referenceBookItemValueInput {
                        attrs {
                            value = state.value
                            onChange = ::handleBookItemValueChanged
                        }
                    }
                    val errorMessage = state.errorMessage
                    if (errorMessage != null) {
                        span(classes = "book-edit-console--error-message") {
                            +errorMessage
                        }
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var bookItemData: ReferenceBookItemData
        var onCancel: () -> Unit
        var onSubmit: suspend (ReferenceBookItemData) -> Unit
    }

    interface State : RState {
        var value: String
        var errorMessage: String?
    }
}

fun RBuilder.bookItemEditConsole(block: RHandler<ReferenceBookItemEditConsole.Props>) =
    child(ReferenceBookItemEditConsole::class, block)