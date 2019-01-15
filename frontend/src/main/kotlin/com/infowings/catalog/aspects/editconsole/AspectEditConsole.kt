package com.infowings.catalog.aspects.editconsole

import com.infowings.catalog.aspects.AspectBadRequestException
import com.infowings.catalog.aspects.editconsole.aspect.*
import com.infowings.catalog.aspects.editconsole.view.aspectConsoleBlock
import com.infowings.catalog.aspects.editconsole.view.consoleButtonsGroup
import com.infowings.catalog.common.*
import com.infowings.catalog.components.description.descriptionComponent
import com.infowings.catalog.components.popup.forceRemoveConfirmWindow
import com.infowings.catalog.components.popup.confirmRefBookRemovalWindow
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

    private fun handleAspectMeasureChanged(measure: String?) {
        val newAspect = props.aspect.copy(
            measure = measure,
            baseType = measure?.let { GlobalMeasureMap[it]?.baseType?.name }
        )
        props.editConsoleModel.updateAspect(newAspect)

        updateRefBookRemoveConfirmation(newAspect)
    }

    private fun handleAspectSubjectChanged(subjectData: SubjectData?) {
        props.editConsoleModel.updateAspect(
            props.aspect.copy(subject = subjectData)
        )
    }

    private fun handleAspectDomainChanged(domain: String?) {
        props.editConsoleModel.updateAspect(
            props.aspect.copy(domain = domain)
        )
    }

    private fun handleAspectBaseTypeChanged(baseType: String?) {
        val newAspect = props.aspect.copy(baseType = baseType)
        props.editConsoleModel.updateAspect(newAspect)
        updateRefBookRemoveConfirmation(newAspect)
    }

    private fun handleAspectDescriptionChanged(description: String) {
        props.editConsoleModel.updateAspect(
            props.aspect.copy(description = description)
        )
    }

    private fun handleRefBookRemovalConfirm() {
        val newAspect = props.aspect.copy(refBookName = null)
        props.editConsoleModel.updateAspect(newAspect)
        setState {
            confirmationRefBookRemoval = false
        }
    }

    private fun handleRefBookRemovalCancel() {
        val newAspect = props.aspect.copy(measure = null, baseType = BaseType.Text.name)
        props.editConsoleModel.updateAspect(newAspect)
        setState {
            confirmationRefBookRemoval = false
        }
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
                        aspectIsUpdated = props.aspectIsUpdated
                    }
                }
                aspectMeasureInput {
                    attrs {
                        value = if (props.aspect.refBookName != null) null else props.aspect.measure
                        onChange = ::handleAspectMeasureChanged
                    }
                }
                aspectDomainInput {
                    attrs {
                        value = props.aspect.refBookName ?: props.aspect.domain
                        // показываем имя справочника, если есть
                        // возможно, надо как-то handleAspectDomainChanged менять под такой случай, но у нас это поле
                        // вроде бы всегда disabled. А если оно может быть не disabled, то надо иметь
                        // понятную интерпретацию нововведенного значения
                        onChange = ::handleAspectDomainChanged
                    }
                }
                aspectBaseTypeInput {
                    attrs {
                        value = if (props.aspect.refBookName != null) null else props.aspect.baseType
                        disabled = !props.aspect.measure.isNullOrEmpty()
                        onChange = ::handleAspectBaseTypeChanged
                    }
                }
                aspectSubjectInput {
                    attrs {
                        value = props.aspect.subject
                        onChange = ::handleAspectSubjectChanged
                    }
                }
                consoleButtonsGroup(
                    onSubmitClick = ::tryMakeSubmitAspectRequest,
                    onAddToListClick = ::trySwitchToProperties,
                    onCancelClick = {
                        props.editConsoleModel.discardChanges()
                    },
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
                onCancel = { setState {
                    confirmation = false

                } }
                isOpen = state.confirmation
                message = "Aspect has linked entities."
            }
        }

        confirmRefBookRemovalWindow {
            attrs {
                onConfirm = ::handleRefBookRemovalConfirm
                onCancel = ::handleRefBookRemovalCancel
                isOpen = state.confirmationRefBookRemoval
                message = "It will delete reference book"
            }
        }
    }

    fun updateRefBookRemoveConfirmation(newAspect: AspectData) {
        val hasRefBook = newAspect.refBookName != null
        val hasMeasure = newAspect.measure != null
        val baseTypeNonString = newAspect.baseType != BaseType.Text.name
        val criteria = hasRefBook && (hasMeasure || baseTypeNonString)
        setState { confirmationRefBookRemoval = criteria }
    }

    interface Props : RProps {
        var aspect: AspectData
        var aspectIsUpdated: Boolean
        var editConsoleModel: AspectEditConsoleModel
    }

    interface State : RState {
        var confirmation: Boolean
        var confirmationRefBookRemoval: Boolean
    }
}

fun RBuilder.aspectEditConsole(block: RHandler<AspectEditConsole.Props>) = child(AspectEditConsole::class, block)