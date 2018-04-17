package com.infowings.catalog.aspects

import com.infowings.catalog.aspects.editconsole.aspectConsole
import com.infowings.catalog.aspects.editconsole.popup.unsafeChangesWindow
import com.infowings.catalog.aspects.sort.aspectSort
import com.infowings.catalog.aspects.treeview.aspectTreeView
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import com.infowings.catalog.common.emptyAspectData
import com.infowings.catalog.common.emptyAspectPropertyData
import react.RBuilder
import react.RComponent
import react.RState
import react.setState
import kotlin.math.min

interface AspectsModel {
    /**
     * Method for selecting existing aspect (or new but not yet saved if [AspectData.id] == null)
     *
     * if supplied [AspectData.id] does not exist in context (should not happen at all), does nothing
     *
     * @param aspectId - [AspectData.id] of [AspectData] to select.
     */
    fun selectAspect(aspectId: String?)

    /**
     * Method for selecting property of existing (or new) aspect by index of the property inside the aspect.
     *
     * The reason for selecting aspect property by index instead of by id or instance is that new aspect properties
     * have all the same id (empty string) and there are no restrictions on the state of the properties while editing,
     * which means that there may be two properties with exact same content inside one aspect (may be use case for
     * copying). Validation regarding possible restrictions is performed on server side.
     *
     * @param index - index of [AspectPropertyData] inside [AspectData.properties] list to select.
     */
    fun selectProperty(index: Int)

    /**
     * Method for canceling selected state.
     *
     * By default resets state to creating new aspect.
     */
    fun discardSelect()

    /**
     * Method for creating new [AspectPropertyData] inside currently selected [AspectData] at index.
     */
    fun createProperty(index: Int)

    /**
     * Method for updating currently selected [AspectData]
     *
     * Made suspended in case if submission to server happens immediately after a call to this method (setState should
     * complete before submission to the server).
     */
    fun updateAspect(aspect: AspectData)

    /**
     * Method for updating currently selected [AspectPropertyData]
     *
     * Made suspended in case if submission to server happens immediately after a call to this method (setState should
     * complete before submission to the server).
     */
    fun updateProperty(property: AspectPropertyData)

    /**
     * Method for submitting changes of currently selected [AspectData] to the server
     */
    suspend fun submitAspect()

    /**
     * Method for requesting delete of currently selected [AspectData] to the server
     */
    suspend fun deleteAspect(force: Boolean)

    suspend fun deleteAspectProperty()
}

class AspectsModelComponent : RComponent<AspectApiReceiverProps, AspectsModelComponent.State>(), AspectsModel {

    override fun State.init() {
        selectedAspect = emptyAspectData
        selectedAspectPropertyIndex = null
        unsafeSelection = false
    }

    override fun selectAspect(aspectId: String?) {
        setState {
            if (unsavedDataSelection(aspectId, null)) {
                unsafeSelection = true
            } else {
                selectedAspect = newAspectSelection(aspectId)
                if (selectedAspect.properties.lastOrNull() == emptyAspectPropertyData) { // If there was an empty last property
                    selectedAspect = selectedAspect.copy(properties = selectedAspect.properties.dropLast(1)) //drop it
                }
                selectedAspectPropertyIndex = null
            }
        }
    }

    override fun selectProperty(index: Int) {
        setState {
            if (unsavedDataSelection(selectedAspect.id, index)) {
                unsafeSelection = true
            } else {
                if (selectedAspect.properties.lastOrNull() == emptyAspectPropertyData && index < selectedAspect.properties.lastIndex) {
                    selectedAspect = selectedAspect.copy(properties = selectedAspect.properties.dropLast(1))
                }
                selectedAspectPropertyIndex = min(index, selectedAspect.properties.lastIndex)
            }
        }
    }

    override fun discardSelect() {
        setState {
            val selectedAspectId = selectedAspect.id
            val prevSelectedAspect = selectedAspect
            val selectedIndex = selectedAspectPropertyIndex
            selectedAspect =
                    if (selectedAspectId == null) emptyAspectData else props.aspectContext[selectedAspectId]!!
            selectedAspectPropertyIndex = when (selectedIndex) {
                null -> null
                else -> if (prevSelectedAspect.properties[selectedIndex] == emptyAspectPropertyData) null else selectedIndex
            }
        }
    }

    override fun createProperty(index: Int) {
        setState {
            val currentlySelectedAspect = selectedAspect

            selectedAspect = currentlySelectedAspect.copy(
                properties = currentlySelectedAspect.properties.insertEmptyAtIndex(index)
            )

            selectedAspectPropertyIndex = if (0 <= index && index <= currentlySelectedAspect.properties.size)
                index else null
        }
    }

