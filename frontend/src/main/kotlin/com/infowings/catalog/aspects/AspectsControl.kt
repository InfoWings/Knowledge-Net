package com.infowings.catalog.aspects

import com.infowings.catalog.aspects.editconsole.aspectEditConsole
import com.infowings.catalog.aspects.editconsole.aspectPropertyEditConsole
import com.infowings.catalog.aspects.treeview.aspectTreeView
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectPropertyData
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

    private fun handleDeleteAspect(aspect: AspectData) {
        setState {
            selectedAspect = emptyAspectData
            selectedAspectPropertyIndex = null
        }
    }

    private fun handleSubmitAspectChanges(aspectData: AspectData) {
        if (aspectData.id == null) {
            setState {
                selectedAspect = emptyAspectData
            }
            props.onAspectCreate(aspectData)
        } else {
            val existingAspect = state.selectedAspect
            setState {
                selectedAspect = emptyAspectData
            }
            if (existingAspect != aspectData) {
                props.onAspectUpdate(aspectData)
            }
        }
    }

    private fun handleSwitchToAspectProperties(aspect: AspectData) {

        val tmpAspect = aspect.copy(
            name = aspect.name,
            measure = aspect.measure,
            domain = aspect.domain,
            baseType = aspect.baseType,
            properties = aspect.properties
        )

        setState {
            selectedAspect = if (!aspect.hasNextAlivePropertyIndex(0)) tmpAspect.plusEmptyProperty() else tmpAspect
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


    private fun handleSaveParentAspect(aspectProperty: AspectPropertyData) {
        val currentSelectedAspect = state.selectedAspect
                ?: error("handleSwitchToNextProperty when no aspect is selected")
        val currentSelectedAspectPropertyIndex = state.selectedAspectPropertyIndex
                ?: error("handleSwitchToNextProperty when no property is selected")

        val savedAspect = currentSelectedAspect.changePropertyValues(currentSelectedAspectPropertyIndex, aspectProperty)
        currentSelectedAspect.id?.let { props.onAspectUpdate(savedAspect) } ?: props.onAspectCreate(savedAspect)

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
                        onDelete = ::handleDeleteAspect
                        onSubmit = ::handleSubmitAspectChanges
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
                        onSaveParentAspect = ::handleSaveParentAspect
                    }
                }
        }
    }

    interface State : RState {
        var selectedAspect: AspectData?
        var selectedAspectPropertyIndex: Int?
    }
}

fun AspectData.hasNextAlivePropertyIndex(index: Int) =
    properties.size > index && properties.subList(index, properties.size).indexOfFirst { !it.deleted } != -1

fun AspectData.nextAlivePropertyIndex(index: Int) =
    properties.subList(index, properties.size).indexOfFirst { !it.deleted } + index

fun AspectData.firstAlivePropertyIndex() =
    properties.indexOfFirst { !it.deleted }

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

private val emptyAspectData: AspectData
    get() = AspectData(null, "", null, null, null)

private val emptyAspectPropertyData: AspectPropertyData
    get() = AspectPropertyData("", "", "", "")

private fun AspectData.dropProperty(index: Int) =
    copy(properties = properties.filterIndexed { i, _ -> i != index }.toList())

private fun AspectData.plusEmptyProperty() = copy(
    properties = properties + AspectPropertyData("", "", "", "")
)