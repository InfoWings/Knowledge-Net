package com.infowings.catalog.objects

import com.infowings.catalog.common.*
import com.infowings.catalog.objects.treeview.objectTreeView
import kotlinx.coroutines.experimental.launch
import react.*

enum class Cardinality {
    ZERO, ONE, INFINITY
}

data class ObjectPropertyValueView(
    val id: String?,
    var value: String?
)

data class ObjTreeViewProperty(
    val id: String?,
    var name: String?,
    var cardinality: Cardinality?,
    var aspect: AspectData?,
    var value: ObjectPropertyValueView?
) {
    fun toObjPropertyData() = ObjPropertyData(
        id,
        name,
        cardinality?.name ?: error("Inconsistent State"),
        aspect ?: error("Inconsistent State"),
        listOf(
            // TODO: Proper data construction
            ObjectPropertyValueData(
                value?.id,
                listOf(Characteristics(aspect?.id ?: error("Inconsistent state"))),
                value?.value ?: error("Inconsistent State")
            )
        )
    )
}

data class ObjTreeView(
    val id: String?,
    var name: String?,
    var subject: SubjectData?,
    var properties: MutableList<ObjTreeViewProperty>,
    var expanded: Boolean = false
) {
    fun toObjData() =
        ObjData(id, name, subject ?: error("Inconsistent State"), properties.map { it.toObjPropertyData() })
}

interface ObjectTreeViewModel {
    fun selectObjTree(objTreeView: ObjTreeView)
    fun updateSelectedObjTree(updater: ObjTreeView.() -> Unit)
    fun updateObjTree(index: Int, updater: ObjTreeView.() -> Unit)
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

    override fun componentWillReceiveProps(nextProps: ObjectApiConsumerProps) = setState {
        // TODO: Smart merging of the incoming data into view state
        objForest = nextProps.objList.map {
            ObjTreeView(
                it.id,
                it.name,
                it.subject,
                it.properties.map {
                    ObjTreeViewProperty(
                        it.id,
                        it.name,
                        when (it.cardinality) {
                            "ZERO" -> Cardinality.ZERO
                            "ONE" -> Cardinality.ONE
                            "INFINITY" -> Cardinality.INFINITY
                            else -> error("Inconsistent State")
                        },
                        it.aspect,
                        ObjectPropertyValueView(
                            it.values.first().id,
                            it.values.first().scalarValue
                        )
                    )
                }.toMutableList()
            )
        }.toMutableList()
    }


    override fun updateSelectedObjTree(updater: ObjTreeView.() -> Unit) = setState {
        editedObjTree?.updater() ?: error("Inconsistent state")
    }

    override fun updateObjTree(index: Int, updater: ObjTreeView.() -> Unit) = setState {
        objForest[index].updater()
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
        // TODO: Probably different interfaces for creating an editing
        editedObjTree?.let { objTree ->
            objTree.id?.let {
                val objData = props.objMap[it] ?: error("Inconsistent State")
                objTree.name = objData.name
                objTree.subject = objData.subject
            } ?: error("Inconsistent State")
        }
        objForest.add(ObjTreeView(null, null, null, ArrayList()))
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
