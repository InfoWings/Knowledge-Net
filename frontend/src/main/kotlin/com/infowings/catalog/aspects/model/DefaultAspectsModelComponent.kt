package com.infowings.catalog.aspects.model

import com.infowings.catalog.aspects.*
import com.infowings.catalog.aspects.filter.AspectsFilter
import com.infowings.catalog.common.*
import com.infowings.catalog.utils.ServerException
import react.RBuilder
import react.RComponent
import react.RState
import react.setState
import kotlin.math.min


class DefaultAspectsModelComponent : RComponent<AspectApiReceiverProps, DefaultAspectsModelComponent.State>(),
    AspectsModel {

    override fun State.init() {
        selectedAspect = emptyAspectData
        selectedAspectPropertyIndex = null
        unsafeSelection = false
        errorMessages = emptyList()
        aspectsFilter = AspectsFilter(emptyList(), emptyList())
    }

    override fun componentWillReceiveProps(nextProps: AspectApiReceiverProps) {
        if (state.selectedAspect.id != null) {
            setState {
                val selectedAspectOnServer =
                    nextProps.aspectContext[selectedAspect.id] ?: error("Context must contain all aspects")

                selectedAspect = if (nextProps.refreshOperation) {
                    selectedAspectOnServer
                } else {
                    selectedAspect.copy(version = selectedAspectOnServer.version, deleted = selectedAspectOnServer.deleted)
                }
            }
        }
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
                aspectId?.let { props.refreshAspect(it) }
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
                else -> if (prevSelectedAspect.properties[selectedIndex].id == "") null else selectedIndex
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
                subject = aspect.subject,
                refBookName = aspect.refBookName
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
        tryMakeApiCall {
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
    }

    override suspend fun deleteAspect(force: Boolean) {
        tryMakeApiCall {
            val aspectId = state.selectedAspect.id

            if (!aspectId.isNullOrEmpty()) {
                props.onAspectDelete(props.aspectContext[aspectId] ?: error("Incorrect aspect state"), force)
            }
            setState {
                selectedAspect = emptyAspectData
                selectedAspectPropertyIndex = null
            }
        }
    }

    override suspend fun deleteAspectProperty(force: Boolean) {
        tryMakeApiCall {
            val selectedPropertyIndex =
                state.selectedAspectPropertyIndex ?: error("Aspect Property should be selected in order to be deleted")
            val aspectPropertyId = state.selectedAspect.properties[selectedPropertyIndex].id

            if (aspectPropertyId == "") {
                setState {
                    selectedAspect =
                            selectedAspect.copy(properties = selectedAspect.properties.filterIndexed { index, _ -> index != selectedAspectPropertyIndex })
                    selectedAspectPropertyIndex = null
                }
            } else {
                val parentAspectReference = props.onAspectPropertyDelete(aspectPropertyId, force)
                setState {
                    selectedAspect = selectedAspect.copy(
                        version = parentAspectReference.version,
                        properties = selectedAspect.properties.filterNot { it.id == aspectPropertyId }
                    )
                    selectedAspectPropertyIndex = null
                }
            }
        }
    }

    private fun setSubjectsFilter(subjects: List<SubjectData?>) = setState {
        aspectsFilter = aspectsFilter.copy(subjects = subjects)
    }

    private fun setExcludedAspectsToFilter(aspects: List<AspectHint>) = setState {
        aspectsFilter = aspectsFilter.copy(excludedAspects = aspects)
    }

    private inline fun tryMakeApiCall(block: () -> Unit) {
        try {
            block()
        } catch (exception: AspectBadRequestException) {
            if (exception.exceptionInfo.code == BadRequestCode.INCORRECT_INPUT) {
                setState {
                    exception.exceptionInfo.message?.let {
                        errorMessages += it
                    }
                }
            } else {
                throw exception
            }
        } catch (exception: ServerException) {
            setState {
                errorMessages += "Oops, something went wrong, changes were not saved"
            }
        }
    }

    private fun State.unsavedDataSelection(aspectId: String?, index: Int?): Boolean = when {
        entityIsAlreadySelected(aspectId, index) -> false
        isEmptyPropertySelected() -> false
        isSelectedAspectHasChanges() -> true
        else -> false
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
        if (!props.loading) {
            aspectPageHeader(
                onOrderByChanged = props.onOrderByChanged,
                onSearchQueryChanged = props.onSearchQueryChanged,
                filter = state.aspectsFilter,
                setFilterSubjects = ::setSubjectsFilter,
                setFilterAspects = ::setExcludedAspectsToFilter,
                refreshAspects = props.refreshAspects,
                aspectByGuid = props.data.mapNotNull { aspect -> aspect.guid?.let { it to aspect } }.toMap()
            )
            aspectPageContent(
                filteredAspects = state.aspectsFilter.applyToAspects(props.data),
                aspectContext = props.aspectContext,
                aspectsModel = this@DefaultAspectsModelComponent,
                selectedAspect = state.selectedAspect,
                selectedAspectPropertyIndex = state.selectedAspectPropertyIndex
            )
            aspectPageOverlay(
                isUnsafeSelection = state.unsafeSelection,
                onCloseUnsafeSelection = { setState { unsafeSelection = false } },
                errorMessages = state.errorMessages,
                onDismissErrorMessage = { errorMessage -> setState { errorMessages = errorMessages.filterNot { it == errorMessage } } }
            )
        }
    }

    interface State : RState {
        var selectedAspect: AspectData
        var selectedAspectPropertyIndex: Int?
        var unsafeSelection: Boolean
        var errorMessages: List<String>
        var aspectsFilter: AspectsFilter
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
                    description = aspectProperty.description,
                    deleted = aspectProperty.deleted
                )
            } else {
                existingProperty
            }
        }
    )

private fun AspectData.normalize() =
    copy(properties = properties.filter { it != emptyAspectPropertyData && !(it.id.isEmpty() && it.deleted) })
