package com.infowings.catalog.objects.view

import com.infowings.catalog.common.DetailedObjectViewResponse
import com.infowings.catalog.common.ObjectGetResponse
import com.infowings.catalog.objects.getAllObjects
import com.infowings.catalog.objects.getDetailedObject
import kotlinx.coroutines.experimental.launch
import react.*

interface ObjectsViewApiModel {
    fun refresh()
    fun fetchDetailedObject(id: String)
}

interface ObjectsViewApiConsumerProps : RProps {
    var objects: List<ObjectGetResponse>
    var detailedObjectsView: Map<String, DetailedObjectViewResponse>
    var objectApiModel: ObjectsViewApiModel
}

class ObjectsViewApiModelComponent : RComponent<RProps, ObjectsViewApiModelComponent.State>(),
    ObjectsViewApiModel {

    override fun State.init() {
        objects = emptyList()
        detailedObjectsView = emptyMap()
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
            val objectsResponse = getAllObjects()
            setState {
                objects = objectsResponse.objects
            }
        }
    }

    override fun RBuilder.render() {
        objectsViewModel {
            attrs {
                objects = state.objects
                detailedObjectsView = state.detailedObjectsView
                objectApiModel = this@ObjectsViewApiModelComponent
            }
        }
    }

    interface State : RState {
        var objects: List<ObjectGetResponse>
        var detailedObjectsView: Map<String, DetailedObjectViewResponse>
    }
}

val RBuilder.objectViewApiModel
    get() = child(ObjectsViewApiModelComponent::class) {}