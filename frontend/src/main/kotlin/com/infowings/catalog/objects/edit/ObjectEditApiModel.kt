package com.infowings.catalog.objects.edit

import com.infowings.catalog.aspects.getAspectTree
import com.infowings.catalog.common.ObjectEditDetailsResponse
import com.infowings.catalog.common.ObjectPropertyEditDetailsResponse
import com.infowings.catalog.common.ValueTruncated
import com.infowings.catalog.common.objekt.*
import com.infowings.catalog.objects.*
import com.infowings.catalog.utils.ServerException
import com.infowings.catalog.utils.mapOn
import com.infowings.catalog.utils.replaceBy
import com.infowings.catalog.wrappers.reactRouter
import kotlinx.coroutines.experimental.launch
import react.*


interface ObjectEditApiModel {
    suspend fun submitObjectProperty(propertyCreateRequest: PropertyCreateRequest)
    suspend fun submitObjectValue(valueCreateRequest: ValueCreateRequest)
    suspend fun editObject(objectUpdateRequest: ObjectUpdateRequest, subjectName: String)
    suspend fun editObjectProperty(propertyUpdateRequest: PropertyUpdateRequest)
    suspend fun editObjectValue(valueUpdateRequest: ValueUpdateRequest)
    suspend fun deleteObject(force: Boolean = false)
    suspend fun deleteObjectProperty(id: String, force: Boolean = false)
    suspend fun deleteObjectValue(propertyId: String, id: String, force: Boolean = false)
}