    override fun updateAspect(aspect: AspectData) =
        setState {
            selectedAspect = selectedAspect.copy(
                name = aspect.name,
                measure = aspect.measure,
                domain = aspect.domain,
                baseType = aspect.baseType,
                description = aspect.description,
                subject = aspect.subject
            )
        }

    override fun updateProperty(property: AspectPropertyData) =
        setState {
            val currentlySelectedAspect = selectedAspect
            val currentlySelectedPropertyIndex = selectedAspectPropertyIndex
                    ?: error("Currently selected aspect property index should not be null")

            selectedAspect = currentlySelectedAspect.updatePropertyAtIndex(
                currentlySelectedPropertyIndex,
                property
            )
        }

    override suspend fun submitAspect() {
        val selectedAspect = state.selectedAspect

        val aspect = when (selectedAspect.id) {
            null -> props.onAspectCreate(selectedAspect.normalize())
            else -> props.onAspectUpdate(selectedAspect.normalize())
        }

        setState {
            this.selectedAspect = when (selectedAspect.properties.lastOrNull()) {
                emptyAspectPropertyData -> aspect.copy(properties = aspect.properties + emptyAspectPropertyData)
                else -> aspect
            }
        }
    }

    override suspend fun deleteAspect(force: Boolean) {

        val aspectId = state.selectedAspect.id

        if (!aspectId.isNullOrEmpty()) {
            props.onAspectDelete(props.aspectContext[aspectId] ?: error("Incorrect aspect state"), force)
        }
        setState {
            selectedAspect = emptyAspectData
            selectedAspectPropertyIndex = null
        }
    }

    override suspend fun deleteAspectProperty() {
        val selectedPropertyIndex =
            state.selectedAspectPropertyIndex ?: error("Aspect Property should be selected in order to be deleted")
        val deletedAspectProperty = state.selectedAspect.properties[selectedPropertyIndex].copy(deleted = true)

        val updatedAspect = props.onAspectUpdate(
            state.selectedAspect.updatePropertyAtIndex(
                selectedPropertyIndex,
                deletedAspectProperty
            )
        )

        setState {
            selectedAspect = updatedAspect
            selectedAspectPropertyIndex = null
        }

    }

    private fun State.unsavedDataSelection(aspectId: String?, index: Int?): Boolean {
        return when {
            entityIsAlreadySelected(aspectId, index) -> false
            isEmptyPropertySelected() -> false
            isSelectedAspectHasChanges() -> true
            else -> false
        }
    }

    private fun State.isSelectedAspectHasChanges() =
        selectedAspect != props.aspectContext[selectedAspect.id] && selectedAspect != emptyAspectData

    private fun State.isEmptyPropertySelected() =
        selectedAspectPropertyIndex != null && selectedAspect.properties[selectedAspectPropertyIndex!!] == emptyAspectPropertyData

    private fun State.entityIsAlreadySelected(aspectId: String?, index: Int?) =
        selectedAspect.id == aspectId && selectedAspectPropertyIndex == index

    private fun State.newAspectSelection(aspectId: String?): AspectData {
        val selectedAspect = selectedAspect
        return when (aspectId) {
            selectedAspect.id -> selectedAspect // If we select aspect that is already selected, do nothing
            null -> emptyAspectData
            else -> props.aspectContext[aspectId] ?: selectedAspect
        }
    }

    override fun RBuilder.render() {
        val selectedAspect = state.selectedAspect
        val selectedAspectPropertyIndex = state.selectedAspectPropertyIndex
        if (!props.loading) {
            aspectSort {
                attrs {
                    onFetchAspect = props.onFetchAspects
                }
            }
            aspectTreeView {
                attrs {
                    aspects = props.data
                    aspectContext = props.aspectContext
                    selectedAspectId = state.selectedAspect.id
                    selectedPropertyIndex = state.selectedAspectPropertyIndex
                    aspectsModel = this@AspectsModelComponent
                }
            }
            aspectConsole {
                attrs {
                    aspect = selectedAspect
                    propertyIndex = selectedAspectPropertyIndex
                    aspectContext = props.aspectContext
                    aspectsModel = this@AspectsModelComponent
                }
            }
            unsafeChangesWindow(state.unsafeSelection) {
                setState { unsafeSelection = false }
            }
        }
    }

    interface State : RState {
        var selectedAspect: AspectData
        var selectedAspectPropertyIndex: Int?
        var unsafeSelection: Boolean
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

private fun AspectData.normalize() =
    copy(properties = properties.filter { it != emptyAspectPropertyData && !(it.id.isEmpty() && it.deleted) })
