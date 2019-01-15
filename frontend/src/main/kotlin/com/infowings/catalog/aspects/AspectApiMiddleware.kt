package com.infowings.catalog.aspects

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.SortOrder
import com.infowings.catalog.common.BadRequest
import com.infowings.catalog.common.objekt.Reference
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
    var onAspectPropertyDelete: suspend (propertyId: String, force: Boolean) -> Reference
    var onOrderByChanged: (List<SortOrder>) -> Unit
    var onSearchQueryChanged: (String) -> Unit
    var refreshAspects: () -> Unit
    var refreshOperation: Boolean
}

/**
 * Component that manages already fetched aspects and makes real requests to the server API
 */
class AspectApiMiddleware : RComponent<AspectApiMiddleware.Props, AspectApiMiddleware.State>() {

    override fun State.init() {
        data = emptyList()
        loading = true
        serverError = false
        orderBy = emptyList()
        searchQuery = ""
    }

    override fun componentDidMount() {
        fetchAllAspects()
    }

    private fun fetchAllAspects() {
        launch {
            try {
                val response = getAllAspects()
                setState {
                    data = response.aspects
                    context = response.aspects.associateBy { it.id!! }.toMutableMap()
                    loading = false
                    refreshOperation = false
                }
            } catch (exception: ServerException) {
                setState {
                    data = emptyList()
                    context = mutableMapOf()
                    loading = false
                    serverError = true
                    refreshOperation = false
                }
            }
        }
    }

    private fun fetchAspects(updateContext: Boolean = false, refreshOperation: Boolean = false) {
        launch {
            try {
                val response = getAllAspects(state.orderBy, state.searchQuery)
                setState {
                    data = response.aspects
                    if (updateContext) {
                        val updatedContext = context + response.aspects.associateBy { it.id!! }
                        context = updatedContext.toMutableMap()
                        loading = false
                    }
                    this@setState.refreshOperation = refreshOperation
                }
            } catch (exception: ServerException) {
                setState {
                    data = emptyList()
                    context = mutableMapOf()
                    loading = false
                    serverError = true
                    this@setState.refreshOperation = false
                }
            }
        }
    }

    private fun setAspectsOrderBy(orderBy: List<SortOrder>) {
        setState {
            this.orderBy = orderBy
            refreshOperation = false
        }
        fetchAspects()
    }

    private fun setAspectsSearchQuery(query: String) {
        println("sasq: " + query)
        setState {
            this.searchQuery = query
            refreshOperation = false
        }
        fetchAspects()
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
            refreshOperation = false
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
            refreshOperation = false
        }

        return updatedAspect
    }

    private fun refreshAspect(id: String) {
        launch {
            try {
                val response = getAspectById(id)
                setState {
                    val currentAspect = context[id]
                    val updatedByVersion = currentAspect!!.copy(version = response.version, deleted = response.deleted)
                    data = data.replaceBy({ it.id == response.id }, updatedByVersion)
                    context[id] = updatedByVersion
                    refreshOperation = false
                }
            } catch (exception: ServerException) {
                setState {
                    data = emptyList()
                    context = mutableMapOf()
                    loading = false
                    serverError = true
                    refreshOperation = false
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
            data = data.replaceBy({ deletedAspect.id == it.id }, deletedAspect)
            if (!aspectData.id.isNullOrEmpty()) {
                context[aspectData.id!!] = deletedAspect
            }
            refreshOperation = false
        }

        return deletedAspect.id ?: error("Aspect delete request returned AspectData with id == null")
    }

    private suspend fun handleDeleteAspectProperty(propertyId: String, force: Boolean = false): Reference {
        val aspectPropertyDeleteResponse = try {
            removeAspectProperty(propertyId, force)
        } catch (e: BadRequestException) {
            throw AspectBadRequestException(JSON.parse(e.message))
        }

        setState {
            val parentAspect = context.getValue(aspectPropertyDeleteResponse.parentAspect.id)
            val childAspect = context.getValue(aspectPropertyDeleteResponse.childAspect.id)

            val modifiedParentAspect = parentAspect.copy(
                version = aspectPropertyDeleteResponse.parentAspect.version,
                properties = parentAspect.properties.filterNot { it.id == aspectPropertyDeleteResponse.id }
            )
            val modifiedChildAspect = childAspect.copy(version = aspectPropertyDeleteResponse.childAspect.version)

            data = data.map { aspect ->
                when {
                    aspect.id == aspectPropertyDeleteResponse.parentAspect.id -> modifiedParentAspect
                    aspect.id == aspectPropertyDeleteResponse.childAspect.id -> modifiedChildAspect
                    else -> aspect
                }
            }
            context[aspectPropertyDeleteResponse.parentAspect.id] = modifiedParentAspect
            context[aspectPropertyDeleteResponse.childAspect.id] = modifiedChildAspect
        }

        return aspectPropertyDeleteResponse.parentAspect
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
                    onAspectPropertyDelete = { id, force -> handleDeleteAspectProperty(id, force) }
                    refreshAspect = ::refreshAspect
                    onAspectDelete = { aspect, force -> handleDeleteAspect(aspect, force) }
                    onOrderByChanged = this@AspectApiMiddleware::setAspectsOrderBy
                    onSearchQueryChanged = this@AspectApiMiddleware::setAspectsSearchQuery
                    refreshAspects = { fetchAspects(updateContext = true, refreshOperation = true) }
                    refreshOperation = state.refreshOperation
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
        /**
         * Ordering of returned aspects
         */
        var orderBy: List<SortOrder>
        /**
         * Aspect search query
         */
        var searchQuery: String
        /**
         * Current action is refresh operation.
         */
        var refreshOperation: Boolean
    }
}

fun RBuilder.aspectApiMiddleware(apiReceiverComponent: KClass<out RComponent<AspectApiReceiverProps, *>>) =
    child(AspectApiMiddleware::class) {
        attrs {
            this.apiReceiverComponent = apiReceiverComponent
        }
    }