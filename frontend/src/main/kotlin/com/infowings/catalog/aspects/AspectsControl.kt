package com.infowings.catalog.aspects

import com.infowings.catalog.aspects.editconsole.aspectEditConsole
import com.infowings.catalog.aspects.editconsole.aspectPropertyEditConsole
import com.infowings.catalog.aspects.treeview.aspectTreeView
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
import com.infowings.catalog.common.emptyAspectData
import com.infowings.catalog.common.emptyAspectPropertyData
import kotlinext.js.invoke
import kotlinext.js.require
import react.RBuilder
import react.RComponent
import react.RState
import react.setState

class AspectsControl(props: AspectApiReceiverProps) : RComponent<AspectApiReceiverProps, AspectsControl.State>(props) {

    companion object {
        init {
            require("styles/aspect-edit-console.scss") // Styles regarding aspect console
        }
    }

    override fun State.init(props: AspectApiReceiverProps) {
        selectedAspect = if (!props.loading) emptyAspectData else null
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

    private fun handleClickAspect(aspect: AspectData) {
        setState {
            selectedAspect = aspect
            selectedAspectPropertyIndex = null
        }
    }

    private fun handleCancelChanges() {
        setState {
            selectedAspect = emptyAspectData
            selectedAspectPropertyIndex = null
        }
    }

    private suspend fun handleDeleteAspect(force: Boolean) {

        val aspectId = state.selectedAspect?.id

        if (aspectId.isNullOrEmpty()) {
            setState {
                selectedAspect = emptyAspectData
                selectedAspectPropertyIndex = null
            }
        } else {
            props.onAspectDelete(props.aspectContext[aspectId] ?: error("Incorrect aspect state"), force)
            setState {
                selectedAspect = emptyAspectData
                selectedAspectPropertyIndex = null
            }
        }
    }

    private suspend fun handleSubmitAspectChanges(aspectData: AspectData) {
        if (aspectData.id == null) {
            props.onAspectCreate(aspectData.normalize())
            setState {
                selectedAspect = emptyAspectData
            }
        } else {
            val existingAspect = state.selectedAspect
            if (existingAspect != aspectData) {
                props.onAspectUpdate(aspectData.normalize())
            }
            setState {
                selectedAspect = emptyAspectData
            }
        }
    }

    private fun handleSwitchToAspectProperties(aspect: AspectData) {

        val tmpAspect = aspect.copy(
            name = aspect.name,
            measure = aspect.measure,
            domain = aspect.domain,
            baseType = aspect.baseType,
            properties = aspect.properties,
                    subject = aspect.subject
        )

        setState {
            selectedAspect = if (!tmpAspect.hasNextAlivePropertyIndex(0)) tmpAspect.plusEmptyProperty() else tmpAspect
            selectedAspectPropertyIndex = selectedAspect!!.nextAlivePropertyIndex(0)
        }
    }

    private fun handleDeleteProperty() = setState {

        val currentSelectedAspect = selectedAspect ?: error("handleSwitchToNextProperty when no aspect is selected")
        val currentSelectedAspectPropertyIndex = selectedAspectPropertyIndex
                ?: error("handleSwitchToNextProperty when no property is selected")

        val property = currentSelectedAspect.properties[currentSelectedAspectPropertyIndex]
        selectedAspect = currentSelectedAspect.changePropertyValues(
            currentSelectedAspectPropertyIndex,
            property.copy(deleted = true)
        )

        if (!selectedAspect!!.hasNextAlivePropertyIndex(currentSelectedAspectPropertyIndex)) {
            selectedAspectPropertyIndex = null
        } else {
            selectedAspectPropertyIndex = selectedAspect!!.nextAlivePropertyIndex(currentSelectedAspectPropertyIndex)
        }
    }

    private fun handleSwitchToNextProperty(aspectProperty: AspectPropertyData) = setState {

        val currentSelectedAspect = selectedAspect ?: error("handleSwitchToNextProperty when no aspect is selected")
        var currentSelectedAspectPropertyIndex = selectedAspectPropertyIndex
                ?: error("handleSwitchToNextProperty when no property is selected")

        if (aspectProperty != emptyAspectPropertyData) {

            selectedAspect =
                    currentSelectedAspect.changePropertyValues(currentSelectedAspectPropertyIndex, aspectProperty)

            currentSelectedAspectPropertyIndex++
            if (currentSelectedAspectPropertyIndex == currentSelectedAspect.properties.size
                || !currentSelectedAspect.hasNextAlivePropertyIndex(currentSelectedAspectPropertyIndex)
            ) {

                selectedAspect = selectedAspect!!.plusEmptyProperty()
            }
            selectedAspectPropertyIndex = selectedAspect!!.nextAlivePropertyIndex(currentSelectedAspectPropertyIndex)
        }
    }

    private suspend fun handleSaveParentAspect(aspectProperty: AspectPropertyData) {
        val currentSelectedAspect = state.selectedAspect
                ?: error("handleSwitchToNextProperty when no aspect is selected")
        val currentSelectedAspectPropertyIndex = state.selectedAspectPropertyIndex
                ?: error("handleSwitchToNextProperty when no property is selected")

        val savedAspect = currentSelectedAspect.changePropertyValues(currentSelectedAspectPropertyIndex, aspectProperty)
        currentSelectedAspect.id?.let { props.onAspectUpdate(savedAspect.normalize()) }
                ?: props.onAspectCreate(savedAspect.normalize())

        setState {
            selectedAspect = emptyAspectData
            selectedAspectPropertyIndex = null
        }
    }

    private fun handleClickAspectProperty(aspect: AspectData, aspectPropertyIndex: Int) {
        setState {
            selectedAspect = aspect
            selectedAspectPropertyIndex = aspectPropertyIndex
        }
    }

    private fun handleClickAddPropertyToAspect(aspect: AspectData) {
        setState {
            selectedAspect = aspect.copy(properties = aspect.properties + emptyAspectPropertyData)
            selectedAspectPropertyIndex = aspect.properties.size
        }
    }

    override fun RBuilder.render() {
        val selectedAspect = state.selectedAspect
        val selectedAspectPropertyIndex = state.selectedAspectPropertyIndex
        aspectTreeView {
            attrs {
                aspects = if (selectedAspect != null && selectedAspect.id == null)
                    props.data + selectedAspect
                else props.data
                aspectContext = props.aspectContext
                this.selectedAspect = state.selectedAspect
                selectedPropertyIndex = state.selectedAspectPropertyIndex
                onAspectClick = ::handleClickAspect
                onAspectPropertyClick = ::handleClickAspectProperty
                onNewAspectPropertyRequest = ::handleClickAddPropertyToAspect
            }
        }
        when {
            selectedAspect != null && selectedAspectPropertyIndex == null ->
                aspectEditConsole {
                    attrs {
                        aspect = selectedAspect
                        onCancel = ::handleCancelChanges
                        onSubmit = { handleSubmitAspectChanges(it) }
                        onDelete = { handleDeleteAspect(it) }
                        onSwitchToProperties = ::handleSwitchToAspectProperties
                    }
                }
            selectedAspect != null && selectedAspectPropertyIndex != null ->
                aspectPropertyEditConsole {
                    attrs {
                        parentAspect = selectedAspect
                        aspectPropertyIndex = selectedAspectPropertyIndex
                        childAspect =
                                if (selectedAspect.properties[selectedAspectPropertyIndex].aspectId == "") null
                                else props.aspectContext[selectedAspect.properties[selectedAspectPropertyIndex].aspectId]!!
                        onCancel = ::handleCancelChanges
                        onDelete = ::handleDeleteProperty
                        onSwitchToNextProperty = ::handleSwitchToNextProperty
                        onSaveParentAspect = { handleSaveParentAspect(it) }
                    }
                }
        }
    }

    interface State : RState {
        var selectedAspect: AspectData?
        var selectedAspectPropertyIndex: Int?
    }
}

/** Possibly should be moved somewhere during refactoring. For example to file AspectDataFrontendExtensions.  */

private fun AspectData.hasNextAlivePropertyIndex(index: Int) =
    properties.size > index && properties.subList(index, properties.size).indexOfFirst { !it.deleted } != -1

private fun AspectData.nextAlivePropertyIndex(index: Int) =
    properties.subList(index, properties.size).indexOfFirst { !it.deleted } + index

private fun AspectData.changePropertyValues(index: Int, aspectProperty: AspectPropertyData) = copy(
    properties = properties.mapIndexed { i, property ->
        if (i != index) {
            property
        } else {
            property.copy(
                name = aspectProperty.name,
                cardinality = aspectProperty.cardinality,
                aspectId = aspectProperty.aspectId,
                deleted = aspectProperty.deleted
            )
        }
    }
)

private fun AspectData.plusEmptyProperty() = copy(
    properties = properties + emptyAspectPropertyData
)

private fun AspectData.normalize() =
    copy(properties = properties.filter { it != emptyAspectPropertyData && !(it.id.isEmpty() && it.deleted) })
