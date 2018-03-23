package com.infowings.catalog.reference.book.editconsole

import com.infowings.catalog.common.ReferenceBookData
import kotlinx.html.js.onBlurFunction
import kotlinx.html.js.onKeyDownFunction
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import react.*
import react.dom.div

class ReferenceBookEditConsole(props: Props) :
    RComponent<ReferenceBookEditConsole.Props, ReferenceBookEditConsole.State>(props) {

    override fun State.init(props: Props) {
        bookName = props.bookData.name
    }

    override fun componentWillReceiveProps(nextProps: Props) {
        if (props.bookData.id != nextProps.bookData.id) {
            setState {
                bookName = nextProps.bookData.name
            }
        }
    }

    private fun handleKeyDown(e: Event) {
        e.stopPropagation()
        val keyCode = e.unsafeCast<KeyboardEvent>().keyCode
        when (keyCode) {
            27 -> props.onCancel() //esc
            13 -> {
                if (state.bookName.isEmpty()) error("Reference Book Name must not be empty!")
                props.onSubmit(props.bookData.copy(name = state.bookName))
            } //Enter
        }
    }

    private fun handleBlur(e: Event) {
        e.preventDefault()
        e.stopPropagation()
        props.onCancel()
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
            }
        }
    }

    interface Props : RProps {
        var bookData: ReferenceBookData
        var onCancel: () -> Unit
        var onSubmit: (ReferenceBookData) -> Unit
    }

    interface State : RState {
        var bookName: String
    }
}

fun RBuilder.bookEditConsole(block: RHandler<ReferenceBookEditConsole.Props>) =
    child(ReferenceBookEditConsole::class, block)