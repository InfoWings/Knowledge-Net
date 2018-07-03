package com.infowings.catalog.objects.edit

import com.infowings.catalog.aspects.getAspectTree
import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.PropertyCreateRequest
import com.infowings.catalog.common.objekt.ValueCreateRequest
import com.infowings.catalog.common.objekt.ValueCreateResponse
import com.infowings.catalog.objects.createProperty
import com.infowings.catalog.objects.createValue
import com.infowings.catalog.objects.getDetailedObjectForEdit
import kotlinx.coroutines.experimental.launch
import react.*


interface ObjectEditApiModel {
    suspend fun submitObjectProperty(propertyCreateRequest: PropertyCreateRequest)
    suspend fun submitObjectValue(valueCreateRequest: ValueCreateRequest)
}

class ObjectEditApiModelComponent : RComponent<ObjectEditApiModelComponent.Props, ObjectEditApiModelComponent.State>(),
    ObjectEditApiModel {

    override fun componentDidMount() {
        launch {
            val detailedObjectResponse = getDetailedObjectForEdit(props.objectId)
            setState {
                editedObject = detailedObjectResponse
            }
        }
    }

    override suspend fun submitObjectProperty(propertyCreateRequest: PropertyCreateRequest) {
        val createPropertyResponse = createProperty(propertyCreateRequest)
        val treeAspectResponse = getAspectTree(propertyCreateRequest.aspectId)
        setState {
            val editedObject = this.editedObject ?: error("Object is not yet loaded")
            this.editedObject = editedObject.copy(
                properties = editedObject.properties + ObjectPropertyEditDetailsResponse(
                    createPropertyResponse.id,
                    propertyCreateRequest.name,
                    propertyCreateRequest.description,
                    PropertyCardinality.valueOf(propertyCreateRequest.cardinality),
                    emptyList(),
                    emptyList(),
                    treeAspectResponse
                )

            )
        }
    }

    override suspend fun submitObjectValue(valueCreateRequest: ValueCreateRequest) {
        val valueCreateResponse = createValue(valueCreateRequest)
        setState {
            val editedObject = this.editedObject ?: error("Object is not yet loaded")
            this.editedObject = editedObject.copy(
                properties = editedObject.properties.map { property ->
                    if (property.id == valueCreateRequest.objectPropertyId) {
                        property.addValue(valueCreateRequest, valueCreateResponse)
                    } else {
                        property
                    }
                }
            )
        }
    }

    private fun ObjectPropertyEditDetailsResponse.addValue(request: ValueCreateRequest, response: ValueCreateResponse): ObjectPropertyEditDetailsResponse {
        val valueDescriptor = ValueTruncated(
            id = response.id,
            value = request.value.toDTO(),
            propertyId = request.aspectPropertyId ?: request.objectPropertyId,
            childrenIds = emptyList()
        )
        return if (request.aspectPropertyId == null) {
            this.copy(
                rootValues = this.rootValues + valueDescriptor,
                valueDescriptors = this.valueDescriptors + valueDescriptor
            )
        } else {
            this.copy(
                valueDescriptors = this.valueDescriptors + valueDescriptor
            )
        }
    }

    override fun RBuilder.render() {
        state.editedObject?.let {
            objectTreeEditModel {
                attrs {
                    serverView = it
                }
            }
        }
    }

    interface State : RState {
        var editedObject: ObjectEditDetailsResponse?
    }

    interface Props : RProps {
        var objectId: String
    }
}

fun RBuilder.objectEditApiModel(objectId: String) = child(ObjectEditApiModelComponent::class) {
    attrs {
        this.objectId = objectId
    }
}
