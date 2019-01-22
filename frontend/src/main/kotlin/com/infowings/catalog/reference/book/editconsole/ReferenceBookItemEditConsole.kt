package com.infowings.catalog.reference.book.editconsole

import com.infowings.catalog.common.ReferenceBookItem
import com.infowings.catalog.components.treeview.treeNode
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

class ReferenceBookItemEditConsole(props: Props) :
    RComponent<ReferenceBookItemEditConsole.Props, ReferenceBookItemEditConsole.State>(props),
    JobCoroutineScope by JobSimpleCoroutineScope() {

    override fun componentWillMount() {
        job = Job()
    }

    override fun componentWillUnmount() {
        job.cancel()
    }

    override fun State.init(props: Props) {
        value = props.bookItem.value
        errorMessage = null
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
                props.onSubmit(props.bookItem.copy(value = state.value), false)
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
        treeNode {
            attrs {
                treeNodeContent = buildElement {
                    div(classes = "book-edit-console") {
                        attrs {
                            onKeyDownFunction = ::handleKeyDown
                            onBlurFunction = ::handleBlur
                        }
                        div(classes = "book-edit-console__input-group") {
                            referenceBookItemValueInput {
                                attrs {
                                    value = state.value
                                    onChange = ::handleBookItemValueChanged
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
                }!!
            }
        }
    }

    interface Props : RProps {
        var bookItem: ReferenceBookItem
        var onCancel: () -> Unit
        var onSubmit: suspend (ReferenceBookItem, force: Boolean) -> Unit
    }

    interface State : RState {
        var value: String
        var errorMessage: String?
    }
}

fun RBuilder.bookItemEditConsole(block: RHandler<ReferenceBookItemEditConsole.Props>) =
    child(ReferenceBookItemEditConsole::class, block)