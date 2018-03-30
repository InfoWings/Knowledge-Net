package com.infowings.catalog.reference.book.editconsole

import com.infowings.catalog.common.ReferenceBookData
import com.infowings.catalog.utils.BadRequestException
import kotlinx.coroutines.experimental.launch
import kotlinx.html.js.onBlurFunction
import kotlinx.html.js.onKeyDownFunction
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import react.*
import react.dom.div
import react.dom.span

class ReferenceBookEditConsole(props: Props) :
    RComponent<ReferenceBookEditConsole.Props, ReferenceBookEditConsole.State>(props) {

    override fun State.init(props: Props) {
        bookName = props.bookData.name
        errorMessage = null
    }

    override fun componentWillReceiveProps(nextProps: Props) {
        if (props.bookData.aspectId != nextProps.bookData.aspectId) {
            setState {
                bookName = nextProps.bookData.name
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
                if (state.bookName.isEmpty()) {
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
                props.onSubmit(props.bookData.copy(name = state.bookName))
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

    private fun handleBookNameChanged(name: String) {
        setState {
            bookName = name
        }
    }

    override fun RBuilder.render() {
        div(classes = "book-edit-console") {
            attrs {
                onKeyDownFunction = ::handleKeyDown
                onBlurFunction = ::handleBlur
            }
            div(classes = "book-edit-console--input-group") {
                referenceBookNameInput {
                    attrs {
                        value = state.bookName
                        onChange = ::handleBookNameChanged
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

    interface Props : RProps {
        var bookData: ReferenceBookData
        var onCancel: () -> Unit
        var onSubmit: suspend (ReferenceBookData) -> Unit
    }

    interface State : RState {
        var bookName: String
        var errorMessage: String?
    }
}

fun RBuilder.bookEditConsole(block: RHandler<ReferenceBookEditConsole.Props>) =
    child(ReferenceBookEditConsole::class, block)