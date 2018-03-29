package com.infowings.catalog.aspects

import com.infowings.catalog.aspects.editconsole.aspectConsole
import com.infowings.catalog.aspects.treeview.aspectTreeView
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import com.infowings.catalog.common.emptyAspectData
import com.infowings.catalog.common.emptyAspectPropertyData
import com.infowings.catalog.wrappers.react.setStateWithCallback
import react.RBuilder
import react.RComponent
import react.RState
import react.setState
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

class AspectsControl(props: AspectApiReceiverProps) : RComponent<AspectApiReceiverProps, AspectsControl.State>(props) {

    override fun State.init(props: AspectApiReceiverProps) {
        selectedAspect = emptyAspectData
        selectedAspectPropertyIndex = null
    }

    override fun componentWillReceiveProps(nextProps: AspectApiReceiverProps) {
        if (props.loading && !nextProps.loading) {
            setState {
                selectedAspect = emptyAspectData
                selectedAspectPropertyIndex = null
            }
        }
    }

    /**
     * Handler for selecting existing aspect (or new but not yet saved if [AspectData.id] == null)
     *
     * if supplied [AspectData.id] does not exist in context (should not happen at all), does nothing
     *
     * @param aspectId - [AspectData.id] of [AspectData] to select.
     */
    private fun handleSelectAspect(aspectId: String?) {
        setState {
            selectedAspect = when (aspectId) {
                selectedAspect.id -> selectedAspect // If we select aspect that is already selected, do nothing
                null -> emptyAspectData
                else -> props.aspectContext[aspectId] ?: selectedAspect
            }
            selectedAspectPropertyIndex = null
        }
    }

    /**
     * Handler for selecting property of existing (or new) aspect by index of the property inside the aspect.
     *
     * The reason for selecting aspect property by index instead of by id or instance is that new aspect properties
     * have all the same id (empty string) and there are no restrictions on the state of the properties while editing,
     * which means that there may be two properties with exact same content inside one aspect (may be use case for
     * copying). Validation regarding possible restrictions is performed on server side.
     *
     * @param aspectId - [AspectData.id] of parent [AspectData] of [AspectPropertyData] to select.
     * @param index - index of [AspectPropertyData] inside [AspectData.properties] list to select.
     */
    private fun handleSelectAspectProperty(aspectId: String?, index: Int) {
        setState {
            selectedAspect = when (aspectId) {
                selectedAspect.id -> selectedAspect // If we select aspect that is already selected, do nothing
                null -> emptyAspectData
                else -> props.aspectContext[aspectId] ?: selectedAspect
            }
            selectedAspectPropertyIndex = if (index > selectedAspect.properties.lastIndex)
                selectedAspect.properties.lastIndex else index
        }
    }

    /**
     * Handler for canceling selected state.
     *
     * By default resets state to creating new aspect.
     */
    private fun handleCancelSelect() {
        setState {
            selectedAspect = emptyAspectData
            selectedAspectPropertyIndex = null
        }
    }

    /**
     * Handler for creating new [AspectPropertyData] inside currently selected [AspectData] at index.
     */
    private fun handleCreateNewProperty(index: Int) {
        setState {
            val currentlySelectedAspect = selectedAspect
            selectedAspect = currentlySelectedAspect.copy(
                properties = currentlySelectedAspect.properties.insertEmptyAtIndex(index)
            )
            selectedAspectPropertyIndex = if (0 <= index && index <= currentlySelectedAspect.properties.size)
                index else null
        }
    }

    /**
     * Handler for updating currently selected [AspectData]
     *
     * Made suspended in case if submission to server happens immediately after a call to this method (setState should
     * complete before submission to the server).
     */
    private suspend fun handleUpdateSelectedAspect(aspect: AspectData) = suspendCoroutine { cont: Continuation<Unit> ->
        setStateWithCallback({ cont.resume(Unit) }) {
            val currentlySelectedAspect = selectedAspect
            selectedAspect = currentlySelectedAspect.copy(
                name = aspect.name,
                measure = aspect.measure,
                domain = aspect.domain,
                baseType = aspect.baseType
            )
        }
    }

