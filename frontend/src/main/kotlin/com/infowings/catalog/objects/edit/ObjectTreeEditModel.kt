package com.infowings.catalog.objects.edit

import com.infowings.catalog.common.ObjectEditDetailsResponse
import com.infowings.catalog.common.ObjectValueData
import com.infowings.catalog.common.objekt.PropertyCreateRequest
import com.infowings.catalog.common.objekt.PropertyUpdateRequest
import com.infowings.catalog.common.objekt.ValueCreateRequest
import com.infowings.catalog.common.objekt.ValueUpdateRequest
import com.infowings.catalog.objects.ObjectEditModel
import com.infowings.catalog.objects.ObjectPropertyEditModel
import com.infowings.catalog.objects.edit.tree.objectEditTree
import com.infowings.catalog.utils.BadRequestException
import com.infowings.catalog.wrappers.blueprint.Alert
import com.infowings.catalog.wrappers.blueprint.Intent
import kotlinx.coroutines.experimental.launch
import react.*
import react.dom.div
import react.dom.h3
import react.dom.span


interface ObjectTreeEditModel {
    fun update(updater: ObjectEditModel.() -> Unit)
    fun deleteObject()
    fun createProperty(propertyEditModel: ObjectPropertyEditModel)
    fun updateProperty(propertyEditModel: ObjectPropertyEditModel)
    fun deleteProperty(propertyEditModel: ObjectPropertyEditModel)
    fun createValue(value: ObjectValueData, objectPropertyId: String, parentValueId: String?, aspectPropertyId: String?)
    fun updateValue(valueId: String, propertyId: String, value: ObjectValueData)
    fun deleteValue(valueId: String, propertyId: String)
}

class ObjectTreeEditModelComponent(props: Props) : RComponent<ObjectTreeEditModelComponent.Props, ObjectTreeEditModelComponent.State>(props),
    ObjectTreeEditModel {

    override fun State.init(props: Props) {
        model = ObjectEditModel(props.serverView)
    }

    override fun componentWillReceiveProps(nextProps: Props) {
        setState {
            if (nextProps.serverView.id == model.id) {
                model.mergeFrom(nextProps.serverView)
            } else {
                model = ObjectEditModel(nextProps.serverView)
            }
        }
    }

    override fun update(updater: ObjectEditModel.() -> Unit) = setState {
        model.updater()
    }

    override fun deleteObject() {
        deleteEntity { force -> props.apiModel.deleteObject(force) }
    }

    override fun createProperty(propertyEditModel: ObjectPropertyEditModel) {
        launch {
            props.apiModel.submitObjectProperty(
                PropertyCreateRequest(
                    state.model.id,
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
                    propertyEditModel.name ?: TODO("Does property should have name in order to be updated?")
                )
            )
        }
    }

    override fun deleteProperty(propertyEditModel: ObjectPropertyEditModel) {
        val propertyId = propertyEditModel.id ?: error("Property should have id in order to be deleted")
        deleteEntity { force -> props.apiModel.deleteObjectProperty(propertyId, force) }
    }

    override fun createValue(value: ObjectValueData, objectPropertyId: String, parentValueId: String?, aspectPropertyId: String?) {
        launch {
            props.apiModel.submitObjectValue(
                ValueCreateRequest(
                    value = value,
                    description = null,
                    objectPropertyId = objectPropertyId,
                    measureId = null,
                    aspectPropertyId = aspectPropertyId,
                    parentValueId = parentValueId
                )
            )
        }
    }

    override fun updateValue(valueId: String, propertyId: String, value: ObjectValueData) {
        launch {
            props.apiModel.editObjectValue(
                propertyId,
                ValueUpdateRequest(
                    valueId = valueId,
                    value = value
                )
            )
        }
    }

    override fun deleteValue(valueId: String, propertyId: String) {
        deleteEntity { force -> props.apiModel.deleteObjectValue(propertyId, valueId, force) }
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
                objectTree = state.model
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
        var model: ObjectEditModel
        var entityDeleteInfo: EntityDeleteInfo?
    }

    interface Props : RProps {
        var serverView: ObjectEditDetailsResponse
        var apiModel: ObjectEditApiModel
    }

    data class EntityDeleteInfo(val message: String, val continuation: () -> Unit)
}

fun RBuilder.objectTreeEditModel(block: RHandler<ObjectTreeEditModelComponent.Props>) =
    child(ObjectTreeEditModelComponent::class, block)
