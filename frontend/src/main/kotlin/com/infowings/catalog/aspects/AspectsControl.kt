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
        selectedAspectProperty = null
    }

    override fun componentWillReceiveProps(nextProps: AspectApiReceiverProps) {
        if (props.loading && !nextProps.loading) {
            setState {
                selectedAspect = AspectData(null, "", null, null, null)
                selectedAspectProperty = null
            }
        }
    }

    private fun handleClickAspect(aspect: AspectData) {
        setState {
            selectedAspectProperty = null
            selectedAspect = aspect
        }
    }

    private fun handleCancelChanges() {
        setState {
            selectedAspectProperty = null
            selectedAspect = AspectData(null, "", null, null, null)
        }
    }

    private fun handleSubmitAspectChanges(aspectData: AspectData) {
        if (aspectData.id == null) {
            setState {
                selectedAspect = AspectData(null, "", null, null, null)
            }
            props.onAspectCreate(aspectData)
        } else {
            setState {
                selectedAspect = AspectData(null, "", null, null, null)
            }
            props.onAspectUpdate(aspectData)
        }
    }

    private fun handleClickAspectProperty(aspectProperty: AspectPropertyData) {
        setState {
            selectedAspect = null
            selectedAspectProperty = aspectProperty
        }
    }

    private fun handleRequestNewAspect() {
        setState {
            selectedAspect = AspectData(null, "", null, null, null)
            selectedAspectProperty = null
        }
    }

    override fun RBuilder.render() {
        val selectedAspect = state.selectedAspect
        val selectedAspectProperty = state.selectedAspectProperty
        aspectTreeView {
            attrs {
                aspects = if (selectedAspect != null && selectedAspect.id == null)
                    props.data + selectedAspect
                else props.data
                aspectContext = props.aspectContext
                selectedId = when {
                    selectedAspect != null -> selectedAspect.id
                    selectedAspectProperty != null -> selectedAspectProperty.id
                    else -> null
                }
                onAspectClick = ::handleClickAspect
                onAspectPropertyClick = ::handleClickAspectProperty
                onNewAspectRequest = ::handleRequestNewAspect
            }
        }
        when {
            selectedAspect != null && selectedAspectProperty == null ->
                aspectEditConsole {
                    attrs {
                        aspect = selectedAspect
                        onCancel = ::handleCancelChanges
                        onSubmit = ::handleSubmitAspectChanges
                    }
                }
            selectedAspect == null && selectedAspectProperty != null ->
                aspectPropertyEditConsole {
                    attrs {
                        aspectProperty = selectedAspectProperty
                        childAspect = props.aspectContext[selectedAspectProperty.aspectId]!!
                        onCancel = ::handleCancelChanges
                    }
                }
        }
    }

    interface State : RState {
        var selectedAspect: AspectData?
        var selectedAspectProperty: AspectPropertyData?
    }
}
