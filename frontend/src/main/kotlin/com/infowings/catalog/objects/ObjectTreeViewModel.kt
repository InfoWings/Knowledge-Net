package com.infowings.catalog.objects

import com.infowings.catalog.objects.treeview.objectLazyTreeView
import react.*

interface ObjectsLazyModel {
    fun expandObject(id: String)
    fun updateObject(index: Int, block: ObjectLazyViewModel.() -> Unit)
}

class ObjectTreeViewModelComponent(props: ObjectsViewApiConsumerProps) : RComponent<ObjectsViewApiConsumerProps, ObjectTreeViewModelComponent.State>(props),
    ObjectsLazyModel {

    override fun State.init(props: ObjectsViewApiConsumerProps) {
        objects = props.objects.toLazyView()
    }

    override fun componentWillReceiveProps(nextProps: ObjectsViewApiConsumerProps) {
        setState {
            objects = nextProps.objects.toLazyView()
        }
    }

    override fun expandObject(id: String) {
        props.objectApiModel.fetchDetailedObject(id)
    }

    override fun updateObject(index: Int, block: ObjectLazyViewModel.() -> Unit) = setState {
        objects[index].block()
    }

    override fun RBuilder.render() {
        objectLazyTreeView {
            attrs {
                objects = state.objects
                objectTreeViewModel = this@ObjectTreeViewModelComponent
            }
        }
    }

    interface State : RState {
        var objects: List<ObjectLazyViewModel>
    }
}

fun RBuilder.objectsViewModel(block: RHandler<ObjectsViewApiConsumerProps>) =
    child(ObjectTreeViewModelComponent::class, block)
