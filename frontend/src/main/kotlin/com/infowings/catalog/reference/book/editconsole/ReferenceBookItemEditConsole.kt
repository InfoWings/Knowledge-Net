package com.infowings.catalog.reference.book.editconsole

import com.infowings.catalog.common.ReferenceBookItemData
import kotlinx.html.js.onBlurFunction
import kotlinx.html.js.onKeyDownFunction
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import react.*
import react.dom.div

class ReferenceBookItemEditConsole(props: Props) :
    RComponent<ReferenceBookItemEditConsole.Props, ReferenceBookItemEditConsole.State>(props) {

    override fun State.init(props: Props) {
        value = props.bookItem.value
    }

    override fun componentWillReceiveProps(nextProps: Props) {
        if (props.bookItem.id != nextProps.bookItem.id) {
            setState {
                value = nextProps.bookItem.value
            }
        }
    }

    private fun handleKeyDown(e: Event) {
        e.stopPropagation()
        val keyCode = e.unsafeCast<KeyboardEvent>().keyCode
        when (keyCode) {
            27 -> props.onCancel() //esc
            13 -> {
                if (state.value.isNullOrEmpty()) error("Reference Book Item Value is empty")
                props.onSubmit(props.bookItem.copy(value = state.value))
            } //Enter
        }
    }

    private fun handleBlur(e: Event) {
        e.preventDefault()
        e.stopPropagation()
        props.onCancel()
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
                }
            }
        }
    }

    interface Props : RProps {
        var bookItem: ReferenceBookItemData
        var onCancel: () -> Unit
        var onSubmit: (ReferenceBookItemData) -> Unit
    }

    interface State : RState {
        var value: String?
    }
}

fun RBuilder.bookItemEditConsole(block: RHandler<ReferenceBookItemEditConsole.Props>) =
    child(ReferenceBookItemEditConsole::class, block)