    /**
     * Handler for updating currently selected [AspectPropertyData]
     *
     * Made suspended in case if submission to server happens immediately after a call to this method (setState should
     * complete before submission to the server).
     */
    private suspend fun handleUpdateSelectedAspectProperty(aspectProperty: AspectPropertyData) = suspendCoroutine { cont: Continuation<Unit> ->
        setStateWithCallback({ cont.resume(Unit) }) {
            val currentlySelectedAspect = selectedAspect
            val currentlySelectedPropertyIndex = selectedAspectPropertyIndex
                    ?: error("Currently selected aspect property index should not be null")
            selectedAspect = currentlySelectedAspect.updatePropertyAtIndex(
                currentlySelectedPropertyIndex,
                aspectProperty
            )
        }
    }

    /**
     * Handler for submitting changes of currently selected [AspectData] to the server
     */
    private suspend fun handleSubmitSelectedAspect() {
        val selectedAspect = state.selectedAspect
        if (selectedAspect.id == null) {
            props.onAspectCreate(selectedAspect.normalize())
            setState {
                this.selectedAspect = emptyAspectData
                selectedAspectPropertyIndex = null
            }
        } else {
            props.onAspectUpdate(selectedAspect.normalize())
            setState {
                this.selectedAspect = emptyAspectData
                selectedAspectPropertyIndex = null
            }
        }
    }

    private suspend fun handleDeleteSelectedAspect(force: Boolean) {

        val aspectId = state.selectedAspect.id

        if (!aspectId.isNullOrEmpty()) {
            props.onAspectDelete(props.aspectContext[aspectId] ?: error("Incorrect aspect state"), force)
        }
        setState {
            selectedAspect = emptyAspectData
            selectedAspectPropertyIndex = null
        }
    }


    override fun RBuilder.render() {
        val selectedAspect = state.selectedAspect
        val selectedAspectPropertyIndex = state.selectedAspectPropertyIndex
        if (!props.loading) {
            aspectTreeView {
                attrs {
                    aspects = props.data.withSelected(state.selectedAspect)
                    aspectContext = { if (it == selectedAspect.id) selectedAspect else props.aspectContext[it] }
                    selectedAspectId = state.selectedAspect.id
                    selectedPropertyIndex = state.selectedAspectPropertyIndex
                    onAspectClick = ::handleSelectAspect
                    onAspectPropertyClick = ::handleSelectAspectProperty
                    onAddAspectProperty = ::handleCreateNewProperty
                }
            }
            aspectConsole {
                attrs {
                    aspect = selectedAspect
                    propertyIndex = selectedAspectPropertyIndex
                    aspectContext = props.aspectContext::get
                    onSelectProperty = ::handleSelectAspectProperty
                    onSelectAspect = ::handleSelectAspect
                    onCreateProperty = ::handleCreateNewProperty
                    onCancel = ::handleCancelSelect
                    onAspectUpdate = { handleUpdateSelectedAspect(it) }
                    onAspectPropertyUpdate = { handleUpdateSelectedAspectProperty(it) }
                    onAspectDelete = { handleDeleteSelectedAspect(it) }
                    onSubmit = { handleSubmitSelectedAspect() }
                }
            }
        }
    }

    interface State : RState {
        var selectedAspect: AspectData
        var selectedAspectPropertyIndex: Int?
    }
}

private fun List<AspectPropertyData>.insertEmptyAtIndex(suggestedIndex: Int): List<AspectPropertyData> {
    val tempProperties = this.toMutableList()
    val targetIndex = if (suggestedIndex > this.size) this.size else suggestedIndex
    tempProperties.add(targetIndex, emptyAspectPropertyData)
    return tempProperties.toList()
}

private fun AspectData.updatePropertyAtIndex(atIndex: Int, aspectProperty: AspectPropertyData) =
        this.copy(
            properties = this.properties.mapIndexed { index, existingProperty ->
                if (index == atIndex) {
                    existingProperty.copy(
                        name = aspectProperty.name,
                        cardinality = aspectProperty.cardinality,
                        aspectId = aspectProperty.aspectId,
                        deleted = aspectProperty.deleted
                    )
                } else {
                    existingProperty
                }
            }
        )

private fun List<AspectData>.withSelected(aspect: AspectData) =
            if (aspect.id == null) {
                this + aspect
            } else {
                this.map { if (it.id == aspect.id) aspect else it }
            }

private fun AspectData.normalize() =
    copy(properties = properties.filter { it != emptyAspectPropertyData && !(it.id.isEmpty() && it.deleted) })
