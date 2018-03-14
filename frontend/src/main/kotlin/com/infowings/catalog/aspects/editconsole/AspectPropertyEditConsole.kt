package com.infowings.catalog.aspects.editconsole

import com.infowings.catalog.aspects.editconsole.aspect.aspectBaseTypeInput
import com.infowings.catalog.aspects.editconsole.aspect.aspectDomainInput
import com.infowings.catalog.aspects.editconsole.aspect.aspectMeasureInput
import com.infowings.catalog.aspects.editconsole.aspect.aspectNameInput
import com.infowings.catalog.aspects.editconsole.aspectproperty.aspectPropertyCardinality
import com.infowings.catalog.aspects.editconsole.aspectproperty.aspectPropertyNameInput
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import kotlinx.html.js.onKeyDownFunction
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import react.*
import react.dom.div

class AspectPropertyEditConsole(props: Props) : RComponent<AspectPropertyEditConsole.Props, AspectPropertyEditConsole.State>(props) {

    override fun State.init(props: Props) {
        aspectPropertyName = props.aspectProperty.name
        aspectPropertyCardinality = props.aspectProperty.cardinality
        childAspectName = props.childAspect.name
        childAspectMeasure = props.childAspect.measure
        childAspectDomain = props.childAspect.domain
        childAspectBaseType = props.childAspect.baseType
    }

    private fun handleKeyDown(e: Event) {
        e.stopPropagation()
        val keyCode = e.unsafeCast<KeyboardEvent>().keyCode
        when (keyCode) {
            27 -> props.onCancel()
            13 -> console.log("Submit")
        }
    }

    override fun RBuilder.render() {
        div(classes = "aspect-edit-console") {
            attrs {
                onKeyDownFunction = ::handleKeyDown
                div(classes = "aspect-edit-console--input-group") {
                    aspectPropertyNameInput {
                        attrs {
                            initialValue = state.aspectPropertyName
                        }
                    }
                    aspectPropertyCardinality {
                        attrs {
                            initialValue = state.aspectPropertyCardinality
                        }
                    }
                    aspectNameInput {
                        attrs {
                            initialValue = state.childAspectName
                        }
                    }
                    aspectMeasureInput {
                        attrs {
                            initialValue = state.childAspectMeasure
                        }
                    }
                    aspectDomainInput {
                        attrs {
                            initialValue = state.childAspectDomain
                        }
                    }
                    aspectBaseTypeInput {
                        attrs {
                            initialValue = state.childAspectBaseType
                        }
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var aspectProperty: AspectPropertyData
        var childAspect: AspectData
        var onCancel: () -> Unit
        var onSubmit: (AspectData) -> Unit
    }

    interface State : RState {
        var aspectPropertyName: String?
        var aspectPropertyCardinality: String?
        var childAspectName: String?
        var childAspectMeasure: String?
        var childAspectDomain: String?
        var childAspectBaseType: String?
    }
}

fun RBuilder.aspectPropertyEditConsole(block: RHandler<AspectPropertyEditConsole.Props>) = child(AspectPropertyEditConsole::class, block)