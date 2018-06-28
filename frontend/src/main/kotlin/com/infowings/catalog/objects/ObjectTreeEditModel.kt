package com.infowings.catalog.objects

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.objects.treeedit.objectTreeView
import kotlinx.coroutines.experimental.launch
import react.*


interface ObjectTreeEditModel {
    fun selectObject(objTreeEdit: ObjectEditModel)
    fun updateSelectedObject(updater: ObjectEditModel.() -> Unit)
    fun updateObject(index: Int, updater: ObjectEditModel.() -> Unit)
    fun addObject()
    fun saveObject()
}

interface ObjectTreeEditModelConsumerProps : RProps {
    var objectForest: List<ObjectEditModel>
    var editedObject: ObjectEditModel?
    var objectTreeViewModel: ObjectTreeEditModel
    var aspectsMap: Map<String, AspectData>
}

class ObjectTreeEditModelComponent : RComponent<ObjectApiConsumerProps, ObjectTreeEditModelComponent.State>(),
    ObjectTreeEditModel {

    override fun State.init() {
        objects = ArrayList()
        editedObject = null
    }

    override fun componentWillReceiveProps(nextProps: ObjectApiConsumerProps) = setState {
        // TODO: Smart merging of the incoming data into view state
        objects = nextProps.objList.map(::ObjectEditModel).toMutableList()
    }


    override fun updateSelectedObject(updater: ObjectEditModel.() -> Unit) = setState {
        editedObject?.updater() ?: error("Inconsistent state")
    }

    override fun updateObject(index: Int, updater: ObjectEditModel.() -> Unit) = setState {
        objects[index].updater()
    }

    override fun saveObject() = setState {
        val savedObjData = editedObject?.toObjectData() ?: error("Inconsistent state")
        launch {
            props.objectApiModel.submitObj(savedObjData)
        }
        editedObject = null
    }

    override fun selectObject(objTreeEdit: ObjectEditModel) = setState {
        if (objTreeEdit != editedObject) {
            editedObject?.let { objTree ->
                objTree.id?.let {
                    val objData = props.objMap[it] ?: error("Inconsistent State")
                    objTree.name = objData.name
                    objTree.subject = objData.subject
                } ?: objects.removeAt(objects.lastIndex)
            }
            editedObject = objTreeEdit
        }
    }

    override fun addObject() = setState {
        // TODO: Probably different interfaces for creating an editing
        editedObject?.let { objTree ->
            objTree.id?.let {
                val objData = props.objMap[it] ?: error("Inconsistent State")
                objTree.name = objData.name
                objTree.subject = objData.subject
            } ?: error("Inconsistent State")
        }
        objects.add(ObjectEditModel(null, null, null, ArrayList()))
        editedObject = objects.last()
    }

    override fun RBuilder.render() {
        objectTreeView {
            attrs {
                objectForest = state.objects
                editedObject = state.editedObject
                objectTreeViewModel = this@ObjectTreeEditModelComponent
                aspectsMap = props.aspectMap
            }
        }
    }

    interface State : RState {
        var objects: MutableList<ObjectEditModel>
        var editedObject: ObjectEditModel?
    }
}

fun RBuilder.objectTreeEditModel(block: RHandler<ObjectApiConsumerProps>) =
    child(ObjectTreeEditModelComponent::class, block)
