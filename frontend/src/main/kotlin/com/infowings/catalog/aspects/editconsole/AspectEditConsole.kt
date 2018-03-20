package com.infowings.catalog.aspects.editconsole

import com.infowings.catalog.aspects.editconsole.aspect.aspectBaseTypeInput
import com.infowings.catalog.aspects.editconsole.aspect.aspectDomainInput
import com.infowings.catalog.aspects.editconsole.aspect.aspectMeasureInput
import com.infowings.catalog.aspects.editconsole.aspect.aspectNameInput
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.wrappers.react.use
import kotlinx.html.js.onKeyDownFunction
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import react.*
import react.dom.div
import react.dom.svg

class AspectEditConsole(props: Props) : RComponent<AspectEditConsole.Props, AspectEditConsole.State>(props) {

    private var inputRef: HTMLInputElement? = null
    private var aspectChanged: Boolean = false

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

    override fun componentDidUpdate(prevProps: Props, prevState: State) {
        when {
            props.aspect.id != null && props.aspect.id != prevProps.aspect.id -> {
                inputRef?.focus()
                inputRef?.select()
            }
            props.aspect.id == null && aspectChanged -> {
                aspectChanged = false
                inputRef?.focus()
                inputRef?.select()
            }
        }
    }

    override fun componentWillReceiveProps(nextProps: Props) {
        setState {
            aspectName = nextProps.aspect.name
            aspectMeasure = nextProps.aspect.measure
            aspectDomain = nextProps.aspect.domain
            aspectBaseType = nextProps.aspect.baseType
        }
    }

    private fun assignInputRef(element: HTMLInputElement?) {
        inputRef = element
    }

    private fun handleKeyDown(e: Event) {
        e.stopPropagation()
        val keyCode = e.unsafeCast<KeyboardEvent>().keyCode
        val ctrlPressed = e.unsafeCast<KeyboardEvent>().ctrlKey
        when (keyCode) {
            27 -> {
                aspectChanged = true
                inputRef?.blur()
                props.onCancel()
            }
            13 -> {
                if (ctrlPressed) {
                    aspectChanged = true
                    inputRef?.blur()
                    props.onSwitchToProperties(props.aspect.copy(
                            name = state.aspectName ?: error("Aspect Name is null"),
                            measure = if (state.aspectMeasure.isNullOrEmpty()) null else state.aspectMeasure,
                            domain = if (state.aspectDomain.isNullOrEmpty()) null else state.aspectDomain,
                            baseType = if (state.aspectBaseType.isNullOrEmpty()) null else state.aspectBaseType
                    ))
                } else {
                    aspectChanged = true
                    inputRef?.blur()
                    props.onSubmit(props.aspect.copy(
                            name = state.aspectName ?: error("Aspect Name is null"),
                            measure = if (state.aspectMeasure.isNullOrEmpty()) null else state.aspectMeasure,
                            domain = if (state.aspectDomain.isNullOrEmpty()) null else state.aspectDomain,
                            baseType = if (state.aspectBaseType.isNullOrEmpty()) null else state.aspectBaseType
                    ))
                }
            }
        }
    }

    private fun handleAspectNameChanged(name: String) {
        setState {
            aspectName = name
        }
    }

    private fun handleAspectMeasureChanged(measure: String) {
        setState {
            aspectMeasure = measure
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
        div(classes = "aspect-edit-console") {
            attrs {
                onKeyDownFunction = ::handleKeyDown
            }
            div(classes = "aspect-edit-console--button-control-tab") {
                div(classes = "aspect-edit-console--button-control") {
                    svg(classes = "aspect-edit-console--button-icon aspect-edit-console--button-icon__green") {
                        use("svg/sprite.svg#icon-check")
                    }
                }
                div(classes = "aspect-edit-console--button-control") {
                    svg(classes = "aspect-edit-console--button-icon aspect-edit-console--button-icon__green") {
                        use("svg/sprite.svg#icon-add-to-list")
                    }
                }
                div(classes = "aspect-edit-console--button-control") {
                    svg(classes = "aspect-edit-console--button-icon aspect-edit-console--button-icon__red") {
                        use("svg/sprite.svg#icon-cross")
                    }
                }
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
                        value = state.aspectBaseType
                        onChange = ::handleAspectBaseTypeChanged
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var aspect: AspectData
        var onCancel: () -> Unit
        var onSubmit: (AspectData) -> Unit
        var onSwitchToProperties: (AspectData) -> Unit
    }

    interface State : RState {
        var aspectName: String?
        var aspectMeasure: String?
        var aspectDomain: String?
        var aspectBaseType: String?
    }
}

fun RBuilder.aspectEditConsole(block: RHandler<AspectEditConsole.Props>) = child(AspectEditConsole::class, block)