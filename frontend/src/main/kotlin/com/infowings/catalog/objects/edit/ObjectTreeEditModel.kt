package com.infowings.catalog.objects.edit

import com.infowings.catalog.common.ObjectEditDetailsResponse
import com.infowings.catalog.common.ObjectValueData
import com.infowings.catalog.common.PropertyCardinality
import com.infowings.catalog.common.objekt.PropertyCreateRequest
import com.infowings.catalog.common.objekt.ValueCreateRequest
import com.infowings.catalog.objects.ObjectEditModel
import com.infowings.catalog.objects.ObjectPropertyEditModel
import com.infowings.catalog.objects.ObjectPropertyValueEditModel
import com.infowings.catalog.objects.edit.tree.objectEditTree
import kotlinx.coroutines.experimental.launch
import react.*


interface ObjectTreeEditModel {
    fun onUpdate(updater: ObjectEditModel.() -> Unit)
    fun onCreateProperty(propertyEditModel: ObjectPropertyEditModel)
    fun onCreateValue(value: ObjectValueData, objectPropertyId: String, parentValueId: String?, aspectPropertyId: String?)
}

class ObjectTreeEditModelComponent(props: Props) : RComponent<ObjectTreeEditModelComponent.Props, ObjectTreeEditModelComponent.State>(props),
    ObjectTreeEditModel {

    override fun State.init(props: Props) {
        model = ObjectEditModel(props.serverView)
    }

    override fun componentWillReceiveProps(nextProps: Props) {
        setState {
            /*if (nextProps.serverView.id == model.id) {
                model.mergeFrom(nextProps.serverView)
            } else {
                model = ObjectEditModel(nextProps.serverView)
            }*/
            model = ObjectEditModel(nextProps.serverView)
        }
    }

    override fun onUpdate(updater: ObjectEditModel.() -> Unit) = setState {
        model.updater()
    }

    override fun onCreateProperty(propertyEditModel: ObjectPropertyEditModel) {
        launch {
            props.apiModel.submitObjectProperty(
                PropertyCreateRequest(
                    state.model.id,
                    propertyEditModel.name,
                    propertyEditModel.description,
                    PropertyCardinality.ZERO.name,
                    propertyEditModel.aspect?.id ?: error("Aspect must be set when submitting object property")
                )
            )
        }
    }

    override fun onCreateValue(value: ObjectValueData, objectPropertyId: String, parentValueId: String?, aspectPropertyId: String?) {
        launch {
            props.apiModel.submitObjectValue(
                ValueCreateRequest(
                    value = value,
                    objectPropertyId = objectPropertyId,
                    measureId = null,
                    aspectPropertyId = aspectPropertyId,
                    parentValueId = parentValueId
                )
            )
        }
    }

    override fun RBuilder.render() {
        objectEditTree {
            attrs {
                editModel = this@ObjectTreeEditModelComponent
                objectTree = state.model
            }
        }
    }

    interface State : RState {
        var model: ObjectEditModel
    }

    interface Props : RProps {
        var serverView: ObjectEditDetailsResponse
        var apiModel: ObjectEditApiModel
    }
}

fun RBuilder.objectTreeEditModel(block: RHandler<ObjectTreeEditModelComponent.Props>) =
    child(ObjectTreeEditModelComponent::class, block)
