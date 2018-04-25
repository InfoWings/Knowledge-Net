package com.infowings.catalog.objects

import com.infowings.catalog.common.ObjData
import com.infowings.catalog.objects.treeview.objectTreeView
import kotlinx.coroutines.experimental.launch
import react.*

data class ObjTreeView(
    var id: String?,
    var name: String?,
    var subject: String?
) {
    fun toObjData() = ObjData(id, name, subject)
}

interface ObjectTreeViewModel {
    fun selectObjTree(objTreeView: ObjTreeView)
    fun updateObjTree(updater: ObjTreeView.() -> Unit)
    fun addNewObjTree()
    fun saveObjTree()
}

interface ObjectTreeViewModelConsumerProps : RProps {
    var objectForest: List<ObjTreeView>
    var editedObject: ObjTreeView?
    var objectTreeViewModel: ObjectTreeViewModel
}

class ObjectTreeViewModelComponent : RComponent<ObjectApiConsumerProps, ObjectTreeViewModelComponent.State>(),
    ObjectTreeViewModel {

    override fun State.init() {
        objForest = ArrayList()
        editedObjTree = null
    }

    override fun componentWillReceiveProps(nextProps: ObjectApiConsumerProps) {
        // Think what to do about diff
        console.log(nextProps.objList)
        setState {
            objForest = nextProps.objList.map { ObjTreeView(it.id, it.name, it.subject) }.toMutableList()
        }
    }

    override fun updateObjTree(updater: ObjTreeView.() -> Unit) = setState {
        editedObjTree?.updater() ?: error("Inconsistent state")
    }

    override fun saveObjTree() = setState {
        val savedObjData = editedObjTree?.toObjData() ?: error("Inconsistent state")
        launch {
            props.objectApiModel.submitObj(savedObjData)
        }
        editedObjTree = null
    }

    override fun selectObjTree(objTreeView: ObjTreeView) = setState {
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
        editedObjTree?.let { objTree ->
            objTree.id?.let {
                val objData = props.objMap[it] ?: error("Inconsistent State")
                objTree.name = objData.name
                objTree.subject = objData.subject
            } ?: error("Inconsistent State")
        }
        objForest.add(ObjTreeView(null, null, null))
        editedObjTree = objForest.last()
    }

    override fun RBuilder.render() {
        objectTreeView {
            attrs {
                objectForest = state.objForest
                editedObject = state.editedObjTree
                objectTreeViewModel = this@ObjectTreeViewModelComponent
            }
        }

    }

    interface State : RState {
        var objForest: MutableList<ObjTreeView>
        var editedObjTree: ObjTreeView?
    }
}

fun RBuilder.objectTreeViewModel(block: RHandler<ObjectApiConsumerProps>) =
    child(ObjectTreeViewModelComponent::class, block)
