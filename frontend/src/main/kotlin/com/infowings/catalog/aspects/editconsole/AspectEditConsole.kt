package com.infowings.catalog.aspects.editconsole

import com.infowings.catalog.aspects.editconsole.aspect.aspectBaseTypeInput
import com.infowings.catalog.aspects.editconsole.aspect.aspectDomainInput
import com.infowings.catalog.aspects.editconsole.aspect.aspectMeasureInput
import com.infowings.catalog.aspects.editconsole.aspect.aspectNameInput
import com.infowings.catalog.common.AspectData
import kotlinx.html.js.onKeyDownFunction
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import react.*
import react.dom.div

class AspectEditConsole(props: Props) : RComponent<AspectEditConsole.Props, AspectEditConsole.State>(props) {

    override fun State.init(props: Props) {
        aspectName = props.aspect.name
        aspectMeasure = props.aspect.measure
        aspectDomain = props.aspect.domain
        aspectBaseType = props.aspect.baseType
    }

    override fun componentWillReceiveProps(nextProps: Props) {
        if (props.aspect.id != nextProps.aspect.id) {
            setState {
                aspectName = nextProps.aspect.name
                aspectMeasure = nextProps.aspect.measure
                aspectDomain = nextProps.aspect.domain
                aspectBaseType = nextProps.aspect.baseType
            }
        }
    }

    private fun handleKeyDown(e: Event) {
        e.stopPropagation()
        val keyCode = e.unsafeCast<KeyboardEvent>().keyCode
        when (keyCode) {
            27 -> props.onCancel()
            13 -> props.onSubmit(props.aspect.copy(
                    name = state.aspectName ?: error("Aspect Name is null"),
                    measure = state.aspectMeasure,
                    domain = state.aspectDomain,
                    baseType = state.aspectBaseType
            ))
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
            div(classes = "aspect-edit-console--input-group") {
                aspectNameInput {
                    attrs {
                        value = state.aspectName
                        onChange = ::handleAspectNameChanged
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
    }

    interface State : RState {
        var aspectName: String?
        var aspectMeasure: String?
        var aspectDomain: String?
        var aspectBaseType: String?
    }
}

fun RBuilder.aspectEditConsole(block: RHandler<AspectEditConsole.Props>) = child(AspectEditConsole::class, block)