package com.infowings.catalog.objects.edit

import com.infowings.catalog.common.ObjectEditDetailsResponse
import com.infowings.catalog.objects.ObjectEditModel
import com.infowings.catalog.objects.edit.tree.objectEditTree
import react.*


interface ObjectTreeEditModel {
    fun onUpdate(updater: ObjectEditModel.() -> Unit)
}

class ObjectTreeEditModelComponent(props: Props) : RComponent<ObjectTreeEditModelComponent.Props, ObjectTreeEditModelComponent.State>(props),
    ObjectTreeEditModel {

    override fun State.init(props: Props) {
        model = ObjectEditModel(props.serverView)
    }

    override fun onUpdate(updater: ObjectEditModel.() -> Unit) = setState {
        model.updater()
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
