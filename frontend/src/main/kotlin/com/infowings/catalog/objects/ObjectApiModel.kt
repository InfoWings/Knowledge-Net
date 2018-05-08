package com.infowings.catalog.objects

import com.infowings.catalog.aspects.getAllAspects
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.ObjectData
import com.infowings.catalog.utils.get
import com.infowings.catalog.utils.post
import kotlinx.coroutines.experimental.launch
import kotlinx.serialization.json.JSON
import react.*

suspend fun getAllObjects(): List<ObjectData> = JSON.parse(get("/api/objects/all"))

suspend fun saveObject(objData: ObjectData): ObjectData = JSON.parse(post("/api/objects/save", JSON.stringify(objData)))

interface ObjectApiModel {
    suspend fun submitObj(objData: ObjectData)
}

interface ObjectApiConsumerProps : RProps {
    var objList: List<ObjectData>
    var objMap: Map<String, ObjectData>
    var aspectMap: Map<String, AspectData>
    var objectApiModel: ObjectApiModel
}

class ObjectApiModelComponent : RComponent<RProps, ObjectApiModelComponent.State>(), ObjectApiModel {

    override fun componentDidMount() {
//        fetchObjects()
        launch {
            val aspects = getAllAspects(emptyList())
            setState {
                objList = emptyList()
                objMap = HashMap()
                aspectMap = aspects.aspects.associateBy { it.id ?: error("Client received aspects with id == null") }
            }
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

    override suspend fun submitObj(objData: ObjectData) {
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
                aspectMap = state.aspectMap
                objectApiModel = this@ObjectApiModelComponent
            }
        }
    }

    interface State : RState {
        var objList: List<ObjectData>
        var objMap: MutableMap<String, ObjectData>
        var aspectMap: Map<String, AspectData>
    }
}

val RBuilder.objectApiModel
    get() = child(ObjectApiModelComponent::class) {}