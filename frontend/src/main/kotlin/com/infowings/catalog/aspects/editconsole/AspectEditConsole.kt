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

class AspectEditConsole : RComponent<AspectEditConsole.Props, AspectEditConsole.State>() {

    private fun handleKeyDown(e: Event) {
        e.stopPropagation()
        e.preventDefault()
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

    override fun RBuilder.render() {
        div(classes = "aspect-edit-console") {
            attrs {
                onKeyDownFunction = ::handleKeyDown
            }
            div(classes = "aspect-edit-console--input-group") {
                aspectNameInput {
                    attrs {
                        initialValue = props.aspect.name
                    }
                }
                aspectMeasureInput {
                    attrs {
                        initialValue = props.aspect.measure
                    }
                }
                aspectDomainInput {
                    attrs {
                        initialValue = props.aspect.domain
                    }
                }
                aspectBaseTypeInput {
                    attrs {
                        initialValue = props.aspect.baseType
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