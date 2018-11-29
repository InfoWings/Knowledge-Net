package com.infowings.catalog.objects.edit.tree.inputs

import com.infowings.catalog.common.LinkValueData
import com.infowings.catalog.common.guid.BriefObjectView
import com.infowings.catalog.common.guid.EntityClass
import com.infowings.catalog.common.guid.EntityMetadata
import com.infowings.catalog.errors.showError
import com.infowings.catalog.objects.*
import com.infowings.catalog.utils.ApiException
import com.infowings.catalog.utils.BadRequestException
import com.infowings.catalog.wrappers.History
import com.infowings.catalog.wrappers.RouteSuppliedProps
import com.infowings.catalog.wrappers.blueprint.Spinner
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

@Suppress("TooManyFunctions", "NotImplementedDeclaration")
class EntityLinkGuidInput(props: EntityLinkGuidInput.Props) : RComponent<EntityLinkGuidInput.Props, EntityLinkGuidInput.State>(props) {

    companion object {
        init {
            kotlinext.js.require("styles/entity-link-guid-input.scss")
        }
    }

    override fun State.init(props: EntityLinkGuidInput.Props) {
        editState = if (props.value == null) EditState.SHOWING else EditState.LOADING
        valueMeta = null
        briefInfo = null
    }

    override fun componentDidMount() {
        loadBriefViewById(props.value)
    }

    override fun componentDidUpdate(prevProps: Props, prevState: State, snapshot: Any) {
        if (props.value != prevProps.value && props.value?.id != state.valueMeta?.id) {
            loadBriefViewById(props.value)
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

    private fun handleChange(event: Event) {
        event.stopPropagation()
        event.preventDefault()
    }

    private suspend fun loadMeta(guid: String): EntityMetadata {
        val entityMeta = loadEntityMetadata(guid)
        setState {
            valueMeta = entityMeta
        }
        return entityMeta
    }

    private suspend fun loadEntityBrief(entityMetadata: EntityMetadata) {

        val briefEntityInfo = briefInfoGetters(props.history)[entityMetadata.entityClass]?.invoke(entityMetadata.guid) ?: run {
            showError(BadRequestException("Entity type ${entityMetadata.entityClass} is not supported", null))
            setState {
                editState = EditState.SHOWING
                briefInfo = null
            }
            TODO("Support EntityType: ${entityMetadata.entityClass}")
        }
        setState {
            briefInfo = briefEntityInfo
        }
    }

    private suspend fun loadEntityBriefById(linkValueData: LinkValueData) {
        val briefEntityInfo = when (linkValueData) {
            is LinkValueData.Object -> ObjectBriefInfo(linkValueData.getObjectBriefById(), props.history, true)
            is LinkValueData.ObjectValue -> ValueBriefInfo(getValueBriefById(linkValueData.id), props.history, true)
            else -> {
                showError(BadRequestException("Entity for link value $linkValueData is not yet supported", null))
                setState {
                    editState = EditState.SHOWING
                    briefInfo = null
                    valueMeta = null
                }
                TODO("Support type: $linkValueData")
            }
        }
        setState {
            briefInfo = briefEntityInfo
        }
    }

    private fun loadBriefViewById(id: LinkValueData?) {
        id?.let {
            launch {
                try {
                    loadEntityBriefById(it)
                    setState {
                        editState = EditState.SHOWING
                    }
                } catch (e: ApiException) {
                    showError(e)
                    setState {
                        editState = EditState.SHOWING
                    }
                }
            }
        } ?: run {
            setState {
                editState = EditState.SHOWING
                valueMeta = null
                briefInfo = null
            }
        }
    }

    private fun invokeOnUpdate(entityMetadata: EntityMetadata) {
        props.onUpdate(
            when (entityMetadata.entityClass) {
                EntityClass.OBJECT -> LinkValueData.Object(entityMetadata.id, entityMetadata.guid)
                EntityClass.OBJECT_VALUE -> LinkValueData.ObjectValue(entityMetadata.id, entityMetadata.guid)
                EntityClass.ASPECT -> LinkValueData.Aspect(entityMetadata.id, entityMetadata.guid)
                EntityClass.ASPECT_PROPERTY -> LinkValueData.AspectProperty(entityMetadata.id, entityMetadata.guid)
                EntityClass.OBJECT_PROPERTY -> LinkValueData.ObjectProperty(entityMetadata.id, entityMetadata.guid)
                EntityClass.REFBOOK_ITEM -> LinkValueData.RefBookItem(entityMetadata.id, entityMetadata.guid)
                EntityClass.SUBJECT -> LinkValueData.Subject(entityMetadata.id, entityMetadata.guid)
            }
        )
    }

    private fun loadBriefView(guid: String?) {
        guid?.let {
            setState {
                editState = EditState.LOADING
            }
            launch {
                try {
                    val entityMeta = loadMeta(it)
                    loadEntityBrief(entityMeta)
                    setState {
                        editState = EditState.SHOWING
                    }
                    invokeOnUpdate(entityMeta)
                } catch (e: ApiException) {
                    showError(e)
                    setState {
                        editState = EditState.SHOWING
                    }
                }
            }
        } ?: run {
            setState {
                editState = EditState.SHOWING
                valueMeta = null
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
            state.briefInfo?.let {
                attrs {
                    classes = setOf("value-entity-guid--full entity-brief-info")
                    onClickFunction = this@EntityLinkGuidInput::handleSpanClick
                }
                it.render(this)
            } ?: run {
                attrs {
                    classes = if (state.valueMeta == null) setOf("value-entity-guid--empty") else setOf("value-entity-guid--unsuccessful")
                    onClickFunction = this@EntityLinkGuidInput::handleSpanClick
                }
                +if (state.valueMeta == null) EMPTY_GUID_VALUE_PLACEHOLDER else UNSUCCESSFUL_ENTITY_RETRIEVAL_PLACEHOLDER
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

    interface Props : RouteSuppliedProps {
        var value: LinkValueData?
        var onUpdate: (LinkValueData?) -> Unit
        var disabled: Boolean
    }

    interface State : RState {
        var editState: EditState
        var valueMeta: EntityMetadata?
        var briefInfo: EntityBriefInfo?
    }
}

private fun briefInfoGetters(history: History): Map<EntityClass, BriefInfoGetter> = mapOf(
    EntityClass.OBJECT to { guid -> ObjectBriefInfo(BriefObjectView.of("noid", guid, getObjectBrief(guid)), history, true) },
    EntityClass.OBJECT_VALUE to { guid -> ValueBriefInfo(getValueBrief(guid), history, true) }
)

private typealias BriefInfoGetter = suspend (String) -> EntityBriefInfo

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