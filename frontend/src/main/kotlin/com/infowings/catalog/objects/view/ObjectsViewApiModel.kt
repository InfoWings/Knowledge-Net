package com.infowings.catalog.objects.view

import com.infowings.catalog.common.DetailedObjectViewResponse
import com.infowings.catalog.common.ObjectGetResponse
import com.infowings.catalog.common.SortOrder
import com.infowings.catalog.common.ViewSlice
import com.infowings.catalog.objects.getAllObjects
import com.infowings.catalog.objects.getDetailedObject
import com.infowings.catalog.utils.JobCoroutineScope
import com.infowings.catalog.utils.JobSimpleCoroutineScope
import com.infowings.catalog.wrappers.RouteSuppliedProps
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import react.*

private const val PageSize = 20

interface ObjectsViewApiModel {
    fun refresh()
    fun fetchDetailedObject(id: String)
}

interface ObjectsViewApiConsumerProps : RouteSuppliedProps {
    var objects: List<ObjectGetResponse>
    var detailedObjectsView: Map<String, DetailedObjectViewResponse>
    var objectApiModel: ObjectsViewApiModel
    var orderBy: List<SortOrder>
    var query: String?
    var onOrderByChanged: (List<SortOrder>) -> Unit
    var onSearchQueryChanged: (String) -> Unit
    var onPrevPage: () -> Unit
    var onNextPage: () -> Unit
    var viewSlice: ViewSlice
}

class ObjectsViewApiModelComponent :
    RComponent<RouteSuppliedProps, ObjectsViewApiModelComponent.State>(),
    ObjectsViewApiModel,
    JobCoroutineScope by JobSimpleCoroutineScope() {

    override fun State.init() {
        objects = emptyList()
        detailedObjectsView = emptyMap()
        orderBy = emptyList()
        viewSlice = ViewSlice(offset = 0, limit = PageSize)
    }

    override fun componentDidMount() {
        job = Job()
        fetch()
    }

    override fun componentWillUnmount() {
        job.cancel()
    }

    override fun refresh() = fetch()

    override fun fetchDetailedObject(id: String) {
        launch {
            val detailedObjectResponse = getDetailedObject(id)
            setState {
                detailedObjectsView += id to detailedObjectResponse
            }
        }
    }

    private fun fetch() {
        launch {
            //val objectsResponse = getAllObjects(state.orderBy, state.searchQuery)
            val objectsResponse = getAllObjects(state.orderBy, state.searchQuery, offset = state.viewSlice.offset, limit = state.viewSlice.limit)
            setState {
                objects = objectsResponse.objects
            }
        }
    }

    private fun fetch(viewSlice: ViewSlice) {
        launch {
            val objectsResponse = getAllObjects(state.orderBy, state.searchQuery, offset = viewSlice.offset, limit = viewSlice.limit)
            setState {
                objects = objectsResponse.objects
                this.viewSlice = viewSlice
            }
        }
    }

    override fun RBuilder.render() {
        objectsViewModel {
            val currHistory = props.history
            attrs {
                objects = state.objects
                detailedObjectsView = state.detailedObjectsView
                objectApiModel = this@ObjectsViewApiModelComponent
                history = currHistory
                orderBy = state.orderBy
                query = state.searchQuery
                onOrderByChanged = ::updateSortConfig
                onPrevPage = {
                    fetch(state.viewSlice.prev())
                }
                onNextPage = {
                    fetch(state.viewSlice.next())
                }
                viewSlice = state.viewSlice
                onSearchQueryChanged = ::updateSearchQuery
            }
        }

    }

    private fun updateSortConfig(newOrderBy: List<SortOrder>) {
        println("update sort config: $newOrderBy")
        setState {
            orderBy = newOrderBy
        }
        //refresh()
    }

    private fun updateSearchQuery(query: String) {
        setState {
            searchQuery = query
            viewSlice = ViewSlice(0, PageSize)
        }
        //refresh()
    }

    interface State : RState {
        var objects: List<ObjectGetResponse>
        var detailedObjectsView: Map<String, DetailedObjectViewResponse>
        var orderBy: List<SortOrder>
        var searchQuery: String?
        var viewSlice: ViewSlice
    }

}

fun RBuilder.objectViewApiModel(block: RHandler<RouteSuppliedProps>) =
    child(ObjectsViewApiModelComponent::class, block)