class ObjectEditApiModelComponent : RComponent<ObjectEditApiModelComponent.Props, ObjectEditApiModelComponent.State>(),
    ObjectEditApiModel {

    override fun State.init() {
        editedObject = null
        deleted = false
    }

    override fun componentDidMount() {
        launch {
            val detailedObjectResponse = getDetailedObjectForEdit(props.objectId)
            setState {
                editedObject = detailedObjectResponse
            }
        }
    }

    override suspend fun editObject(objectUpdateRequest: ObjectUpdateRequest, subjectName: String) {
        try {
            val updateResponse = updateObject(objectUpdateRequest)
            setState {
                val editedObject = this.editedObject ?: error("Object is not yet loaded")
                this.editedObject = editedObject.copy(
                    name = updateResponse.name,
                    subjectId = updateResponse.subjectId,
                    subjectName = updateResponse.subjectName,
                    description = updateResponse.description,
                    version = updateResponse.version
                )
                lastApiError = null
            }
        } catch (exception: ServerException) {
            setState {
                lastApiError = exception.message
            }
        }
    }

    override suspend fun deleteObject(force: Boolean) {
        try {
            val editedObject = state.editedObject ?: error("Object is not yet loaded")
            deleteObject(editedObject.id, force)
            setState {
                this.editedObject = null
                this.deleted = true
                lastApiError = null
            }
        } catch (exception: ServerException) {
            setState {
                lastApiError = exception.message
            }
        }
    }

    override suspend fun submitObjectProperty(propertyCreateRequest: PropertyCreateRequest) {
        try {
            val createPropertyResponse = createProperty(propertyCreateRequest)
            val treeAspectResponse = getAspectTree(propertyCreateRequest.aspectId)
            setState {
                val editedObject = this.editedObject ?: error("Object is not yet loaded")
                this.editedObject = editedObject.copy(
                    properties = editedObject.properties + ObjectPropertyEditDetailsResponse(
                        createPropertyResponse.id,
                        createPropertyResponse.name,
                        createPropertyResponse.description,
                        createPropertyResponse.version,
                        emptyList(),
                        emptyList(),
                        treeAspectResponse
                    )
                )
                lastApiError = null
            }
        } catch (exception: ServerException) {
            setState {
                lastApiError = exception.message
            }
        }
    }

    override suspend fun editObjectProperty(propertyUpdateRequest: PropertyUpdateRequest) {
        try {
            val editPropertyResponse = updateProperty(propertyUpdateRequest)
            setState {
                val editedObject = this.editedObject ?: error("Object is not yet loaded")
                this.editedObject = editedObject.copy(
                    properties = editedObject.properties.mapOn(
                        { it.id == editPropertyResponse.id },
                        { it.copy(name = editPropertyResponse.name, description = editPropertyResponse.description, version = editPropertyResponse.version) }
                    )
                )
                lastApiError = null
            }
        } catch (exception: ServerException) {
            setState {
                lastApiError = exception.message
            }
        }
    }

    override suspend fun deleteObjectProperty(id: String, force: Boolean) {
        try {
            deleteProperty(id, force)
            setState {
                val editedObject = this.editedObject ?: error("Object is not yet loaded")
                this.editedObject = editedObject.copy(
                    properties = editedObject.properties.filterNot { it.id == id }
                )
                lastApiError = null
            }
        } catch (exception: ServerException) {
            setState {
                lastApiError = exception.message
            }
        }
    }

    override suspend fun submitObjectValue(valueCreateRequest: ValueCreateRequest) {
        try {
            val valueCreateResponse = createValue(valueCreateRequest)
            setState {
                val editedObject = this.editedObject ?: error("Object is not yet loaded")
                this.editedObject = editedObject.copy(
                    properties = editedObject.properties.mapOn(
                        { it.id == valueCreateResponse.objectPropertyId },
                        { it.addValue(valueCreateResponse) })
                )
                lastApiError = null
            }
        } catch (exception: ServerException) {
            setState {
                lastApiError = exception.message
            }
        }
    }

    override suspend fun editObjectValue(valueUpdateRequest: ValueUpdateRequest) {
        try {
            val valueEditResponse = updateValue(valueUpdateRequest)
            setState {
                val editedObject = this.editedObject ?: error("Object is not yet loaded")
                this.editedObject = editedObject.copy(
                    properties = editedObject.properties.mapOn(
                        { it.id == valueEditResponse.objectPropertyId },
                        { it.editValue(valueEditResponse) })
                )
                lastApiError = null
            }
        } catch (exception: ServerException) {
            setState {
                lastApiError = exception.message
            }
        }

    }

    override suspend fun deleteObjectValue(propertyId: String, id: String, force: Boolean) {
        try {
            deleteValue(id, force)
            setState {
                val editedObject = this.editedObject ?: error("Object is not yet loaded")
                this.editedObject = editedObject.copy(
                    properties = editedObject.properties.mapOn({ it.id == propertyId }, { it.deleteValue(id) })
                )
            }
        } catch (exception: ServerException) {
            setState {
                lastApiError = exception.message
            }
        }
    }

    private fun ObjectPropertyEditDetailsResponse.addValue(response: ValueCreateResponse): ObjectPropertyEditDetailsResponse {
        val valueDescriptor = ValueTruncated(
            id = response.id,
            value = response.value,
            description = response.description,
            propertyId = response.aspectPropertyId,
            version = response.version,
            childrenIds = emptyList()
        )
        return if (response.aspectPropertyId == null) {
            this.copy(
                rootValues = this.rootValues + valueDescriptor,
                valueDescriptors = this.valueDescriptors + valueDescriptor
            )
        } else {
            val parentValueId = response.parentValueId ?: error("Parent value should be null when aspectPropertyId is not null")
            val parentDescriptor = valueDescriptors.find { it.id == parentValueId } ?: error("Value descriptors should contain parent value")
            val newParentDescriptor = parentDescriptor.copy(childrenIds = parentDescriptor.childrenIds + response.id)

            when {
                newParentDescriptor.propertyId == null -> {
                    this.copy(
                        rootValues = this.rootValues.replaceBy({ it.id == newParentDescriptor.id }, newParentDescriptor),
                        valueDescriptors = this.valueDescriptors.replaceBy({ it.id == newParentDescriptor.id }, newParentDescriptor) + valueDescriptor
                    )
                }
                else -> {
                    this.copy(
                        valueDescriptors = this.valueDescriptors.replaceBy({ it.id == newParentDescriptor.id }, newParentDescriptor) + valueDescriptor
                    )
                }
            }
        }
    }

    private fun ObjectPropertyEditDetailsResponse.editValue(response: ValueUpdateResponse): ObjectPropertyEditDetailsResponse {
        return this.copy(
            rootValues = this.rootValues.mapOn(
                { it.id == response.id },
                { it.copy(value = response.value, version = response.version, description = response.description) }),
            valueDescriptors = this.valueDescriptors.mapOn(
                { it.id == response.id },
                { it.copy(value = response.value, version = response.version, description = response.description) })
        )
    }

    private fun ObjectPropertyEditDetailsResponse.deleteValue(valueId: String): ObjectPropertyEditDetailsResponse {
        val hasParentValue = this.rootValues.find { it.id == valueId } == null

        return if (hasParentValue) {
            val parentValue = this.valueDescriptors.find { it.childrenIds.contains(valueId) } ?: error("Parent value should exist")
            val changedParentValue = parentValue.copy(childrenIds = parentValue.childrenIds.filterNot { it == valueId })

            this.copy(
                rootValues = this.rootValues.replaceBy({ it.id == changedParentValue.id }, changedParentValue),
                valueDescriptors = this.valueDescriptors.replaceBy({ it.id == changedParentValue.id }, changedParentValue).filterNot { it.id == valueId }
            )
        } else {
            this.copy(
                rootValues = this.rootValues.filterNot { it.id == valueId },
                valueDescriptors = this.valueDescriptors.filterNot { it.id == valueId }
            )
        }
    }

    override fun RBuilder.render() {
        if (state.deleted) {
            reactRouter.Redirect {
                attrs {
                    to = "/objects"
                }
            }
        } else {
            state.editedObject?.let {
                objectTreeEditModel {
                    attrs {
                        apiModel = this@ObjectEditApiModelComponent
                        lastApiError = state.lastApiError
                        serverView = it
                    }
                }
            }
        }
    }

    interface State : RState {
        var editedObject: ObjectEditDetailsResponse?
        var lastApiError: String?
        var deleted: Boolean
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
