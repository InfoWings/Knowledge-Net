package com.infowings.catalog.reference.book.editconsole

import com.infowings.catalog.common.ReferenceBook
import com.infowings.catalog.utils.BadRequestException
import com.infowings.catalog.utils.JobCoroutineScope
import com.infowings.catalog.utils.JobSimpleCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.html.js.onBlurFunction
import kotlinx.html.js.onKeyDownFunction
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import react.*
import react.dom.div
import react.dom.span

class ReferenceBookEditConsole(props: Props) :
    RComponent<ReferenceBookEditConsole.Props, ReferenceBookEditConsole.State>(props), JobCoroutineScope by JobSimpleCoroutineScope() {

    override fun State.init(props: Props) {
        bookName = props.book.name
        errorMessage = null
    }

    override fun componentWillMount() {
        job = Job()
    }

    override fun componentWillUnmount() {
        job.cancel()
    }

    override fun componentWillReceiveProps(nextProps: Props) {
        if (props.book.aspectId != nextProps.book.aspectId) {
            setState {
                bookName = nextProps.book.name
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
                props.onSubmit(props.book.copy(name = state.bookName))
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
            div(classes = "book-edit-console__input-group") {
                referenceBookNameInput {
                    attrs {
                        value = state.bookName
                        onChange = ::handleBookNameChanged
                    }
                }
                val errorMessage = state.errorMessage
                if (errorMessage != null) {
                    span(classes = "book-edit-console__error-message") {
                        +errorMessage
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var book: ReferenceBook
        var onCancel: () -> Unit
        var onSubmit: suspend (ReferenceBook) -> Unit
    }

    interface State : RState {
        var bookName: String
        var errorMessage: String?
    }
}

fun RBuilder.bookEditConsole(block: RHandler<ReferenceBookEditConsole.Props>) =
    child(ReferenceBookEditConsole::class, block)