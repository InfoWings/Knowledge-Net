package com.infowings.catalog.aspects

import com.infowings.catalog.common.AspectData
import kotlinx.coroutines.experimental.launch
import react.*

interface AspectApiReceiverProps : RProps {
    var loading: Boolean
    var data: Array<AspectData>
    var aspectContext: Map<String, AspectData>
    var onAspectUpdate: (changedAspect: AspectData) -> Unit
    var onAspectCreate: (newAspect: AspectData) -> Unit
}

/**
 * Component that manages already fetched aspects and makes real requests to the server API
 */
class AspectApiMiddleware : RComponent<RProps, AspectApiMiddleware.State>() {

    override fun State.init() {
        data = emptyArray()
        loading = true
    }

    override fun componentDidMount() {
        launch {
            val aspects = getAllAspects()
            setState {
                data = aspects.aspects.toTypedArray()
                context = aspects.aspects.associate { Pair(it.id!!, it) }.toMutableMap()
                loading = false
            }
        }
    }

    private fun handleCreateNewAspect(aspectData: AspectData) {
        launch {
            val newAspect = createAspect(aspectData)
            setState {
                data += newAspect
                context[newAspect.id!!] = newAspect
            }
        }
    }

    private fun handleUpdateAspect(aspectData: AspectData) {}

    override fun RBuilder.render() {
        child(AspectsTable::class) {
            attrs {
                data = state.data
                aspectContext = state.context
                loading = state.loading
                onAspectCreate = ::handleCreateNewAspect
                onAspectUpdate = ::handleUpdateAspect
            }
        }
    }

    interface State : RState {
        var data: Array<AspectData>
        var loading: Boolean
        var context: MutableMap<String, AspectData>
    }
}