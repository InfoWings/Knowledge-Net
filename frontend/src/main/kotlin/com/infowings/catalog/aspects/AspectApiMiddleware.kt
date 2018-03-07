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
            val response = getAllAspects()
            setState {
                data = response.aspects.toTypedArray()
                context = response.aspects.associate { Pair(it.id!!, it) }.toMutableMap()
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

    private fun handleUpdateAspect(aspectData: AspectData) {
        launch {
            val updatedAspect = updateAspect(aspectData)
            setState {
                data = data.map {
                    if (updatedAspect.id == it.id) updatedAspect else it
                }.toTypedArray()
                context[updatedAspect.id!!] = updatedAspect
            }
        }
    }

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
        /**
         * Last fetched data from server (actual)
         */
        var data: Array<AspectData>
        /**
         * Flag showing if the data is still being fetched
         */
        var loading: Boolean
        /**
         * Map from AspectId to actual AspectData objects. Necessary for reconstructing tree structure
         * (AspectPropertyData contains aspectId)
         */
        var context: MutableMap<String, AspectData>
    }
}