package com.infowings.catalog.objects.view

import com.infowings.catalog.common.*
import com.infowings.catalog.objects.filter.ObjectsFilter
import com.infowings.catalog.objects.getAllObjects
import com.infowings.catalog.objects.getDetailedObject
import com.infowings.catalog.utils.JobCoroutineScope
import com.infowings.catalog.utils.JobSimpleCoroutineScope
import com.infowings.catalog.utils.ServerException
import com.infowings.catalog.wrappers.RouteSuppliedProps
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import react.*


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
    var onFilterChanged: (ObjectsFilter) -> Unit
    var onPage: (Int) -> Unit
    var paginationData: PaginationData
    var refreshObjects: () -> Unit
    var objectsFilter: ObjectsFilter
}

class ObjectsViewApiModelComponent :
    RComponent<RouteSuppliedProps, ObjectsViewApiModelComponent.State>(),
    ObjectsViewApiModel,
    JobCoroutineScope by JobSimpleCoroutineScope() {

    override fun State.init() {
        objects = emptyList()
        detailedObjectsView = emptyMap()
        orderBy = emptyList()
        paginationData = PaginationData.emptyPage
        objectsFilter = ObjectsFilter(emptyList(), emptyList())
    }

    private fun State.toObjectRequestData() =
        ObjectsRequestData(
            sortOrder = orderBy,
            query = searchQuery,
            excludedFromSubjectFilter = objectsFilter.excluded.mapNotNull { it.guid },
            pagination = paginationData,
            subjectsGuids = objectsFilter.subjects.mapNotNull { it?.guid }
        )

    override fun componentDidMount() {
        job = Job()
        fetch()
    }

    override fun componentWillUnmount() {
        job.cancel()
    }

    override fun refresh() {
        launch {
            try {
                val response = getAllObjects(state.toObjectRequestData())
                val detailsNeeded = state.objects.filter { it.id in state.detailedObjectsView.keys }
                val freshDetails = detailsNeeded.map { it.id to getDetailedObject(it.id) }.toMap()

                setState {
                    objects = response.objects
                    detailedObjectsView += freshDetails
                    paginationData = paginationData.copy(totalItems = response.totalObjects)
                }
            } catch (exception: ServerException) {
                println("something wrong: $exception")
            }

        }

    }


    override fun fetchDetailedObject(id: String) {
        launch {
            val detailedObjectResponse = getDetailedObject(id)
            setState {
                detailedObjectsView += id to detailedObjectResponse
            }
        }
    }

    private fun fetch() {
        job.cancelChildren()
        launch {
            //val objectsResponse = getAllObjects(state.orderBy, state.searchQuery)
            val objectsResponse = getAllObjects(state.toObjectRequestData())
            objectsReceived(objectsResponse, state.paginationData)
        }
    }

    private fun objectsReceived(objectsResponse: ObjectsResponse, viewSlice: PaginationData) {
        setState {
            objects = objectsResponse.objects
            this.paginationData = this.paginationData.updateTotal(objectsResponse.totalObjects)
        }
    }

    private fun fetch(viewSlice: PaginationData) {
        job.cancelChildren()
        launch {
            setState { paginationData = viewSlice }
            val objectsResponse = getAllObjects(state.toObjectRequestData())
            objectsReceived(objectsResponse, viewSlice)
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
                onPage = { fetch(state.paginationData.copy(current = it)) }
                paginationData = state.paginationData
                onSearchQueryChanged = ::updateSearchQuery
                onFilterChanged = ::updateSearchFilter
                refreshObjects = ::refresh
                objectsFilter = state.objectsFilter
            }
        }

    }

    private fun updateSearchFilter(objectsFilter: ObjectsFilter) {
        setState {
            this.objectsFilter = objectsFilter
            paginationData = paginationData.copy(current = 1)
        }
        refresh()
    }

    private fun updateSortConfig(newOrderBy: List<SortOrder>) {
        setState {
            orderBy = newOrderBy
        }
        refresh()
    }

    private fun updateSearchQuery(query: String) {
        setState {
            searchQuery = query
            paginationData = paginationData.copy(current = 1)
        }
        refresh()
    }

    interface State : RState {
        var objects: List<ObjectGetResponse>
        var detailedObjectsView: Map<String, DetailedObjectViewResponse>
        var orderBy: List<SortOrder>
        var searchQuery: String?
        var paginationData: PaginationData
        var objectsFilter: ObjectsFilter
    }

}

fun RBuilder.objectViewApiModel(block: RHandler<RouteSuppliedProps>) =
    child(ObjectsViewApiModelComponent::class, block)
