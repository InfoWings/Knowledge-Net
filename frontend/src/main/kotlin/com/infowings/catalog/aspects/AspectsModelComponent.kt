package com.infowings.catalog.aspects

import com.infowings.catalog.aspects.editconsole.aspectConsole
import com.infowings.catalog.aspects.sort.aspectSort
import com.infowings.catalog.aspects.treeview.aspectTreeView
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import com.infowings.catalog.common.emptyAspectData
import com.infowings.catalog.common.emptyAspectPropertyData
import com.infowings.catalog.wrappers.react.suspendSetState
import react.RBuilder
import react.RComponent
import react.RState
import react.setState

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
     * @param aspectId - [AspectData.id] of parent [AspectData] of [AspectPropertyData] to select.
     * @param index - index of [AspectPropertyData] inside [AspectData.properties] list to select.
     */
    fun selectAspectProperty(aspectId: String?, index: Int)

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
    suspend fun updateAspect(aspect: AspectData)

    /**
     * Method for updating currently selected [AspectPropertyData]
     *
     * Made suspended in case if submission to server happens immediately after a call to this method (setState should
     * complete before submission to the server).
     */
    suspend fun updateProperty(property: AspectPropertyData)

    /**
     * Method for submitting changes of currently selected [AspectData] to the server
     */
    suspend fun submitAspect()

    /**
     * Method for requesting delete of currently selected [AspectData] to the server
     */
    suspend fun deleteAspect(force: Boolean)
}

class AspectsModelComponent(props: AspectApiReceiverProps) :
    RComponent<AspectApiReceiverProps, AspectsModelComponent.State>(props), AspectsModel {

    override fun State.init(props: AspectApiReceiverProps) {
        selectedAspect = emptyAspectData
        selectedAspectPropertyIndex = null
    }

    override fun selectAspect(aspectId: String?) {
        setState {
            selectedAspect = when (aspectId) {
                selectedAspect.id -> selectedAspect // If we select aspect that is already selected, do nothing
                null -> emptyAspectData
                else -> props.aspectContext[aspectId] ?: selectedAspect
            }

            if (selectedAspect.properties.lastOrNull() == emptyAspectPropertyData) { // If there was an empty last property
                selectedAspect = selectedAspect.copy(properties = selectedAspect.properties.dropLast(1)) //drop it
            }

            selectedAspectPropertyIndex = null
        }
    }

    override fun selectAspectProperty(aspectId: String?, index: Int) {
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

    override fun discardSelect() {
        setState {
            selectedAspect = emptyAspectData
            selectedAspectPropertyIndex = null
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

    override suspend fun updateAspect(aspect: AspectData) =
        suspendSetState {
            selectedAspect = selectedAspect.copy(
                name = aspect.name,
                measure = aspect.measure,
                domain = aspect.domain,
                baseType = aspect.baseType,
                subject = aspect.subject
            )
        }

    override suspend fun updateProperty(property: AspectPropertyData) =
        suspendSetState {
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

        when (selectedAspect.id) {
            null -> props.onAspectCreate(selectedAspect.normalize())
            else -> props.onAspectUpdate(selectedAspect.normalize())
        }

        setState {
            this.selectedAspect = emptyAspectData
            selectedAspectPropertyIndex = null
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
                    aspects = props.data.withSelected(state.selectedAspect)
                    aspectContext = { if (it == selectedAspect.id) selectedAspect else props.aspectContext[it] }
                    selectedAspectId = state.selectedAspect.id
                    selectedPropertyIndex = state.selectedAspectPropertyIndex
                    aspectsModel = this@AspectsModelComponent
                }
            }
            aspectConsole {
                attrs {
                    aspect = selectedAspect
                    propertyIndex = selectedAspectPropertyIndex
                    aspectContext = props.aspectContext::get
                    aspectsModel = this@AspectsModelComponent
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
    aspect.id?.let { id -> this.map { if (it.id == id) aspect else it } } ?: this+aspect

private fun AspectData.normalize() =
    copy(properties = properties.filter { it != emptyAspectPropertyData && !(it.id.isEmpty() && it.deleted) })
