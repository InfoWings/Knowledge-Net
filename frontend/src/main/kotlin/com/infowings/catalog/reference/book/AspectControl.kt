package com.infowings.catalog.reference.book

import com.infowings.catalog.aspects.AspectApiReceiverProps
import com.infowings.catalog.aspects.treeview.aspectTreeView
import com.infowings.catalog.common.AspectData
import react.RBuilder
import react.RComponent
import react.RState
import react.setState

class AspectControl(props: AspectApiReceiverProps) :
    RComponent<AspectApiReceiverProps, AspectControl.State>(props) {

    override fun State.init(props: AspectApiReceiverProps) {
        selectedAspect = null
    }

    private fun handleClickAspect(aspect: AspectData) {
        setState {
            selectedAspect = aspect
        }
    }

    private fun closePopup() {
        setState {
            selectedAspect = null
        }
    }

    override fun RBuilder.render() {
        val selectedAspect = state.selectedAspect
        aspectTreeView {
            attrs {
                aspects = if (selectedAspect != null && selectedAspect.id == null)
                    props.data + selectedAspect
                else props.data
                aspectContext = props.aspectContext
                selectedId = when {
                    selectedAspect != null -> selectedAspect.id
                    else -> null
                }
                onAspectClick = ::handleClickAspect
            }
        }
        val aspectId = selectedAspect?.id
        if (aspectId != null) {
            referenceBookApiMiddleware(aspectId, ::closePopup, ReferenceBookControl::class)
        }
    }

    interface State : RState {
        var selectedAspect: AspectData?
    }
}