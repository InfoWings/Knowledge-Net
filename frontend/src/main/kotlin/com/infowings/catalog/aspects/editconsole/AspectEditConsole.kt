package com.infowings.catalog.aspects.editconsole

import com.infowings.catalog.aspects.AspectBadRequestException
import com.infowings.catalog.aspects.editconsole.aspect.aspectBaseTypeInput
import com.infowings.catalog.aspects.editconsole.aspect.aspectDomainInput
import com.infowings.catalog.aspects.editconsole.aspect.aspectMeasureInput
import com.infowings.catalog.aspects.editconsole.aspect.aspectNameInput
import com.infowings.catalog.aspects.editconsole.view.aspectConsoleBlock
import com.infowings.catalog.aspects.editconsole.view.consoleButtonsGroup
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.GlobalMeasureMap
import com.infowings.catalog.wrappers.react.setStateWithCallback
import kotlinx.coroutines.experimental.launch
import org.w3c.dom.HTMLInputElement
import react.*
import react.dom.div
import react.dom.span

class AspectEditConsole(props: Props) : RComponent<AspectEditConsole.Props, AspectEditConsole.State>(props) {

    private var inputRef: HTMLInputElement? = null

    override fun State.init(props: Props) {
        aspectName = props.aspect.name
        aspectMeasure = props.aspect.measure
        aspectDomain = props.aspect.domain
        aspectBaseType = props.aspect.baseType
    }

    override fun componentDidMount() {
        inputRef?.focus()
        inputRef?.select()
    }

    override fun componentWillReceiveProps(nextProps: Props) {
        if (props.aspect != nextProps.aspect || nextProps.aspect == AspectData(null, "", null, null, null)) {
            setStateWithCallback({ inputRef?.focus(); inputRef?.select() }) {
                aspectName = nextProps.aspect.name
                aspectMeasure = nextProps.aspect.measure
                aspectDomain = nextProps.aspect.domain
                aspectBaseType = nextProps.aspect.baseType
                badRequestErrorMessage = null
            }
        }
    }

    private fun assignInputRef(element: HTMLInputElement?) {
        inputRef = element
    }

    private fun tryMakeSubmitAspectRequest() {
        launch {
            try {
                props.onSubmit(props.aspect.copy(
                        name = state.aspectName ?: error("Aspect Name is null"),
                        measure = if (state.aspectMeasure.isNullOrEmpty()) null else state.aspectMeasure,
                        domain = if (state.aspectDomain.isNullOrEmpty()) null else state.aspectDomain,
                        baseType = if (state.aspectBaseType.isNullOrEmpty()) null else state.aspectBaseType
                ))
            } catch (exception: AspectBadRequestException) {
                setState {
                    badRequestErrorMessage = exception.message
                }
            }
        }
    }

    private fun handleSwitchToProperties() {
        props.onSwitchToProperties(props.aspect.copy(
                name = state.aspectName ?: error("Aspect Name is null"),
                measure = if (state.aspectMeasure.isNullOrEmpty()) null else state.aspectMeasure,
                domain = if (state.aspectDomain.isNullOrEmpty()) null else state.aspectDomain,
                baseType = if (state.aspectBaseType.isNullOrEmpty()) null else state.aspectBaseType
        ))
    }

    private fun handleAspectNameChanged(name: String) {
        setState {
            aspectName = name
        }
    }

    private fun handleAspectMeasureChanged(measure: String) {
        setState {
            aspectMeasure = measure
            aspectBaseType = GlobalMeasureMap[measure]?.baseType?.name
        }
    }

    private fun handleAspectDomainChanged(domain: String) {
        setState {
            aspectDomain = domain
        }
    }

    private fun handleAspectBaseTypeChanged(baseType: String) {
        setState {
            aspectBaseType = baseType
        }
    }

    override fun RBuilder.render() {
        aspectConsoleBlock {
            attrs {
                onEscape = props.onCancel
                onEnter = ::tryMakeSubmitAspectRequest
                onCtrlEnter = ::handleSwitchToProperties
            }
            div(classes = "aspect-edit-console--input-group-aspect") {
                aspectNameInput {
                    attrs {
                        value = state.aspectName
                        onChange = ::handleAspectNameChanged
                        inputRef = ::assignInputRef
                    }
                }
                aspectMeasureInput {
                    attrs {
                        value = state.aspectMeasure
                        onChange = ::handleAspectMeasureChanged
                    }
                }
                aspectDomainInput {
                    attrs {
                        value = state.aspectDomain
                        onChange = ::handleAspectDomainChanged
                    }
                }
                aspectBaseTypeInput {
                    attrs {
                        measureUnit = state.aspectMeasure
                        value = state.aspectBaseType
                        onChange = ::handleAspectBaseTypeChanged
                    }
                }
                consoleButtonsGroup(
                        onSubmitClick = ::tryMakeSubmitAspectRequest,
                        onAddToListClick = ::handleSwitchToProperties,
                        onCancelClick = props.onCancel
                )
            }
            val badRequestErrorMessage = state.badRequestErrorMessage
            if (badRequestErrorMessage != null) {
                div(classes = "aspect-edit-console--error-message-container") {
                    span(classes = "aspect-edit-console--error-message") {
                        +badRequestErrorMessage
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var aspect: AspectData
        var onCancel: () -> Unit
        var onSubmit: suspend (AspectData) -> Unit
        var onSwitchToProperties: (AspectData) -> Unit
    }

    interface State : RState {
        var aspectName: String?
        var aspectMeasure: String?
        var aspectDomain: String?
        var aspectBaseType: String?
        var badRequestErrorMessage: String?
    }
}

fun RBuilder.aspectEditConsole(block: RHandler<AspectEditConsole.Props>) = child(AspectEditConsole::class, block)