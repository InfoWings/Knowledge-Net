package com.infowings.catalog.objects.edit

import com.infowings.catalog.common.objekt.ObjectCreateRequest
import com.infowings.catalog.objects.createObject
import com.infowings.catalog.utils.ApiException
import com.infowings.catalog.utils.JobCoroutineScope
import com.infowings.catalog.utils.JobSimpleCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import react.*

interface ObjectCreateApiModel {
    fun submitObject(objectCreateRequest: ObjectCreateRequest)
}

class ObjectCreateApiModelComponent :
    RComponent<ObjectCreateApiModelComponent.Props, ObjectCreateApiModelComponent.State>(),
    ObjectCreateApiModel,
    JobCoroutineScope by JobSimpleCoroutineScope() {

    override fun componentWillMount() {
        job = Job()
    }

    override fun componentWillUnmount() {
        job.cancel()
    }

    override fun submitObject(objectCreateRequest: ObjectCreateRequest) {
        launch {
            try {
                val response = createObject(objectCreateRequest)
                withContext(Dispatchers.Main) {
                    props.goToObject(response.id)
                }
            } catch (apiException: ApiException) {
                setState {
                    lastApiError = apiException
                }
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