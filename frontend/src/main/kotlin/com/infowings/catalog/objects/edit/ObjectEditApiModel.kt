package com.infowings.catalog.objects.edit

import com.infowings.catalog.aspects.getAspectTree
import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.*
import com.infowings.catalog.objects.*
import com.infowings.catalog.utils.replaceBy
import kotlinx.coroutines.experimental.launch
import react.*


interface ObjectEditApiModel {
    suspend fun submitObjectProperty(propertyCreateRequest: PropertyCreateRequest)
    suspend fun submitObjectValue(valueCreateRequest: ValueCreateRequest)
    suspend fun editObject(objectUpdateRequest: ObjectUpdateRequest)
    suspend fun editObjectProperty(propertyUpdateRequest: PropertyUpdateRequest)
    suspend fun editObjectValue(propertyId: String, valueUpdateRequest: ValueUpdateRequest)
    suspend fun deleteObject()
    suspend fun deleteObjectProperty(id: String)
    suspend fun deleteObjectValue(propertyId: String, id: String)
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

    override suspend fun editObject(objectUpdateRequest: ObjectUpdateRequest) {
        updateObject(objectUpdateRequest) // TODO: trim everything?
        setState {
            val editedObject = this.editedObject ?: error("Object is not yet loaded")
            this.editedObject = editedObject.copy(
                name = objectUpdateRequest.name,
                description = objectUpdateRequest.description
            )
        }
    }

    override suspend fun deleteObject() {
        val editedObject = state.editedObject ?: error("Object is not yet loaded")
        deleteObject(editedObject.id, false)
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
                    emptyList(),
                    emptyList(),
                    treeAspectResponse
                )

            )
        }
    }

    override suspend fun editObjectProperty(propertyUpdateRequest: PropertyUpdateRequest) {
        val editPropertyResponse = updateProperty(propertyUpdateRequest)
        setState {
            val editedObject = this.editedObject ?: error("Object is not yet loaded")
            this.editedObject = editedObject.copy(
                properties = editedObject.properties.map {
                    if (it.id == editPropertyResponse.id) {
                        it.copy(name = propertyUpdateRequest.name)
                    } else {
                        it
                    }
                }
            )
        }
    }

    override suspend fun deleteObjectProperty(id: String) {
        deleteProperty(id, false)
        setState {
            val editedObject = this.editedObject ?: error("Object is not yet loaded")
            this.editedObject = editedObject.copy(
                properties = editedObject.properties.filterNot { it.id == id }
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

    override suspend fun editObjectValue(propertyId: String, valueUpdateRequest: ValueUpdateRequest) {
        val valueEditResponse = updateValue(valueUpdateRequest)
        setState {
            val editedObject = this.editedObject ?: error("Object is not yet loaded")
            this.editedObject = editedObject.copy(
                properties = editedObject.properties.map { property ->
                    if (property.id == propertyId) {
                        property.editValue(valueEditResponse.id, valueUpdateRequest.value)
                    } else {
                        property
                    }
                }
            )
        }
    }

    override suspend fun deleteObjectValue(propertyId: String, id: String) {
        deleteValue(id, false)
        setState {
            val editedObject = this.editedObject ?: error("Object is not yet loaded")
            this.editedObject = editedObject.copy(
                properties = editedObject.properties.map { property ->
                    if (property.id == propertyId) {
                        property.deleteValue(id)
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
            propertyId = request.aspectPropertyId,
            childrenIds = emptyList()
        )
        return if (request.aspectPropertyId == null) {
            this.copy(
                rootValues = this.rootValues + valueDescriptor,
                valueDescriptors = this.valueDescriptors + valueDescriptor
            )
        } else {
            val parentValueId = request.parentValueId ?: error("Parent value should be null when aspectPropertyId is not null")
            val parentDescriptor = valueDescriptors.find { it.id == parentValueId } ?: error("Value descriptors should contain parent value")
            val newParentDescriptor = parentDescriptor.copy(childrenIds = parentDescriptor.childrenIds + response.id)

            when {
                newParentDescriptor.propertyId == null -> {
                    this.copy(
                        rootValues = this.rootValues.replaceBy(newParentDescriptor) { it.id == newParentDescriptor.id },
                        valueDescriptors = this.valueDescriptors.replaceBy(newParentDescriptor) { it.id == newParentDescriptor.id } + valueDescriptor
                    )
                }
                else -> {
                    this.copy(
                        valueDescriptors = this.valueDescriptors.replaceBy(newParentDescriptor) { it.id == newParentDescriptor.id } + valueDescriptor
                    )
                }
            }
        }
    }

    private fun ObjectPropertyEditDetailsResponse.editValue(valueId: String, value: ObjectValueData): ObjectPropertyEditDetailsResponse {
        return this.copy(
            rootValues = this.rootValues.map {
                if (it.id == valueId) {
                    it.copy(value = value.toDTO())
                } else {
                    it
                }
            },
            valueDescriptors = this.valueDescriptors.map {
                if (it.id == valueId) {
                    it.copy(value = value.toDTO())
                } else {
                    it
                }
            }
        )
    }

    private fun ObjectPropertyEditDetailsResponse.deleteValue(valueId: String): ObjectPropertyEditDetailsResponse {
        val hasParentValue = this.rootValues.find { it.id == valueId } == null

        return if (hasParentValue) {
            val parentValue = this.valueDescriptors.find { it.childrenIds.contains(valueId) } ?: error("Parent value should exist")
            val changedParentValue = parentValue.copy(childrenIds = parentValue.childrenIds.filterNot { it == valueId })

            this.copy(
                rootValues = this.rootValues.replaceBy(changedParentValue) { it.id == changedParentValue.id },
                valueDescriptors = this.valueDescriptors.replaceBy(changedParentValue) { it.id == changedParentValue.id }.filterNot { it.id == valueId }
            )
        } else {
            this.copy(
                rootValues = this.rootValues.filterNot { it.id == valueId },
                valueDescriptors = this.valueDescriptors.filterNot { it.id == valueId }
            )
        }
    }

    override fun RBuilder.render() {
        state.editedObject?.let {
            objectTreeEditModel {
                attrs {
                    apiModel = this@ObjectEditApiModelComponent
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
