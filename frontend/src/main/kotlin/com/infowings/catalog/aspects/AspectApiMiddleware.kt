package com.infowings.catalog.aspects

import com.infowings.catalog.common.AspectData
import kotlinx.coroutines.experimental.launch
import react.*

interface AspectApiReceiverProps : RProps {
    var loading: Boolean
    var data: Array<AspectData>
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
                loading = false
            }
        }
    }

    private fun handleCreateNewAspect(aspectData: AspectData) {
        launch {
            val newAspect = createAspect(aspectData)
            setState {
                data += newAspect
            }
        }
    }

    private fun handleUpdateAspect(aspectData: AspectData) {}

    override fun RBuilder.render() {
        child(AspectsTable::class) {
            attrs {
                data = state.data
                loading = state.loading
                onAspectCreate = ::handleCreateNewAspect
                onAspectUpdate = ::handleUpdateAspect
            }
        }
    }

    interface State : RState {
        var data: Array<AspectData>
        var loading: Boolean
    }
}