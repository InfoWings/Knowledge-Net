package com.infowings.catalog.aspects.editconsole

import com.infowings.catalog.aspects.AspectBadRequestException
import com.infowings.catalog.aspects.editconsole.aspect.aspectBaseTypeInput
import com.infowings.catalog.aspects.editconsole.aspect.aspectDomainInput
import com.infowings.catalog.aspects.editconsole.aspect.aspectMeasureInput
import com.infowings.catalog.aspects.editconsole.aspect.aspectNameInput
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.GlobalMeasureMap
import com.infowings.catalog.common.SubjectData
import com.infowings.catalog.utils.addToListIcon
import com.infowings.catalog.utils.checkIcon
import com.infowings.catalog.utils.crossIcon
import com.infowings.catalog.utils.ripIcon
import kotlinx.coroutines.experimental.launch
import kotlinx.html.js.onClickFunction
import kotlinx.html.js.onKeyDownFunction
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import react.*
import react.dom.div
import react.dom.span

class AspectEditConsole(props: Props) : RComponent<AspectEditConsole.Props, AspectEditConsole.State>(props) {

    private var inputRef: HTMLInputElement? = null
    private var aspectChanged: Boolean = false
    private val currentState
        get() = props.aspect.copy(
            name = state.aspectName ?: error("Aspect Name is null"),
            measure = if (state.aspectMeasure.isNullOrEmpty()) null else state.aspectMeasure,
            domain = if (state.aspectDomain.isNullOrEmpty()) null else state.aspectDomain,
            baseType = if (state.aspectBaseType.isNullOrEmpty()) null else state.aspectBaseType
        )

    override fun State.init(props: Props) {
        aspectName = props.aspect.name
        aspectMeasure = props.aspect.measure
        aspectDomain = props.aspect.domain
        aspectBaseType = props.aspect.baseType
        aspectName = props.aspect.subject?.name ?: ""
        confirmation = false
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
            aspectSubject = nextProps.aspect.subject
            badRequestErrorMessage = null
        }
    }

    private fun handleSwitchToPropertiesClick(e: Event) {
        e.preventDefault()
        e.stopPropagation()
        aspectChanged = true
        inputRef?.blur()
        props.onSwitchToProperties(currentState)
    }

    private fun handleSubmitAspectClick(e: Event) {
        e.preventDefault()
        e.stopPropagation()
        aspectChanged = true
        inputRef?.blur()
        tryMakeSubmitAspectRequest()
    }

    private fun tryMakeSubmitAspectRequest() {
        launch {
            try {
                props.onSubmit(currentState)
            } catch (exception: AspectBadRequestException) {
                setState {
                    badRequestErrorMessage = exception.message
                }
            }
        }
    }

    private fun handleCancelClick(e: Event) {
        e.preventDefault()
        e.stopPropagation()
        aspectChanged = true
        inputRef?.blur()
        props.onCancel()
    }

    private fun handleDeleteClick(e: Event) {
        e.preventDefault()
        e.stopPropagation()
        aspectChanged = true
        inputRef?.blur()
        tryDelete(false)
    }

    private fun tryDelete(force: Boolean) {
        launch {
            try {
                props.onDelete(force)
                setState {
                    confirmation = false
                }
            } catch (ex: AspectBadRequestException) {
                if (ex.exceptionInfo.code == AspectBadRequestCode.NEED_CONFIRMATION) {
                    setState {
                        confirmation = true
                    }
                } else {
                    throw ex
                }
            }
        }
    }

    private fun assignInputRef(element: HTMLInputElement?) {
        inputRef = element
    }

    private fun handleKeyDown(e: Event) {
        e.stopPropagation()
        val keyCode = e.unsafeCast<KeyboardEvent>().keyCode
        val ctrlPressed = e.unsafeCast<KeyboardEvent>().ctrlKey
        when {
            keyCode == 27 -> {
                aspectChanged = true
                inputRef?.blur()
                props.onCancel()
            }
            keyCode == 13 && ctrlPressed -> {
                aspectChanged = true
                inputRef?.blur()
                props.onSwitchToProperties(currentState)
            }
            keyCode == 13 && !ctrlPressed -> {
                aspectChanged = true
                inputRef?.blur()
                tryMakeSubmitAspectRequest()
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
            aspectBaseType = GlobalMeasureMap[measure]?.baseType?.name
        }
    }

    private fun handleAspectSubjectChanged(subjectName: String, subjetId: String) {
        setState {
            aspectSubject = SubjectData(id = subjetId, name = subjectName)
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
                aspectSubjectInput {
                    attrs {
                        value = state.aspectSubject?.name ?: ""
                        onChange = ::handleAspectSubjectChanged
                    }
                }
                div(classes = "aspect-edit-console--button-control-tab") {
                    div(classes = "aspect-edit-console--button-control") {
                        attrs {
                            onClickFunction = ::handleSubmitAspectClick
                        }
                        checkIcon("aspect-edit-console--button-icon aspect-edit-console--button-icon__green") {}
                    }
                    div(classes = "aspect-edit-console--button-control") {
                        attrs {
                            onClickFunction = ::handleSwitchToPropertiesClick
                        }
                        addToListIcon("aspect-edit-console--button-icon aspect-edit-console--button-icon__green") {}
                    }
                    div(classes = "aspect-edit-console--button-control") {
                        attrs {
                            onClickFunction = ::handleCancelClick
                        }
                        crossIcon("aspect-edit-console--button-icon aspect-edit-console--button-icon__yellow") {}
                    }
                    div(classes = "aspect-edit-console--button-control") {
                        attrs {
                            onClickFunction = ::handleDeleteClick
                        }
                        ripIcon("aspect-edit-console--button-icon aspect-edit-console--button-icon__red") {}
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
        if (state.confirmation) {
            popup {
                attrs.closePopup = { setState { confirmation = false } }

                removeConfirmWindow {
                    attrs {
                        onCancel = { setState { confirmation = false } }
                        onConfirm = { tryDelete(true) }
                    }
                }
            }
        }
    }

    interface Props : RProps {
        var aspect: AspectData
        var onCancel: () -> Unit
        var onSubmit: suspend (AspectData) -> Unit
        var onDelete: suspend (Boolean) -> Unit
        var onSwitchToProperties: (AspectData) -> Unit
    }

    interface State : RState {
        var aspectName: String?
        var aspectMeasure: String?
        var aspectDomain: String?
        var aspectBaseType: String?
        var aspectSubject: SubjectData?
        var badRequestErrorMessage: String?
        var confirmation: Boolean
    }
}

fun RBuilder.aspectEditConsole(block: RHandler<AspectEditConsole.Props>) = child(AspectEditConsole::class, block)