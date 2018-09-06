package com.infowings.catalog.objects.edit.tree.inputs

import com.infowings.catalog.common.LinkValueData
import com.infowings.catalog.errors.showError
import com.infowings.catalog.utils.ApiException
import com.infowings.catalog.wrappers.blueprint.Spinner
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.html.INPUT
import kotlinx.html.InputType
import kotlinx.html.classes
import kotlinx.html.js.onBlurFunction
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import kotlinx.html.js.onKeyDownFunction
import org.w3c.dom.DataTransfer
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.get
import react.*
import react.dom.input
import react.dom.span

private const val EMPTY_GUID_VALUE_PLACEHOLDER = "Please, enter guid value"
private const val UNSUCCESSFUL_ENTITY_RETRIEVAL_PLACEHOLDER = "Could not retrieve entity from server"
private const val KEY_ENTER = 13
private const val KEY_ESCAPE = 27

class EntityLinkGuidInput(props: EntityLinkGuidInput.Props) : RComponent<EntityLinkGuidInput.Props, EntityLinkGuidInput.State>(props) {

    companion object {
        init {
            kotlinext.js.require("styles/entity-link-guid-input.scss")
        }
    }

    override fun State.init(props: EntityLinkGuidInput.Props) {
        editState = if (props.guid == null) EditState.SHOWING else EditState.LOADING
        newGuid = props.guid
        briefView = null
    }

    override fun componentDidMount() {
        loadBriefView(state.newGuid)
    }

    override fun componentDidUpdate(prevProps: Props, prevState: State, snapshot: Any) {
        if (props.guid != prevProps.guid && props.guid != state.newGuid) {
            loadBriefView(props.guid)
        }
    }

    private fun handlePaste(event: Event) {
        /*
         * Note: KotlinJS does not declare wrapper for such event as ClipboardEvent [https://developer.mozilla.org/en-US/docs/Web/API/ClipboardEvent],
         * so the temporary solution is to cast supplied event to dynamic and get DataTransfer object from dynamic js object.
         */
        val dataTransfer = event.asDynamic().clipboardData.unsafeCast<DataTransfer>()
        dataTransfer.withPlainText { loadBriefView(it) }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun handleBlur(event: Event) = setState {
        editState = EditState.SHOWING
    }

    @Suppress("UNUSED_PARAMETER")
    private fun handleSpanClick(event: Event) = setState {
        if (!props.disabled) {
            editState = EditState.EDITING
        }
    }

    private fun handleKeyEvent(event: Event) {
        val keyboardEvent = event.unsafeCast<KeyboardEvent>()
        val which = keyboardEvent.which
        if (which == KEY_ENTER || which == KEY_ESCAPE) {
            setState {
                editState = EditState.SHOWING
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun handleChange(event: Event) {}

    private suspend fun loadMeta(guid: String): String {
        delay(500)
        return "Object"
    }

    private suspend fun loadEntityBrief(entityType: String, guid: String) {
        delay(500)
        setState {
            briefView = "Object (My new Object) [Some Subject]"
        }
    }

    private fun loadBriefView(guid: String?) {
        guid?.let {
            setState {
                editState = EditState.LOADING
                newGuid = guid
            }
            launch {
                try {
                    val entityMeta = loadMeta(it)
                    loadEntityBrief(entityMeta, it)
                    setState {
                        editState = EditState.SHOWING
                    }
                    props.onUpdate(LinkValueData.Object(it))
                } catch (e: ApiException) {
                    showError(e)
                    setState {
                        editState = EditState.SHOWING
                        newGuid = props.guid
                    }
                }
            }
        } ?: run {
            setState {
                editState = EditState.SHOWING
                newGuid = null
            }
        }
    }

    override fun RBuilder.render() {
        when (state.editState) {
            EditState.EDITING -> renderInput()
            EditState.SHOWING -> renderSpan()
            EditState.LOADING -> renderSpinner()
        }
    }

    private fun RBuilder.renderInput() {
        input(type = InputType.text, classes = "pt-input") {
            attrs {
                value = ""
                placeholder = "Paste target entity GUID"
                autoFocus = true
                onPasteFunction = this@EntityLinkGuidInput::handlePaste
                onChangeFunction = this@EntityLinkGuidInput::handleChange
                onBlurFunction = this@EntityLinkGuidInput::handleBlur
                onKeyDownFunction = this@EntityLinkGuidInput::handleKeyEvent
            }
        }
    }

    private fun RBuilder.renderSpan() {
        span {
            state.briefView?.let {
                attrs {
                    classes = setOf("value-entity-guid--full")
                    onClickFunction = this@EntityLinkGuidInput::handleSpanClick
                }
                +it
            } ?: run {
                attrs {
                    classes = if (state.newGuid == null) setOf("value-entity-guid--empty") else setOf("value-entity-guid--unsuccessful")
                    onClickFunction = this@EntityLinkGuidInput::handleSpanClick
                }
                +(if (state.newGuid == null) EMPTY_GUID_VALUE_PLACEHOLDER else UNSUCCESSFUL_ENTITY_RETRIEVAL_PLACEHOLDER)
            }
        }
    }

    private fun RBuilder.renderSpinner() {
        Spinner {
            attrs {
                small = true
            }
        }
    }

    enum class EditState {
        EDITING, LOADING, SHOWING
    }

    interface Props : RProps {
        var guid: String?
        var onUpdate: (LinkValueData?) -> Unit
        var disabled: Boolean
    }

    interface State : RState {
        var editState: EditState
        var newGuid: String?
        var briefView: String?
    }
}

private fun DataTransfer.withPlainText(handler: (String?) -> Unit) {
    for (i in 0 until this.items.length) {
        if (this.types[i] == "text/plain") {
            this.items[i]?.getAsString(handler)
            return
        }
    }
}

var INPUT.onPasteFunction : (Event) -> Unit
    get()  = throw UnsupportedOperationException("You can't read variable onChange")
    set(newValue) {consumer.onTagEvent(this, "onpaste", newValue)}

fun RBuilder.entityLinkGuidInput(handler: RHandler<EntityLinkGuidInput.Props>) = child(EntityLinkGuidInput::class, handler)