package com.infowings.catalog.aspects

import com.infowings.catalog.common.AspectBadRequest
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.utils.ServerException
import kotlinx.coroutines.experimental.launch
import kotlinx.serialization.json.JSON
import react.*
import kotlin.reflect.KClass

class AspectBadRequestException(val exceptionInfo: AspectBadRequest) : RuntimeException(exceptionInfo.message)

interface AspectApiReceiverProps : RProps {
    var loading: Boolean
    var data: List<AspectData>
    var aspectContext: Map<String, AspectData>
    var onAspectUpdate: suspend (changedAspect: AspectData) -> Unit
    var onAspectCreate: suspend (newAspect: AspectData) -> Unit
    var onAspectDelete: (aspect: AspectData, force: Boolean) -> Unit
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

    private suspend fun handleCreateNewAspect(aspectData: AspectData) {
        val newAspect: AspectData

        try {
            newAspect = createAspect(aspectData)
        } catch (ex: ServerException) {
            if (ex.httpStatusCode == 400) {
                throw AspectBadRequestException(JSON.parse(ex.message!!))
            }
            console.log("Server Exception: status = ${ex.httpStatusCode}, message = ${ex.message}")
            return
        }

        val newAspectId: String = newAspect.id ?: throw Error("Server returned Aspect with aspectId == null")

        setState {
            data += newAspect
            context[newAspectId] = newAspect
        }
    }

    private suspend fun handleUpdateAspect(aspectData: AspectData) {
        val updatedAspect: AspectData

        try {
            updatedAspect = updateAspect(aspectData)
        } catch (ex: ServerException) {
            if (ex.httpStatusCode == 400) {
                throw AspectBadRequestException(JSON.parse(ex.message!!))
            }
            console.log("Server Exception: status = ${ex.httpStatusCode}, message = ${ex.message}")
            return
        }

        val updatedAspectId: String = updatedAspect.id
                ?: throw Error("Server returned Aspect with aspectId == null")

        setState {
            data = data.map {
                if (updatedAspect.id == it.id) updatedAspect else it
            }
            context[updatedAspectId] = updatedAspect
        }
    }

//    private suspend fun handleDeleteAspect(aspectData: AspectData, force: Boolean) {
//        try {
//            if (force) {
//                forceRemoveAspect(aspectData)
//            } else {
//                removeAspect(aspectData)
//            }
//        } catch (e: Exception) {
//
//        }
//
//    }

    override fun RBuilder.render() {
        child(props.apiReceiverComponent) {
            attrs {
                data = state.data
                aspectContext = state.context
                loading = state.loading
                onAspectCreate = { handleCreateNewAspect(it) }
                onAspectUpdate = { handleUpdateAspect(it) }
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

fun RBuilder.aspectApiMiddleware(apiReceiverComponent: KClass<out RComponent<AspectApiReceiverProps, *>>) =
    child(AspectApiMiddleware::class) {

        attrs {
            this.apiReceiverComponent = apiReceiverComponent
        }
    }