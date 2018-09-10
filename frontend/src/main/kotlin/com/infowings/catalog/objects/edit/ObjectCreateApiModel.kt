package com.infowings.catalog.objects.edit

import com.infowings.catalog.common.objekt.ObjectCreateRequest
import com.infowings.catalog.objects.createObject
import com.infowings.catalog.utils.ApiException
import react.*

interface ObjectCreateApiModel {
    suspend fun submitObject(objectCreateRequest: ObjectCreateRequest)
}

class ObjectCreateApiModelComponent : RComponent<ObjectCreateApiModelComponent.Props, ObjectCreateApiModelComponent.State>(), ObjectCreateApiModel {

    override suspend fun submitObject(objectCreateRequest: ObjectCreateRequest) {
        try {
            val response = createObject(objectCreateRequest)
            props.goToObject(response.id)
        } catch (apiException: ApiException) {
            setState {
                lastApiError = apiException
            }
        }
    }

    override fun RBuilder.render() {
        objectCreateModel {
            attrs {
                api = this@ObjectCreateApiModelComponent
                lastApiError = state.lastApiError
            }
        }
    }

    interface Props : RProps {
        var goToObject: (String) -> Unit
    }

    interface State : RState {
        var lastApiError: ApiException?
    }
}

fun RBuilder.objectCreateApiModel(goToObject: (String) -> Unit) = child(ObjectCreateApiModelComponent::class) {
    attrs {
        this.goToObject = goToObject
    }
}