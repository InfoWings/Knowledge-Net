package com.infowings.catalog.objects

import com.infowings.catalog.common.ObjData
import com.infowings.catalog.utils.get
import com.infowings.catalog.utils.post
import kotlinx.serialization.json.JSON
import react.*

suspend fun getAllObjects(): List<ObjData> = JSON.parse(get("/api/objects/all"))

suspend fun saveObject(objData: ObjData): ObjData = JSON.parse(post("/api/objects/save", JSON.stringify(objData)))

interface ObjectApiModel {
    suspend fun submitObj(objData: ObjData)
}

interface ObjectApiConsumerProps : RProps {
    var objList: List<ObjData>
    var objMap: Map<String, ObjData>
    var objectApiModel: ObjectApiModel
}

class ObjectApiModelComponent : RComponent<RProps, ObjectApiModelComponent.State>(), ObjectApiModel {

    override fun componentDidMount() {
//        fetchObjects()
        setState {
            objList = emptyList()
            objMap = HashMap()
        }
    }

//    private fun fetchObjects() {
//        launch {
//            val response = getAllObjects()
//            setState {
//                objList = response
//            }
//        }
//    }

    override suspend fun submitObj(objData: ObjData) {
//        val response = saveObject(objData)
//        setState {
//            objList += response
//        }
        objData.id?.let {
            TODO("UPDATING DOES NOT WORK YET")
        } ?: setState {
            val newObjDataId = objList.size.toString()
            val newObjData = objData.copy(id = newObjDataId)
            objList += newObjData
            objMap[newObjDataId] = newObjData
        }
    }

    override fun RBuilder.render() {
        objectTreeViewModel {
            attrs {
                objList = state.objList
                objMap = state.objMap
                objectApiModel = this@ObjectApiModelComponent
            }
        }
    }

    interface State : RState {
        var objList: List<ObjData>
        var objMap: MutableMap<String, ObjData>
    }
}

val RBuilder.objectApiModel
    get() = child(ObjectApiModelComponent::class) {}