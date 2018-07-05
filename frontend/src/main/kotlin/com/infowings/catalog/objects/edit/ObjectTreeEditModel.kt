package com.infowings.catalog.objects.edit

import com.infowings.catalog.common.ObjectEditDetailsResponse
import com.infowings.catalog.common.objekt.PropertyCreateRequest
import com.infowings.catalog.objects.ObjectEditModel
import com.infowings.catalog.objects.ObjectPropertyEditModel
import com.infowings.catalog.objects.ObjectPropertyValueEditModel
import com.infowings.catalog.objects.edit.tree.objectEditTree
import kotlinx.coroutines.experimental.launch
import react.*


interface ObjectTreeEditModel {
    fun onUpdate(updater: ObjectEditModel.() -> Unit)
    fun onCreateProperty(propertyEditModel: ObjectPropertyEditModel)
    fun onCreateValue(valueEditModel: ObjectPropertyValueEditModel, parentValueId: String, aspectPropertyId: String?)
}

class ObjectTreeEditModelComponent(props: Props) : RComponent<ObjectTreeEditModelComponent.Props, ObjectTreeEditModelComponent.State>(props),
    ObjectTreeEditModel {

    override fun State.init(props: Props) {
        model = ObjectEditModel(props.serverView)
    }

    override fun componentWillReceiveProps(nextProps: Props) {
        setState {
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
                    propertyEditModel.cardinality?.name ?: error("Cardinality must be set when submitting object property"),
                    propertyEditModel.aspect?.id ?: error("Aspect must be set when submitting object property")
                )
            )
        }
    }

    override fun onCreateValue(valueEditModel: ObjectPropertyValueEditModel, parentValueId: String, aspectPropertyId: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
