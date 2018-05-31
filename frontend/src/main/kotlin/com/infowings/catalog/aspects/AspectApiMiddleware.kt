package com.infowings.catalog.aspects

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectOrderBy
import com.infowings.catalog.common.BadRequest
import com.infowings.catalog.utils.BadRequestException
import com.infowings.catalog.utils.NotModifiedException
import com.infowings.catalog.utils.ServerException
import com.infowings.catalog.utils.replaceBy
import com.infowings.catalog.wrappers.blueprint.Button
import com.infowings.catalog.wrappers.blueprint.NonIdealState
import com.infowings.catalog.wrappers.react.asReactElement
import kotlinx.coroutines.experimental.launch
import kotlinx.serialization.json.JSON
import react.*
import kotlin.reflect.KClass

class AspectBadRequestException(val exceptionInfo: BadRequest) : RuntimeException(exceptionInfo.message)

interface AspectApiReceiverProps : RProps {
    var loading: Boolean
    var data: List<AspectData>
    var aspectContext: Map<String, AspectData>
    var refreshAspect: (id: String) -> Unit
    var onAspectUpdate: suspend (changedAspect: AspectData) -> AspectData
    var onAspectCreate: suspend (newAspect: AspectData) -> AspectData
    var onAspectDelete: suspend (aspect: AspectData, force: Boolean) -> String
    var onFetchAspects: (List<AspectOrderBy>) -> Unit
}

/**
 * Component that manages already fetched aspects and makes real requests to the server API
 */
class AspectApiMiddleware : RComponent<AspectApiMiddleware.Props, AspectApiMiddleware.State>() {

    override fun State.init() {
        data = emptyList()
        loading = true
        serverError = false
    }

    override fun componentDidMount() {
        fetchAspects()
    }

    private fun fetchAspects(orderBy: List<AspectOrderBy> = emptyList()) {
        launch {
            try {
                val response = getAllAspects(orderBy)
                setState {
                    data = response.aspects
                    context = response.aspects.associate { Pair(it.id!!, it) }.toMutableMap()
                    loading = false
                }
            } catch (exception: ServerException) {
                setState {
                    data = emptyList()
                    context = mutableMapOf()
                    loading = false
                    serverError = true
                }
            }
        }
    }

    private suspend fun handleCreateNewAspect(aspectData: AspectData): AspectData {
        val newAspect: AspectData
        try {
            newAspect = createAspect(aspectData)
        } catch (e: BadRequestException) {
            throw AspectBadRequestException(JSON.parse(e.message))
        }

        val newAspectId: String = newAspect.id ?: error("Server returned Aspect with aspectId == null")

        setState {
            data += newAspect
            context[newAspectId] = newAspect
        }

        return newAspect
    }

    private suspend fun handleUpdateAspect(aspectData: AspectData): AspectData {
        val updatedAspect: AspectData

        updatedAspect = try {
            updateAspect(aspectData)
        } catch (e: BadRequestException) {
            throw AspectBadRequestException(JSON.parse(e.message))
        } catch (e: NotModifiedException) {
            console.log("Aspect updating rejected because data is the same")
            aspectData
        }

        val updatedAspectId: String = updatedAspect.id ?: error("Server returned Aspect with aspectId == null")

        setState {
            data = data.map {
                if (updatedAspect.id == it.id) updatedAspect else it
            }
            context[updatedAspectId] = updatedAspect
        }

        return updatedAspect
    }

    private fun refreshAspect(id: String) {
        launch {
            try {
                val response = getAspectById(id)
                setState {
                    data = data.replaceBy(response) { it.id == response.id }
                    context[response.id ?: error("Server returned Aspect with aspectId == null")] = response
                }
            } catch (exception: ServerException) {
                setState {
                    data = emptyList()
                    context = mutableMapOf()
                    loading = false
                    serverError = true
                }
            }
        }
    }

    private suspend fun handleDeleteAspect(aspectData: AspectData, force: Boolean): String {

        try {
            if (force) {
                forceRemoveAspect(aspectData)
            } else {
                removeAspect(aspectData)
            }
        } catch (e: BadRequestException) {
            throw AspectBadRequestException(JSON.parse(e.message))
        }

        val deletedAspect: AspectData = aspectData.copy(deleted = true)

        setState {
            data = data.replaceBy(deletedAspect) { deletedAspect.id == it.id }
            if (!aspectData.id.isNullOrEmpty()) {
                context[aspectData.id!!] = deletedAspect
            }
        }

        return deletedAspect.id ?: error("Aspect delete request returned AspectData with id == null")
    }

    override fun RBuilder.render() {
        if (!state.serverError) {
            child(props.apiReceiverComponent) {
                attrs {
                    data = state.data
                    aspectContext = state.context
                    loading = state.loading
                    onAspectCreate = { handleCreateNewAspect(it) }
                    onAspectUpdate = { handleUpdateAspect(it) }
                    refreshAspect = ::refreshAspect
                    onAspectDelete = { aspect, force -> handleDeleteAspect(aspect, force) }
                    onFetchAspects = ::fetchAspects
                }
            }
        } else {
            NonIdealState {
                attrs {
                    visual = "error"
                    title = "Oops, something went wrong".asReactElement()
                    action = buildElement {
                        Button {
                            attrs {
                                icon = "refresh"
                                onClick = {
                                    setState {
                                        serverError = false
                                        loading = true
                                    }
                                    fetchAspects()
                                }
                            }
                            +"Try again"
                        }
                    }!!
                }

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
        /**
         * Server error happened
         */
        var serverError: Boolean
    }
}

fun RBuilder.aspectApiMiddleware(apiReceiverComponent: KClass<out RComponent<AspectApiReceiverProps, *>>) =
    child(AspectApiMiddleware::class) {
        attrs {
            this.apiReceiverComponent = apiReceiverComponent
        }
    }