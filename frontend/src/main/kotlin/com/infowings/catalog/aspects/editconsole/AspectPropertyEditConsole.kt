package com.infowings.catalog.aspects.editconsole

import com.infowings.catalog.aspects.AspectBadRequestException
import com.infowings.catalog.aspects.editconsole.aspect.aspectBaseTypeInput
import com.infowings.catalog.aspects.editconsole.aspect.aspectDomainInput
import com.infowings.catalog.aspects.editconsole.aspect.aspectMeasureInput
import com.infowings.catalog.aspects.editconsole.aspect.aspectNameInput
import com.infowings.catalog.aspects.editconsole.aspectproperty.aspectPropertyAspect
import com.infowings.catalog.aspects.editconsole.aspectproperty.aspectPropertyCardinality
import com.infowings.catalog.aspects.editconsole.aspectproperty.aspectPropertyNameInput
import com.infowings.catalog.aspects.editconsole.view.aspectConsoleBlock
import com.infowings.catalog.aspects.editconsole.view.consoleButtonsGroup
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import com.infowings.catalog.common.GlobalMeasureMap
import com.infowings.catalog.wrappers.react.setStateWithCallback
import kotlinx.coroutines.experimental.launch
import org.w3c.dom.HTMLInputElement
import react.*
import react.dom.div
import react.dom.span

class AspectPropertyEditConsole(props: Props) : RComponent<AspectPropertyEditConsole.Props, AspectPropertyEditConsole.State>(props) {

    private var inputRef: HTMLInputElement? = null

    override fun State.init(props: Props) {
        aspectPropertyName = props.parentAspect.properties[props.aspectPropertyIndex].name
        aspectPropertyCardinality = props.parentAspect.properties[props.aspectPropertyIndex].cardinality
        aspectPropertyAspectId = props.parentAspect.properties[props.aspectPropertyIndex].aspectId
        childAspectName = props.childAspect?.name
        childAspectMeasure = props.childAspect?.measure
        childAspectDomain = props.childAspect?.domain
        childAspectBaseType = props.childAspect?.baseType
    }

    override fun componentWillReceiveProps(nextProps: Props) {
        if (props.parentAspect.id != nextProps.parentAspect.id
                || props.aspectPropertyIndex != nextProps.aspectPropertyIndex) {
            setStateWithCallback({ inputRef?.focus(); inputRef?.select() }) {
                aspectPropertyName = nextProps.parentAspect.properties[nextProps.aspectPropertyIndex].name
                aspectPropertyCardinality = nextProps.parentAspect.properties[nextProps.aspectPropertyIndex].cardinality
                aspectPropertyAspectId = nextProps.parentAspect.properties[nextProps.aspectPropertyIndex].aspectId
                childAspectName = nextProps.childAspect?.name
                childAspectMeasure = nextProps.childAspect?.measure
                childAspectDomain = nextProps.childAspect?.domain
                childAspectBaseType = nextProps.childAspect?.baseType
                badRequestErrorMessage = null
            }
        }
    }

    override fun componentDidMount() {
        inputRef?.focus()
        inputRef?.select()
    }

    private fun assignInputRef(inputRef: HTMLInputElement?) {
        this.inputRef = inputRef
    }

    /**
     * Business logic function (handler for add-to-list icon click and ctrl-enter keystoke)
     */
    private fun switchToNextProperty() {
        props.onSwitchToNextProperty(props.parentAspect.properties[props.aspectPropertyIndex].copy(
                name = state.aspectPropertyName ?: error("Can't save aspect property with name == null"),
                cardinality = state.aspectPropertyCardinality
                        ?: error("Can't save aspect property with cardinality == null"),
                aspectId = state.aspectPropertyAspectId
                        ?: error("Can't save aspect property with aspectId == null")
        ))
    }

    /**
     * Business logic function (handler for check icon click and enter keystoke)
     */
    private fun trySubmitParentAspect() {
        launch {
            try {
                props.onSaveParentAspect(props.parentAspect.properties[props.aspectPropertyIndex].copy(
                        name = state.aspectPropertyName ?: error("Can't save aspect property with name == null"),
                        cardinality = state.aspectPropertyCardinality
                                ?: error("Can't save aspect property with cardinality == null"),
                        aspectId = state.aspectPropertyAspectId
                                ?: error("Can't save aspect property with aspectId == null")
                ))
            } catch (exception: AspectBadRequestException) {
                setState {
                    badRequestErrorMessage = exception.message
                }
            }
        }
    }


