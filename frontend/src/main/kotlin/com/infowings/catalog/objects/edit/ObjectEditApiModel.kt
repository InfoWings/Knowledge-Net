package com.infowings.catalog.objects.edit

import com.infowings.catalog.aspects.getAspectTree
import com.infowings.catalog.common.*
import com.infowings.catalog.common.objekt.*
import com.infowings.catalog.objects.*
import com.infowings.catalog.utils.*
import com.infowings.catalog.wrappers.reactRouter
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import react.*

interface ObjectEditApiModel {
    fun submitObjectProperty(propertyCreateRequest: PropertyCreateRequest, selectedPropId: String?)
    fun submitObjectValue(valueCreateRequest: ValueCreateRequest)
    fun editObject(objectUpdateRequest: ObjectUpdateRequest, subjectName: String)
    fun editObjectProperty(propertyUpdateRequest: PropertyUpdateRequest)
    fun editObjectValue(valueUpdateRequest: ValueUpdateRequest)
    fun deleteObject(force: Boolean = false)
    fun deleteObjectProperty(id: String, force: Boolean = false)
    fun deleteObjectValue(id: String, force: Boolean = false)
}

class ObjectEditApiModelComponent : RComponent<ObjectEditApiModelComponent.Props, ObjectEditApiModelComponent.State>(),
    ObjectEditApiModel,
    JobCoroutineScope by JobSimpleCoroutineScope() {

    override fun State.init() {
        editedObject = null
        deleted = false
    }

    override fun componentWillUnmount() {
        job.cancel()
    }

    override fun componentDidMount() {
        job = Job()
        launch {
            val detailedObjectResponse = getDetailedObjectForEdit(props.objectId)
            setState {
                editedObject = detailedObjectResponse
            }
        }
    }

    override fun editObject(objectUpdateRequest: ObjectUpdateRequest, subjectName: String) {
        launch {
            tryRequest {
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
            }
        }
    }

    override fun deleteObject(force: Boolean) {
        launch {
            val toRethrow = tryRequest {
                val editedObject = state.editedObject ?: error("Object is not yet loaded")

                try {
                    deleteObject(editedObject.id, force)
                    setState {
                        this.editedObject = null
                        this.deleted = true
                        lastApiError = null
                    }

                    null
                } catch (badRequestException: BadRequestException) {
                    badRequestException
                }
            }

            toRethrow?.let { throw it }
        }
    }


    override fun submitObjectProperty(propertyCreateRequest: PropertyCreateRequest, aspectPropId: String?) {
        launch {
            tryRequest {
                val createPropertyResponse = createProperty(propertyCreateRequest)
                val treeAspectResponse = getAspectTree(propertyCreateRequest.aspectId)
                println("AP_Id: $aspectPropId, CPR: " + createPropertyResponse)

                val defaultRootValue = ValueTruncated(
                    createPropertyResponse.rootValue.id,
                    ObjectValueData.NullValue.toDTO(),
                    createPropertyResponse.rootValue.guid,
                    null,
                    null,
                    null,
                    createPropertyResponse.rootValue.version,
                    emptyList()
                )

                if (aspectPropId == null) {
                    setState {
                        val editedObject = this.editedObject ?: error("Object is not yet loaded")
                        this.editedObject = editedObject.copy(
                            version = createPropertyResponse.obj.version,
                            properties = editedObject.properties + ObjectPropertyEditDetailsResponse(
                                createPropertyResponse.id,
                                createPropertyResponse.name,
                                createPropertyResponse.description,
                                createPropertyResponse.version,
                                listOf(defaultRootValue),
                                listOf(defaultRootValue),
                                treeAspectResponse
                            )
                        )
                        lastApiError = null
                    }
                } else {
                    val valueCreateResponse = createValue(
                        ValueCreateRequest(
                            value = ObjectValueData.NullValue, description = null,
                            objectPropertyId = createPropertyResponse.id, measureName = null, aspectPropertyId = aspectPropId,
                            parentValueId = createPropertyResponse.rootValue.id
                        )
                    )
                    setState {
                        setState {
                            val editedObject = this.editedObject ?: error("Object is not yet loaded")
                            this.editedObject = editedObject.copy(
                                version = createPropertyResponse.obj.version,
                                properties = editedObject.properties + ObjectPropertyEditDetailsResponse(
                                    createPropertyResponse.id,
                                    createPropertyResponse.name,
                                    createPropertyResponse.description,
                                    createPropertyResponse.version,
                                    listOf(defaultRootValue),
                                    listOf(defaultRootValue),
                                    treeAspectResponse
                                ).addValue(valueCreateResponse)
                            )
                            lastApiError = null
                        }
                    }
                }
            }
        }
    }

    override fun editObjectProperty(propertyUpdateRequest: PropertyUpdateRequest) {
        launch {
            tryRequest {
                val editPropertyResponse = updateProperty(propertyUpdateRequest)
                setState {
                    val editedObject = this.editedObject ?: error("Object is not yet loaded")
                    this.editedObject = editedObject.copy(
                        version = editPropertyResponse.obj.version,
                        properties = editedObject.properties.mapOn(
                            { it.id == editPropertyResponse.id },
                            {
                                it.copy(
                                    name = editPropertyResponse.name,
                                    description = editPropertyResponse.description,
                                    version = editPropertyResponse.version
                                )
                            }
                        )
                    )
                    lastApiError = null
                }
            }
        }
    }

    override fun deleteObjectProperty(id: String, force: Boolean) {
        launch {
            val toRethrow = tryRequest {
                try {
                    val propertyDeleteResponse = deleteProperty(id, force)
                    setState {
                        val editedObject = this.editedObject ?: error("Object is not yet loaded")
                        this.editedObject = editedObject.copy(
                            version = propertyDeleteResponse.obj.version,
                            properties = editedObject.properties.filterNot { it.id == propertyDeleteResponse.id }
                        )
                        lastApiError = null
                    }

                    null
                } catch (badRequestException: BadRequestException) {
                    badRequestException
                }
            }

            toRethrow?.let { throw it }
        }
    }

    override fun submitObjectValue(valueCreateRequest: ValueCreateRequest) {
        launch {
            tryRequest {
                val valueCreateResponse = createValue(valueCreateRequest)
                setState {
                    val editedObject = this.editedObject ?: error("Object is not yet loaded")
                    this.editedObject = editedObject.copy(
                        properties = editedObject.properties.mapOn(
                            { it.id == valueCreateResponse.objectProperty.id },
                            {
                                it.addValue(valueCreateResponse)
                            }
                        )
                    )
                    lastApiError = null
                }
            }
        }
    }

    override fun editObjectValue(valueUpdateRequest: ValueUpdateRequest) {
        launch {
            tryRequest {
                val valueEditResponse = updateValue(valueUpdateRequest)
                setState {
                    val editedObject = this.editedObject ?: error("Object is not yet loaded")
                    this.editedObject = editedObject.copy(
                        properties = editedObject.properties.mapOn(
                            { it.id == valueEditResponse.objectProperty.id },
                            { it.editValue(valueEditResponse) })
                    )
                    lastApiError = null
                }
            }
        }
    }

    override fun deleteObjectValue(id: String, force: Boolean) {
        launch {
            val toRethrow = tryRequest {
                try {
                    val valueDeleteResponse = deleteValue(id, force)
                    setState {
                        val editedObject = this.editedObject ?: error("Object is not yet loaded")
                        this.editedObject = editedObject.copy(
                            properties = editedObject.properties.mapOn(
                                { it.id == valueDeleteResponse.objectProperty.id },
                                { it.deleteValues(valueDeleteResponse, id) })
                        )
                        lastApiError = null
                    }

                    null
                } catch (badRequestException: BadRequestException) {
                    badRequestException
                }
            }

            toRethrow?.let { throw it }
        }
    }

    private inline fun <T> tryRequest(block: () -> T): T? {
        try {
            return block()
        } catch (apiException: ApiException) {
            setState {
                lastApiError = apiException
            }
            return null
        }
    }

    private fun ObjectPropertyEditDetailsResponse.addValue(response: ValueChangeResponse): ObjectPropertyEditDetailsResponse {
        val valueDescriptor = ValueTruncated(
            id = response.id,
            value = response.value,
            measureName = response.measureName,
            description = response.description,
            guid = response.guid,
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
            val parentValue = response.parentValue ?: error("Parent value should be null when aspectPropertyId is not null")
            val parentDescriptor = valueDescriptors.find { it.id == parentValue.id } ?: error("Value descriptors should contain parent value")
            val newParentDescriptor = parentDescriptor.copy(version = parentValue.version, childrenIds = parentDescriptor.childrenIds + response.id)

            when {
                newParentDescriptor.propertyId == null -> {
                    this.copy(
                        version = response.objectProperty.version,
                        rootValues = this.rootValues.replaceBy({ it.id == newParentDescriptor.id }, newParentDescriptor),
                        valueDescriptors = this.valueDescriptors.replaceBy({ it.id == newParentDescriptor.id }, newParentDescriptor) + valueDescriptor
                    )
                }
                else -> {
                    this.copy(
                        version = response.objectProperty.version,
                        valueDescriptors = this.valueDescriptors.replaceBy({ it.id == newParentDescriptor.id }, newParentDescriptor) + valueDescriptor
                    )
                }
            }
        }
    }

    private fun ObjectPropertyEditDetailsResponse.editValue(response: ValueChangeResponse): ObjectPropertyEditDetailsResponse {
        return this.copy(
            version = response.objectProperty.version,
            rootValues = this.rootValues.map {
                when {
                    it.id == response.id -> it.copy(
                        value = response.value,
                        measureName = response.measureName,
                        version = response.version,
                        description = response.description
                    )
                    it.id == response.parentValue?.id -> it.copy(version = response.parentValue.version)
                    else -> it
                }
            },
            valueDescriptors = this.valueDescriptors.map {
                when {
                    it.id == response.id -> it.copy(
                        value = response.value,
                        measureName = response.measureName,
                        version = response.version,
                        description = response.description
                    )
                    it.id == response.parentValue?.id -> it.copy(version = response.parentValue.version)
                    else -> it
                }
            }
        )
    }

    private fun ObjectPropertyEditDetailsResponse.deleteValues(valueDeleteResponse: ValueDeleteResponse, valueId: String): ObjectPropertyEditDetailsResponse {
        return this.copy(
            version = valueDeleteResponse.objectProperty.version,
            rootValues = this.rootValues.asSequence()
                .filterNot { valueDeleteResponse.deletedValues.contains(it.id) || valueDeleteResponse.markedValues.contains(it.id) }
                .map { rootValue ->
                    when {
                        rootValue.id == valueDeleteResponse.parentValue?.id ->
                            rootValue.copy(
                                version = valueDeleteResponse.parentValue.version,
                                childrenIds = rootValue.childrenIds.filterNot { it == valueId }
                            )
                        else -> rootValue
                    }
                }.filterNotNull().toList(),
            valueDescriptors = this.valueDescriptors.asSequence()
                .filterNot { valueDeleteResponse.deletedValues.contains(it.id) || valueDeleteResponse.markedValues.contains(it.id) }
                .map { value ->
                    when {
                        value.id == valueDeleteResponse.parentValue?.id ->
                            value.copy(
                                version = valueDeleteResponse.parentValue.version,
                                childrenIds = value.childrenIds.filterNot { it == valueId }
                            )
                        else -> value
                    }
                }.toList()
        )
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
                        editMode = props.editMode
                        highLightedGuid = props.highlightedGuid
                    }
                }
            }
        }
    }

    interface State : RState {
        var editedObject: ObjectEditDetailsResponse?
        var lastApiError: ApiException?
        var deleted: Boolean
    }

    interface Props : RProps {
        var objectId: String
        var editMode: Boolean
        var highlightedGuid: String?
    }
}

fun RBuilder.objectEditApiModel(objectId: String, editMode: Boolean, highlightedGuid: String?) = child(ObjectEditApiModelComponent::class) {
    attrs {
        this.objectId = objectId
        this.editMode = editMode
        this.highlightedGuid = highlightedGuid
    }
}
