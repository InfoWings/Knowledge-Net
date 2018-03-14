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

class AspectsControl : RComponent<AspectApiReceiverProps, AspectsControl.State>() {

    companion object {
        init {
            require("styles/aspect-edit-console.scss")
        }
    }

    override fun State.init() {
        selectedAspect = null
        selectedAspectProperty = null
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
            selectedAspect = null
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
        aspectTreeView {
            attrs {
                aspects = props.data
                aspectContext = props.aspectContext
                onAspectClick = ::handleClickAspect
                onAspectPropertyClick = ::handleClickAspectProperty
                onNewAspectRequest = ::handleRequestNewAspect
            }
        }
        when {
            state.selectedAspect != null && state.selectedAspectProperty == null ->
                aspectEditConsole {
                    attrs {
                        aspect = state.selectedAspect!!
                        onCancel = ::handleCancelChanges

                    }
                }
            state.selectedAspect == null && state.selectedAspectProperty != null ->
                aspectPropertyEditConsole {
                    attrs {
                        aspectProperty = state.selectedAspectProperty!!
                        childAspect = props.aspectContext[state.selectedAspectProperty!!.aspectId]!!
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