    private fun handlePropertyNameChanged(name: String) {
        setState {
            aspectPropertyName = name
        }
    }

    private fun handlePropertyCardinalityChanged(cardinality: String) {
        setState {
            aspectPropertyCardinality = cardinality
        }
    }

    private fun handlePropertyAspectIdChanged(aspect: AspectData) {
        setState {
            aspectPropertyAspectId = aspect.id
            childAspectName = aspect.name
            childAspectMeasure = aspect.measure
            childAspectDomain = aspect.domain
            childAspectBaseType = aspect.baseType
        }
    }

    private fun handleChildAspectNameChanged(name: String) {
        setState {
            childAspectName = name
        }
    }

    private fun handleChildAspectMeasureChanged(measure: String) {
        setState {
            childAspectMeasure = measure
            childAspectBaseType = GlobalMeasureMap[measure]?.baseType?.name
        }
    }

    private fun handleChildAspectDomainChanged(domain: String) {
        setState {
            childAspectDomain = domain
        }
    }

    private fun handleChildAspectBaseTypeChanged(baseType: String) {
        setState {
            childAspectBaseType = baseType
        }
    }

    override fun RBuilder.render() {
        aspectConsoleBlock {
            attrs {
                onEscape = props.onCancel
                onEnter = ::trySubmitParentAspect
                onCtrlEnter = ::switchToNextProperty
            }
            div(classes = "aspect-edit-console--input-group-aspect-property") {
                aspectPropertyNameInput {
                    attrs {
                        value = state.aspectPropertyName
                        onChange = ::handlePropertyNameChanged
                        inputRef = ::assignInputRef
                    }
                }
                aspectPropertyCardinality {
                    attrs {
                        value = state.aspectPropertyCardinality
                        onChange = ::handlePropertyCardinalityChanged
                    }
                }
                consoleButtonsGroup(
                        onSubmitClick = ::trySubmitParentAspect,
                        onCancelClick = props.onCancel,
                        onAddToListClick = ::switchToNextProperty
                )
            }
            div(classes = "aspect-edit-console--input-group-aspect-property-aspect") {
                aspectPropertyAspect {
                    val boundAspectId = state.aspectPropertyAspectId
                    val aspectPropertyId = props.parentAspect.properties[props.aspectPropertyIndex].id
                    attrs {
                        parentAspectId = props.parentAspect.id
                        this.aspectPropertyId = if (aspectPropertyId.isEmpty()) null else aspectPropertyId
                        aspect = props.childAspect ?: if (!boundAspectId.isNullOrEmpty()) {
                            AspectData(
                                    boundAspectId,
                                    state.childAspectName!!,
                                    state.childAspectMeasure,
                                    state.childAspectDomain,
                                    state.childAspectBaseType
                            )
                        } else null
                        onAspectSelected = ::handlePropertyAspectIdChanged
                    }
                }
                div(classes = "aspect-edit-console--input-group-aspect-container") {
                    div(classes = "aspect-edit-console--input-group-aspect") {
                        aspectNameInput {
                            attrs {
                                value = state.childAspectName
                                onChange = ::handleChildAspectNameChanged
                            }
                        }
                        aspectMeasureInput {
                            attrs {
                                value = state.childAspectMeasure
                                onChange = ::handleChildAspectMeasureChanged
                            }
                        }
                        aspectDomainInput {
                            attrs {
                                value = state.childAspectDomain
                                onChange = ::handleChildAspectDomainChanged
                            }
                        }
                        aspectBaseTypeInput {
                            attrs {
                                measureUnit = state.childAspectMeasure
                                value = state.childAspectBaseType
                                onChange = ::handleChildAspectBaseTypeChanged
                            }
                        }
                    }
                }
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
        var parentAspect: AspectData
        var aspectPropertyIndex: Int
        var childAspect: AspectData?
        var onCancel: () -> Unit
        var onSwitchToNextProperty: (AspectPropertyData) -> Unit
        var onSaveParentAspect: suspend (AspectPropertyData) -> Unit
    }

    interface State : RState {
        var aspectPropertyName: String?
        var aspectPropertyCardinality: String?
        var aspectPropertyAspectId: String?
        var childAspectName: String?
        var childAspectMeasure: String?
        var childAspectDomain: String?
        var childAspectBaseType: String?
        var badRequestErrorMessage: String?
    }
}

fun RBuilder.aspectPropertyEditConsole(block: RHandler<AspectPropertyEditConsole.Props>) = child(AspectPropertyEditConsole::class, block)