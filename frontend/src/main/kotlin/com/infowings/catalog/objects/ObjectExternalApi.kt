package com.infowings.catalog.objects

import com.infowings.catalog.common.DetailedObjectResponse
import com.infowings.catalog.common.ObjectData
import com.infowings.catalog.common.ObjectPropertyData
import com.infowings.catalog.common.ObjectsResponse
import com.infowings.catalog.common.objekt.*
import com.infowings.catalog.utils.encodeURIComponent
import com.infowings.catalog.utils.get
import com.infowings.catalog.utils.post
import kotlinx.serialization.json.JSON

suspend fun getAllObjects(): ObjectsResponse = JSON.parse(get("/api/objects"))

suspend fun getDetailedObject(id: String): DetailedObjectResponse = JSON.parse(get("/api/objects/${encodeURIComponent(id)}"))

suspend fun saveObject(objData: ObjectData): ObjectData = JSON.parse(post("/api/object/save", JSON.stringify(objData)))

suspend fun createObject(request: ObjectCreateRequest): ObjectCreateResponse =
    JSON.parse(post("/api/objects/create", JSON.stringify(request)))

suspend fun createObject(data: ObjectData): ObjectCreateResponse {
    val name = data.name ?: throw IllegalStateException("name is not defined")
    val subjectId = data.subject.id ?: throw IllegalStateException("subject id is not defined")

    return createObject(ObjectCreateRequest(name, data.description, subjectId, data.subject.version))
}


suspend fun createProperty(request: PropertyCreateRequest): PropertyCreateResponse =
    JSON.parse(post("/api/objects/createProperty", JSON.stringify(request)))

suspend fun createProperty(objectId: String, data: ObjectPropertyData): PropertyCreateResponse {
    val name = data.name
    val aspectId = data.aspect.id ?: throw IllegalStateException("aspect id is not defined")

    return createProperty(PropertyCreateRequest(objectId, name, data.cardinality, aspectId))
}

suspend fun createValue(request: ValueCreateRequest): ValueCreateResponse =
    JSON.parse(post("/api/objects/createValue", JSON.stringify(request.toDTO())))
