package com.infowings.catalog.objects.edit

import com.infowings.catalog.common.ObjectEditDetailsResponse
import com.infowings.catalog.common.objekt.*
import com.infowings.catalog.errors.showError
import com.infowings.catalog.objects.ObjectEditViewModel
import com.infowings.catalog.objects.ObjectPropertyEditModel
import com.infowings.catalog.objects.edit.tree.objectEditTree
import com.infowings.catalog.utils.ApiException
import com.infowings.catalog.utils.BadRequestException
import com.infowings.catalog.wrappers.blueprint.Alert
import com.infowings.catalog.wrappers.blueprint.Intent
import kotlinx.coroutines.experimental.launch
import react.*
import react.dom.div
import react.dom.h3
import react.dom.span


interface ObjectTreeEditModel {
    fun update(updater: ObjectEditViewModel.() -> Unit)
    fun updateObject()
    fun deleteObject()
    fun createProperty(propertyEditModel: ObjectPropertyEditModel)
    fun updateProperty(propertyEditModel: ObjectPropertyEditModel)
    fun deleteProperty(propertyEditModel: ObjectPropertyEditModel)
    fun createValue(valueCreateRequest: ValueCreateRequest, onResponse: (ValueChangeResponse?) -> Unit = {})
    fun updateValue(valueUpdateRequest: ValueUpdateRequest)
    fun deleteValue(valueId: String)
}

class ObjectTreeEditModelComponent(props: Props) : RComponent<ObjectTreeEditModelComponent.Props, ObjectTreeEditModelComponent.State>(props),
    ObjectTreeEditModel {

    override fun State.init(props: Props) {
        editContextModel = null
        viewModel = ObjectEditViewModel(props.serverView)
    }

    override fun componentWillReceiveProps(nextProps: Props) {
        nextProps.lastApiError?.let {
            showError(it)
        }
        setState {
            if (nextProps.serverView.id == viewModel.id) {
                viewModel.mergeFrom(nextProps.serverView)
            } else {
                viewModel = ObjectEditViewModel(nextProps.serverView)
            }
        }
    }

    override fun update(updater: ObjectEditViewModel.() -> Unit) = setState {
            viewModel.updater()
        }


    override fun updateObject() {
        launch {
            props.apiModel.editObject(
                ObjectUpdateRequest(
                    state.viewModel.id,
                    state.viewModel.name,
                    state.viewModel.description,
                    state.viewModel.subject.id,
                    state.viewModel.version
                ),
                state.viewModel.subject.name
            )
        }
    }

    override fun deleteObject() {
        deleteEntity { force ->
            props.apiModel.deleteObject(force)
        }
    }

    override fun createProperty(propertyEditModel: ObjectPropertyEditModel) {
        launch {
            props.apiModel.submitObjectProperty(
                PropertyCreateRequest(
                    state.viewModel.id,
                    propertyEditModel.name,
                    propertyEditModel.description,
                    propertyEditModel.aspect?.id ?: error("Aspect must be set when submitting object property")
                )
            )
        }
    }

    override fun updateProperty(propertyEditModel: ObjectPropertyEditModel) {
        launch {
            props.apiModel.editObjectProperty(
                PropertyUpdateRequest(
                    propertyEditModel.id ?: error("Property should have id in order to be updated"),
                    propertyEditModel.name,
                    propertyEditModel.description,
                    propertyEditModel.version ?: error("Property should have version in order to be updated")
                )
            )
        }
    }

    override fun deleteProperty(propertyEditModel: ObjectPropertyEditModel) {
        val propertyId = propertyEditModel.id ?: error("Property should have id in order to be deleted")
        deleteEntity { force -> props.apiModel.deleteObjectProperty(propertyId, force) }
    }

    override fun createValue(valueCreateRequest: ValueCreateRequest, onResponse: (ValueChangeResponse?) -> Unit) {
        launch {
            val response = props.apiModel.submitObjectValue(valueCreateRequest)
            onResponse(response)
        }
    }

    override fun updateValue(valueUpdateRequest: ValueUpdateRequest) {
        launch {
            props.apiModel.editObjectValue(valueUpdateRequest)
        }
    }

    override fun deleteValue(valueId: String) {
        deleteEntity { force -> props.apiModel.deleteObjectValue(valueId, force) }
    }

    private fun deleteEntity(deleteOperation: suspend (force: Boolean) -> Unit) {
        launch {
            try {
                deleteOperation(false)
            } catch (badRequestException: BadRequestException) {
                setState {
                    entityDeleteInfo = EntityDeleteInfo(badRequestException.message) {
                        launch {
                            deleteOperation(true)
                            setState {
                                entityDeleteInfo = null
                            }
                        }
                    }
                }
            }
        }
    }

    override fun RBuilder.render() {
        objectEditTree {
            attrs {
                editModel = this@ObjectTreeEditModelComponent
                objectTree = state.viewModel
                apiModel = props.serverView
                editContext = EditContext(state.editContextModel) { setState { editContextModel = it } }
                editMode = props.editMode
                highlightedGuid = props.highLightedGuid
            }
        }
        state.entityDeleteInfo?.let { entityDeleteInfo ->
            Alert {
                attrs {
                    cancelButtonText = "Cancel"
                    confirmButtonText = "Delete"
                    onCancel = {
                        setState {
                            this.entityDeleteInfo = null
                        }
                    }
                    intent = Intent.DANGER
                    isOpen = true
                    onConfirm = {
                        entityDeleteInfo.continuation()
                    }
                }
                div(classes = "linked-object-window") {
                    h3 { +"The entity is linked" }
                    span { +entityDeleteInfo.message }
                }
            }
        }
    }

    interface State : RState {
        var viewModel: ObjectEditViewModel
        var editContextModel: EditContextModel?
        var entityDeleteInfo: EntityDeleteInfo?
    }

    interface Props : RProps {
        var serverView: ObjectEditDetailsResponse
        var apiModel: ObjectEditApiModel
        var lastApiError: ApiException?
        var editMode: Boolean
        var highLightedGuid: String?
    }

    data class EntityDeleteInfo(val message: String, val continuation: () -> Unit)
}

fun RBuilder.objectTreeEditModel(block: RHandler<ObjectTreeEditModelComponent.Props>) =
    child(ObjectTreeEditModelComponent::class, block)
