package com.infowings.catalog.aspects

import com.infowings.catalog.common.AspectData
import kotlinx.coroutines.experimental.launch
import react.*
import kotlin.reflect.KClass

//private val aspects: List<AspectData> = arrayListOf(
//        AspectData("#1:0", "Width", "Metre", "OpenDomain", "Decimal"),
//        AspectData("#2:0", "Height", "Metre", "OpenDomain", "Decimal"),
//        AspectData("#3:0", "Depth", "Metre", "OpenDomain", "Decimal"),
//        AspectData("#4:0", "Dimensions", null, "OpenDomain", "Complex Type", arrayListOf(
//                AspectPropertyData("#5:0", "Width", "#1:0", "ONE"),
//                AspectPropertyData("#6:0", "Height", "#2:0", "ONE"),
//                AspectPropertyData("#7:0", "Depth", "#3:0", "ONE")
//        ))
//)

interface AspectApiReceiverProps : RProps {
    var loading: Boolean
    var data: List<AspectData>
    var aspectContext: Map<String, AspectData>
    var onAspectUpdate: (changedAspect: AspectData) -> Unit
    var onAspectCreate: (newAspect: AspectData) -> Unit
}

/**
 * Component that manages already fetched aspects and makes real requests to the server API
 */
class AspectApiMiddleware : RComponent<AspectApiMiddleware.Props, AspectApiMiddleware.State>() {

    override fun State.init() {
        data = emptyList()
        loading = true
    }

    override fun componentDidMount() {
        launch {
            val response = getAllAspects()
            setState {
                data = response.aspects
                context = response.aspects.associate { Pair(it.id!!, it) }.toMutableMap()
                loading = false
            }
        }
    }

    private fun handleCreateNewAspect(aspectData: AspectData) {
        launch {
            val newAspect = createAspect(aspectData)
            val newAspectId: String = newAspect.id ?: throw Error("Server returned Aspect with aspectId == null")

            setState {
                data += newAspect
                context[newAspectId] = newAspect
            }
        }
    }

    private fun handleUpdateAspect(aspectData: AspectData) {
        launch {
            val updatedAspect = updateAspect(aspectData)
            val updatedAspectId: String = updatedAspect.id
                    ?: throw Error("Server returned Aspect with aspectId == null")

            setState {
                data = data.map {
                    if (updatedAspect.id == it.id) updatedAspect else it
                }
                context[updatedAspectId] = updatedAspect
            }
        }
    }

    override fun RBuilder.render() {
        child(props.apiReceiverComponent) {
            attrs {
                data = /*aspects*/ state.data
                aspectContext = /*aspects.associateBy { it.id!! }*/ state.context
                loading = state.loading
                onAspectCreate = ::handleCreateNewAspect
                onAspectUpdate = ::handleUpdateAspect
            }
        }
    }

    interface Props : RProps {
        var apiReceiverComponent: KClass<out RComponent<AspectApiReceiverProps, *>>
    }

    interface State : RState {
        /**
         * Last fetched data from server (actual)
         */
        var data: List<AspectData>
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

fun RBuilder.aspectApiMiddleware(apiReceiverComponent: KClass<out RComponent<AspectApiReceiverProps, *>>) = child(AspectApiMiddleware::class) {

    attrs {
        this.apiReceiverComponent = apiReceiverComponent
    }
}