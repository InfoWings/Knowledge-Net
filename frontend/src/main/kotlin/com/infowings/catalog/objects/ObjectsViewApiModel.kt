package com.infowings.catalog.objects

import com.infowings.catalog.common.ObjectGetResponse
import kotlinx.coroutines.experimental.launch
import react.*

interface ObjectsViewApiModel {
    fun refresh()
    fun fetchDetailedObject(id: String)
}

interface ObjectsViewApiConsumerProps : RProps {
    var objects: List<ObjectGetResponse>
    var objectApiModel: ObjectsViewApiModel
}

class ObjectsViewApiModelComponent : RComponent<RProps, ObjectsViewApiModelComponent.State>(), ObjectsViewApiModel {

    override fun State.init() {
        objects = emptyList()
    }

    override fun componentDidMount() = fetchAll()

    override fun refresh() = fetchAll()

    override fun fetchDetailedObject(id: String) {
        launch {
            val detailedObjectResponse = getDetailedObject(id)
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
                objectApiModel = this@ObjectsViewApiModelComponent
            }
        }
    }

    interface State : RState {
        var objects: List<ObjectGetResponse>
    }
}

val RBuilder.objectViewApiModel
    get() = child(ObjectsViewApiModelComponent::class) {}