package com.infowings.catalog.objects.view

import com.infowings.catalog.common.DetailedObjectViewResponse
import com.infowings.catalog.common.ObjectGetResponse
import com.infowings.catalog.common.SortOrder
import com.infowings.catalog.objects.filter.ObjectsFilter
import com.infowings.catalog.objects.getAllObjects
import com.infowings.catalog.objects.getDetailedObject
import com.infowings.catalog.wrappers.RouteSuppliedProps
import kotlinx.coroutines.experimental.launch
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
    var onOrderByChanged: (List<SortOrder>) -> Unit
}

class ObjectsViewApiModelComponent : RComponent<RouteSuppliedProps, ObjectsViewApiModelComponent.State>(),
    ObjectsViewApiModel {

    override fun State.init() {
        objects = emptyList()
        detailedObjectsView = emptyMap()
        orderBy = emptyList()
    }

    override fun componentDidMount() = fetchAll()

    override fun refresh() = fetchAll()

    override fun fetchDetailedObject(id: String) {
        launch {
            val detailedObjectResponse = getDetailedObject(id)
            setState {
                detailedObjectsView += id to detailedObjectResponse
            }
        }
    }

    private fun fetchAll() {
        launch {
            val objectsResponse = getAllObjects(state.orderBy)
            setState {
                objects = objectsResponse.objects
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
                onOrderByChanged = ::updateSortConfig
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

    interface State : RState {
        var objects: List<ObjectGetResponse>
        var detailedObjectsView: Map<String, DetailedObjectViewResponse>
        var orderBy: List<SortOrder>
    }

}

fun RBuilder.objectViewApiModel(block: RHandler<RouteSuppliedProps>) =
    child(ObjectsViewApiModelComponent::class, block)
