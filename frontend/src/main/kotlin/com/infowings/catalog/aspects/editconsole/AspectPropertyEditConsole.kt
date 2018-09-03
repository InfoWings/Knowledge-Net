package com.infowings.catalog.aspects.editconsole

import com.infowings.catalog.aspects.AspectBadRequestException
import com.infowings.catalog.aspects.editconsole.aspectproperty.aspectPropertyAspect
import com.infowings.catalog.aspects.editconsole.aspectproperty.aspectPropertyCardinality
import com.infowings.catalog.aspects.editconsole.aspectproperty.aspectPropertyNameInput
import com.infowings.catalog.aspects.editconsole.view.aspectConsoleBlock
import com.infowings.catalog.aspects.editconsole.view.consoleButtonsGroup
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.BadRequestCode
import com.infowings.catalog.components.description.descriptionComponent
import com.infowings.catalog.components.popup.forceRemoveConfirmWindow
import kotlinx.coroutines.experimental.launch
import org.w3c.dom.HTMLInputElement
import react.*
import react.dom.div

class AspectPropertyEditConsole(props: Props) :
    RComponent<AspectPropertyEditConsole.Props, AspectPropertyEditConsole.State>(props) {

    private var inputRef: HTMLInputElement? = null

    override fun componentWillReceiveProps(nextProps: Props) {
        if (props.parentAspect.id != nextProps.parentAspect.id
            || props.aspectPropertyIndex != nextProps.aspectPropertyIndex
        ) {
            inputRef?.focus()
            inputRef?.select()
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
        launch {
            props.propertyEditConsoleModel.switchToNextProperty()
        }
    }

    /**
     * Business logic function (handler for check icon click and enter keystoke)
     */
    private fun trySubmitParentAspect() {
        launch {
            props.propertyEditConsoleModel.submitParentAspect()
        }
    }

    private fun tryDelete(force: Boolean) {
        launch {
            try {
                props.propertyEditConsoleModel.deleteProperty(force)
                setState {
                    confirmation = false
                }
            } catch (ex: AspectBadRequestException) {
                if (ex.exceptionInfo.code == BadRequestCode.NEED_CONFIRMATION) setState {
                    confirmation = true
                }
            }
        }
    }

    private fun handlePropertyNameChanged(name: String) {
        props.propertyEditConsoleModel.updateAspectProperty(
            props.parentAspect.properties[props.aspectPropertyIndex].copy(
                name = name
            )
        )
    }

    private fun handlePropertyDescriptionChanged(description: String) {
        props.propertyEditConsoleModel.updateAspectProperty(
            props.parentAspect.properties[props.aspectPropertyIndex].copy(
                description = description
            )
        )
    }

    private fun handlePropertyCardinalityChanged(cardinality: String) {
        props.propertyEditConsoleModel.updateAspectProperty(
            props.parentAspect.properties[props.aspectPropertyIndex].copy(
                cardinality = cardinality
            )
        )
    }

    private fun handlePropertyAspectIdChanged(aspect: AspectData) {
        props.propertyEditConsoleModel.updateAspectProperty(
            props.parentAspect.properties[props.aspectPropertyIndex].copy(
                aspectId = aspect.id ?: error("Selected aspect has no ID")
            )
        )
    }

    override fun RBuilder.render() {
        aspectConsoleBlock {
            attrs {
                onEscape = props.propertyEditConsoleModel::discardChanges
                onEnter = ::trySubmitParentAspect
                onCtrlEnter = ::switchToNextProperty
            }
            div(classes = "aspect-edit-console--input-group-aspect-property") {
                aspectPropertyNameInput {
                    attrs {
                        value = props.parentAspect.properties[props.aspectPropertyIndex].name
                        onChange = ::handlePropertyNameChanged
                        inputRef = ::assignInputRef
                    }
                }
                aspectPropertyCardinality {
                    attrs {
                        value = props.parentAspect.properties[props.aspectPropertyIndex].cardinality
                        onChange = ::handlePropertyCardinalityChanged
                    }
                }
                div(classes = "aspect-edit-console--property-aspect-wrapper") {
                    aspectPropertyAspect {
                        val aspectPropertyId = props.parentAspect.properties[props.aspectPropertyIndex].id
                        attrs {
                            parentAspectId = props.parentAspect.id
                            this.aspectPropertyId = if (aspectPropertyId.isEmpty()) null else aspectPropertyId
                            aspect = props.childAspect
                            onAspectSelected = ::handlePropertyAspectIdChanged
                        }
                    }
                }
                descriptionComponent(
                    className = "aspect-edit-console--property-aspect-description",
                    description = props.parentAspect.properties[props.aspectPropertyIndex].description,
                    onNewDescriptionConfirmed = ::handlePropertyDescriptionChanged,
                    onEditStarted = null
                )
                consoleButtonsGroup(
                    onSubmitClick = ::trySubmitParentAspect,
                    onCancelClick = props.propertyEditConsoleModel::discardChanges,
                    onAddToListClick = ::switchToNextProperty,
                    onDeleteClick = { tryDelete(false) }
                )
            }
        }

        forceRemoveConfirmWindow {
            attrs {
                onConfirm = { tryDelete(true) }
                onCancel = { setState { confirmation = false } }
                isOpen = state.confirmation
                message = "Aspect property has linked entities."
            }
        }
    }

    interface State : RState {
        var confirmation: Boolean
    }

    interface Props : RProps {
        var parentAspect: AspectData
        var aspectPropertyIndex: Int
        var childAspect: AspectData?
        var propertyEditConsoleModel: AspectPropertyEditConsoleModel
    }
}

fun RBuilder.aspectPropertyEditConsole(block: RHandler<AspectPropertyEditConsole.Props>) =
    child(AspectPropertyEditConsole::class, block)