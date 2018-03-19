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
            require("styles/aspect-edit-console.scss")
        }
    }

    override fun State.init(props: AspectApiReceiverProps) {
        selectedAspect = if (!props.loading)
            AspectData(null, "", null, null, null)
        else null
        selectedAspectPropertyIndex = null
    }

    override fun componentWillReceiveProps(nextProps: AspectApiReceiverProps) {
        if (props.loading && !nextProps.loading) {
            setState {
                selectedAspect = AspectData(null, "", null, null, null)
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
            selectedAspect = AspectData(null, "", null, null, null)
            selectedAspectPropertyIndex = null
        }
    }

    private fun handleSubmitAspectChanges(aspectData: AspectData) {
        if (aspectData.id == null) {
            setState {
                selectedAspect = AspectData(null, "", null, null, null)
            }
            props.onAspectCreate(aspectData)
        } else {
            val existingAspect = state.selectedAspect
            setState {
                selectedAspect = AspectData(null, "", null, null, null)
            }
            if (existingAspect != aspectData) {
                props.onAspectUpdate(aspectData)
            }
        }
    }

    private fun handleSwitchToAspectProperties(aspect: AspectData) {
        setState {
            if (aspect.properties.isEmpty()) {
                selectedAspect = aspect.copy(
                        name = aspect.name,
                        measure = aspect.measure,
                        domain = aspect.domain,
                        baseType = aspect.baseType,
                        properties = aspect.properties + AspectPropertyData("", "", "", "")
                )
            } else {
                selectedAspect = aspect.copy(
                        name = aspect.name,
                        measure = aspect.measure,
                        domain = aspect.domain,
                        baseType = aspect.baseType
                )
            }
            selectedAspectPropertyIndex = 0
        }
    }

    private fun handleSwitchToNextProperty(aspectProperty: AspectPropertyData) = setState {
        val currentSelectedAspect = selectedAspect ?: error("handleSwitchToNextProperty when no aspect is selected")
        val currentSelectedAspectPropertyIndex = selectedAspectPropertyIndex
                ?: error("handleSwitchToNextProperty when no property is selected")
        selectedAspect = if (currentSelectedAspect.properties.lastIndex == currentSelectedAspectPropertyIndex)
            currentSelectedAspect.copy(
                    properties = currentSelectedAspect.properties.mapIndexed { index, property ->
                        if (index != currentSelectedAspectPropertyIndex) {
                            property
                        } else {
                            property.copy(
                                    name = aspectProperty.name,
                                    cardinality = aspectProperty.cardinality,
                                    aspectId = aspectProperty.aspectId
                            )
                        }
                    } + AspectPropertyData("", "", "", "")
            )
        else
            currentSelectedAspect.copy(
                    properties = currentSelectedAspect.properties.mapIndexed { index, property ->
                        if (index != currentSelectedAspectPropertyIndex) {
                            property
                        } else {
                            property.copy(
                                    name = aspectProperty.name,
                                    cardinality = aspectProperty.cardinality,
                                    aspectId = aspectProperty.aspectId
                            )
                        }
                    }
            )
        selectedAspectPropertyIndex = currentSelectedAspectPropertyIndex + 1
    }

    private fun handleSaveParentAspect(aspectProperty: AspectPropertyData) {
        val currentSelectedAspect = state.selectedAspect
                ?: error("handleSwitchToNextProperty when no aspect is selected")
        val currentSelectedAspectPropertyIndex = state.selectedAspectPropertyIndex
                ?: error("handleSwitchToNextProperty when no property is selected")
        val savedAspect = currentSelectedAspect.copy(
                properties = currentSelectedAspect.properties.mapIndexed { index, property ->
                    if (index != currentSelectedAspectPropertyIndex) {
                        property
                    } else {
                        property.copy(
                                name = aspectProperty.name,
                                cardinality = aspectProperty.cardinality,
                                aspectId = aspectProperty.aspectId
                        )
                    }
                }
        )
        if (currentSelectedAspect.id == null) {
            props.onAspectCreate(savedAspect)
        } else {
            props.onAspectUpdate(savedAspect)
        }
        setState {
            selectedAspect = AspectData(null, "", null, null, null)
            selectedAspectPropertyIndex = null
        }
    }

    private fun handleClickAspectProperty(aspect: AspectData, aspectPropertyIndex: Int) {
        setState {
            selectedAspect = aspect
            selectedAspectPropertyIndex = aspectPropertyIndex
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
            }
        }
        when {
            selectedAspect != null && selectedAspectPropertyIndex == null ->
                aspectEditConsole {
                    attrs {
                        aspect = selectedAspect
                        onCancel = ::handleCancelChanges
                        onSubmit = ::handleSubmitAspectChanges
                        onSwitchToProperties = ::handleSwitchToAspectProperties
                    }
                }
            selectedAspect != null && selectedAspectPropertyIndex != null ->
                aspectPropertyEditConsole {
                    attrs {
                        parentAspect = selectedAspect
                        aspectPropertyIndex = selectedAspectPropertyIndex
                        childAspect = if (selectedAspect.properties[selectedAspectPropertyIndex].aspectId == "") null
                        else props.aspectContext[selectedAspect.properties[selectedAspectPropertyIndex].aspectId]!!
                        onCancel = ::handleCancelChanges
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
