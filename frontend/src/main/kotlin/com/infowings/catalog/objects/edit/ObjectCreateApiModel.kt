package com.infowings.catalog.objects.edit

import com.infowings.catalog.common.objekt.ObjectCreateRequest
import com.infowings.catalog.objects.createObject
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState

interface ObjectCreateApiModel {
    suspend fun submitObject(objectCreateRequest: ObjectCreateRequest)
}

class ObjectCreateApiModelComponent : RComponent<ObjectCreateApiModelComponent.Props, RState>(), ObjectCreateApiModel {

    override suspend fun submitObject(objectCreateRequest: ObjectCreateRequest) {
        val response = createObject(objectCreateRequest)
        props.goToObject(response.id)
    }

    override fun RBuilder.render() {
        objectCreateModel {
            attrs {
                api = this@ObjectCreateApiModelComponent
            }
        }
    }

    interface Props : RProps {
        var goToObject: (String) -> Unit
    }
}

fun RBuilder.objectCreateApiModel(goToObject: (String) -> Unit) = child(ObjectCreateApiModelComponent::class) {
    attrs {
        this.goToObject = goToObject
    }
}