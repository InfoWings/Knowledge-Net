package com.infowings.catalog.objects.view

import com.infowings.catalog.objects.ObjectLazyViewModel
import com.infowings.catalog.objects.mergeDetails
import com.infowings.catalog.objects.toLazyView
import com.infowings.catalog.objects.view.tree.objectLazyTreeView
import react.*

interface ObjectsLazyModel {
    fun requestDetailed(id: String)
    fun updateObject(index: Int, block: ObjectLazyViewModel.() -> Unit)
}

class ObjectTreeViewModelComponent(props: ObjectsViewApiConsumerProps) : RComponent<ObjectsViewApiConsumerProps, ObjectTreeViewModelComponent.State>(props),
    ObjectsLazyModel {

    override fun State.init(props: ObjectsViewApiConsumerProps) {
        objects = props.objects.toLazyView(props.detailedObjectsView)
    }

    override fun componentWillReceiveProps(nextProps: ObjectsViewApiConsumerProps) {
        if (props.objects != nextProps.objects) {
            setState {
                objects = nextProps.objects.toLazyView(nextProps.detailedObjectsView)
            }
        } else {
            setState {
                objects = objects.mergeDetails(nextProps.detailedObjectsView)
            }
        }
    }

    override fun requestDetailed(id: String) {
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
