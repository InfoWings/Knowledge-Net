package com.infowings.catalog.aspects.editconsole

import com.infowings.catalog.aspects.AspectBadRequestException
import com.infowings.catalog.aspects.editconsole.aspect.*
import com.infowings.catalog.aspects.editconsole.view.aspectConsoleBlock
import com.infowings.catalog.aspects.editconsole.view.consoleButtonsGroup
import com.infowings.catalog.common.*
import com.infowings.catalog.components.description.descriptionComponent
import com.infowings.catalog.components.popup.forceRemoveConfirmWindow
import kotlinx.coroutines.experimental.launch
import org.w3c.dom.HTMLInputElement
import react.*
import react.dom.div

class AspectEditConsole(props: Props) : RComponent<AspectEditConsole.Props, AspectEditConsole.State>(props) {

    private var inputRef: HTMLInputElement? = null

    override fun State.init(props: Props) {
        confirmation = false
    }

    override fun componentDidMount() {
        inputRef?.focus()
        inputRef?.select()
    }

    override fun componentWillReceiveProps(nextProps: Props) {
        if (props.aspect.id != nextProps.aspect.id || nextProps.aspect == emptyAspectData) {
            inputRef?.focus()
            inputRef?.select()
        }
    }

    private fun assignInputRef(element: HTMLInputElement?) {
        inputRef = element
    }

    private fun tryMakeSubmitAspectRequest() {
        launch {
            props.editConsoleModel.submitAspect()
        }
    }

    private fun tryDelete(force: Boolean) {
        launch {
            try {
                props.editConsoleModel.deleteAspect(force)
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

    private fun trySwitchToProperties() {
        launch {
            try {
                props.editConsoleModel.switchToProperties()
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

    private fun handleAspectNameChanged(name: String) {
        props.editConsoleModel.updateAspect(props.aspect.copy(name = name))
    }

    private fun handleAspectMeasureChanged(measure: String) {
        props.editConsoleModel.updateAspect(
            props.aspect.copy(
                measure = measure,
                baseType = GlobalMeasureMap[measure]?.baseType?.name
            )
        )
    }

    private fun handleAspectSubjectChanged(subjectName: String, subjectId: String) {
        props.editConsoleModel.updateAspect(
            props.aspect.copy(subject = SubjectData(id = subjectId, name = subjectName, description = ""))
        )
    }

    private fun handleAspectDomainChanged(domain: String) {
        props.editConsoleModel.updateAspect(
            props.aspect.copy(domain = domain)
        )
    }

    private fun handleAspectBaseTypeChanged(baseType: String) {
        props.editConsoleModel.updateAspect(
            props.aspect.copy(baseType = baseType)
        )
    }

    private fun handleAspectDescriptionChanged(description: String) {
        props.editConsoleModel.updateAspect(
            props.aspect.copy(description = description)
        )
    }

    override fun RBuilder.render() {
        aspectConsoleBlock {
            attrs {
                onEscape = props.editConsoleModel::discardChanges
                onEnter = ::tryMakeSubmitAspectRequest
                onCtrlEnter = ::trySwitchToProperties
            }
            div(classes = "aspect-edit-console--input-group-aspect") {
                aspectNameInput {
                    attrs {
                        value = props.aspect.name
                        onChange = ::handleAspectNameChanged
                        inputRef = ::assignInputRef
                    }
                }
                aspectMeasureInput {
                    attrs {
                        value = props.aspect.measure
                        onChange = ::handleAspectMeasureChanged
                    }
                }
                aspectDomainInput {
                    attrs {
                        value = props.aspect.domain
                        onChange = ::handleAspectDomainChanged
                    }
                }
                aspectBaseTypeInput {
                    attrs {
                        value = props.aspect.baseType
                        disabled = !props.aspect.measure.isNullOrEmpty()
                        onChange = ::handleAspectBaseTypeChanged
                    }
                }
                aspectSubjectInput {
                    attrs {
                        value = props.aspect.subject?.name ?: ""
                        onChange = ::handleAspectSubjectChanged
                    }
                }
                consoleButtonsGroup(
                    onSubmitClick = ::tryMakeSubmitAspectRequest,
                    onAddToListClick = ::trySwitchToProperties,
                    onCancelClick = props.editConsoleModel::discardChanges,
                    onDeleteClick = { tryDelete(false) }
                )
                descriptionComponent(
                    className = "aspect-edit-console--description-icon",
                    description = props.aspect.description,
                    onEditStarted = null,
                    onNewDescriptionConfirmed = this@AspectEditConsole::handleAspectDescriptionChanged
                )
            }
        }
        forceRemoveConfirmWindow {
            attrs {
                onConfirm = { tryDelete(true) }
                onCancel = { setState { confirmation = false } }
                isOpen = state.confirmation
                message = "Aspect has linked entities."
            }
        }
    }

    interface Props : RProps {
        var aspect: AspectData
        var editConsoleModel: AspectEditConsoleModel
    }

    interface State : RState {
        var confirmation: Boolean
    }
}

fun RBuilder.aspectEditConsole(block: RHandler<AspectEditConsole.Props>) = child(AspectEditConsole::class, block)