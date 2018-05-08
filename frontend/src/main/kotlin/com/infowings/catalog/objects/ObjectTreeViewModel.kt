package com.infowings.catalog.objects

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.objects.treeview.objectTreeView
import kotlinx.coroutines.experimental.launch
import react.*


interface ObjectTreeViewModel {
    fun selectObjTree(objTreeView: ObjectViewModel)
    fun updateSelectedObjTree(updater: ObjectViewModel.() -> Unit)
    fun updateObjTree(index: Int, updater: ObjectViewModel.() -> Unit)
    fun addNewObjTree()
    fun saveObjTree()
}

interface ObjectTreeViewModelConsumerProps : RProps {
    var objectForest: List<ObjectViewModel>
    var editedObject: ObjectViewModel?
    var objectTreeViewModel: ObjectTreeViewModel
    var aspectsMap: Map<String, AspectData>
}

class ObjectTreeViewModelComponent : RComponent<ObjectApiConsumerProps, ObjectTreeViewModelComponent.State>(),
    ObjectTreeViewModel {

    override fun State.init() {
        objForest = ArrayList()
        editedObjTree = null
    }

    override fun componentWillReceiveProps(nextProps: ObjectApiConsumerProps) = setState {
        // TODO: Smart merging of the incoming data into view state
        objForest = nextProps.objList.map { (id, name, subject, properties) ->
            ObjectViewModel(
                id,
                name,
                subject,
                properties.map { (id, name, cardinality, aspect, values) ->
                    ObjectPropertyViewModel(
                        id,
                        name,
                        when (cardinality) {
                            "ZERO" -> Cardinality.ZERO
                            "ONE" -> Cardinality.ONE
                            "INFINITY" -> Cardinality.INFINITY
                            else -> error("Inconsistent State")
                        },
                        aspect,
                        values.map { (id, scalarValue, _) ->
                            ObjectPropertyValueViewModel(
                                id,
                                scalarValue
                            )
                        }.toMutableList()
                    )
                }.toMutableList()
            )
        }.toMutableList()
    }


    override fun updateSelectedObjTree(updater: ObjectViewModel.() -> Unit) = setState {
        editedObjTree?.updater() ?: error("Inconsistent state")
    }

    override fun updateObjTree(index: Int, updater: ObjectViewModel.() -> Unit) = setState {
        objForest[index].updater()
    }

    override fun saveObjTree() = setState {
        val savedObjData = editedObjTree?.toObjectData() ?: error("Inconsistent state")
        launch {
            props.objectApiModel.submitObj(savedObjData)
        }
        editedObjTree = null
    }

    override fun selectObjTree(objTreeView: ObjectViewModel) = setState {
        if (objTreeView != editedObjTree) {
            editedObjTree?.let { objTree ->
                objTree.id?.let {
                    val objData = props.objMap[it] ?: error("Inconsistent State")
                    objTree.name = objData.name
                    objTree.subject = objData.subject
                } ?: objForest.removeAt(objForest.lastIndex)
            }
            editedObjTree = objTreeView
        }
    }

    override fun addNewObjTree() = setState {
        // TODO: Probably different interfaces for creating an editing
        editedObjTree?.let { objTree ->
            objTree.id?.let {
                val objData = props.objMap[it] ?: error("Inconsistent State")
                objTree.name = objData.name
                objTree.subject = objData.subject
            } ?: error("Inconsistent State")
        }
        objForest.add(ObjectViewModel(null, null, null, ArrayList()))
        editedObjTree = objForest.last()
    }

    override fun RBuilder.render() {
        objectTreeView {
            attrs {
                objectForest = state.objForest
                editedObject = state.editedObjTree
                objectTreeViewModel = this@ObjectTreeViewModelComponent
                aspectsMap = props.aspectMap
            }
        }
    }

    interface State : RState {
        var objForest: MutableList<ObjectViewModel>
        var editedObjTree: ObjectViewModel?
    }
}

fun RBuilder.objectTreeViewModel(block: RHandler<ObjectApiConsumerProps>) =
    child(ObjectTreeViewModelComponent::class, block)
