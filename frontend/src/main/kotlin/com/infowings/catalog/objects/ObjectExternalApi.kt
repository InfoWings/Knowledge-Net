package com.infowings.catalog.objects

import com.infowings.catalog.common.*
import com.infowings.catalog.common.guid.BriefObjectView
import com.infowings.catalog.common.guid.BriefObjectViewResponse
import com.infowings.catalog.common.guid.BriefValueViewResponse
import com.infowings.catalog.common.guid.EntityMetadata
import com.infowings.catalog.common.objekt.*
import com.infowings.catalog.utils.*
import kotlinx.serialization.json.Json
import kotlin.js.Date

suspend fun getAllObjects(objectsRequestData: ObjectsRequestData): ObjectsResponse {
    println("params: $objectsRequestData")
    return Json.parse(ObjectsResponse.serializer(), post("/api/objects", Json.stringify(ObjectsRequestData.serializer(), objectsRequestData)))
}

suspend fun getDetailedObject(id: String): DetailedObjectViewResponse {
    val data = get("/api/objects/${encodeURIComponent(id)}/viewdetails")
    return Json.parse(DetailedObjectViewResponse.serializer(), data)
}

suspend fun getDetailedObjectForEdit(id: String): ObjectEditDetailsResponse {
    return Json.parse(ObjectEditDetailsResponse.serializer(), get("/api/objects/${encodeURIComponent(id)}/editdetails"))
}

suspend fun createObject(request: ObjectCreateRequest): ObjectChangeResponse =
    Json.parse(ObjectChangeResponse.serializer(), post("/api/objects/create", Json.stringify(ObjectCreateRequest.serializer(), request)))

suspend fun createProperty(request: PropertyCreateRequest): PropertyCreateResponse =
    Json.parse(PropertyCreateResponse.serializer(), post("/api/objects/createProperty", Json.stringify(PropertyCreateRequest.serializer(), request)))

suspend fun createValue(request: ValueCreateRequest): ValueChangeResponse =
    Json.parse(ValueChangeResponse.serializer(), post("/api/objects/createValue", Json.stringify(ValueCreateRequestDTO.serializer(), request.toDTO())))

suspend fun updateObject(request: ObjectUpdateRequest): ObjectChangeResponse =
    Json.parse(ObjectChangeResponse.serializer(), post("/api/objects/update", Json.stringify(ObjectUpdateRequest.serializer(), request)))

suspend fun updateProperty(request: PropertyUpdateRequest): PropertyUpdateResponse =
    Json.parse(PropertyUpdateResponse.serializer(), post("/api/objects/updateProperty", Json.stringify(PropertyUpdateRequest.serializer(), request)))

suspend fun updateValue(request: ValueUpdateRequest): ValueChangeResponse {
    return Json.parse(ValueChangeResponse.serializer(), post("/api/objects/updateValue", Json.stringify(ValueUpdateRequestDTO.serializer(), request.toDTO())))
}

suspend fun deleteObject(id: String, force: Boolean) {
    delete("/api/objects/object/${encodeURIComponent(id)}?force=$force")
}

suspend fun deleteProperty(id: String, force: Boolean): PropertyDeleteResponse =
    Json.parse(PropertyDeleteResponse.serializer(), delete("/api/objects/property/${encodeURIComponent(id)}?force=$force"))

suspend fun deleteValue(id: String, force: Boolean): ValueDeleteResponse =
    Json.parse(ValueDeleteResponse.serializer(), delete("/api/objects/value/${encodeURIComponent(id)}?force=$force"))

suspend fun recalculateValue(fromMeasure: String, toMeasure: String, value: String): ValueRecalculationResponse {
    return Json.parse(
        ValueRecalculationResponse.serializer(),
        get(
            "/api/objects/recalculateValue?from=${encodeURIComponent(fromMeasure)}&to=${encodeURIComponent(toMeasure)}&value=${encodeURIComponent(
                value
            )}"
        )
    )
}

suspend fun loadEntityMetadata(guid: String): EntityMetadata {
    if (guid.contains("/") || guid.startsWith("http") || guid.length > 60) {
        throw BadRequestException("strange guid: ${guid.take(60)}", Date.now())
    }
    return Json.parse(EntityMetadata.serializer(), get("/api/guid/meta/${encodeURIComponent(guid)}"))
}

suspend fun getObjectBrief(guid: String): BriefObjectViewResponse =
    Json.parse(BriefObjectViewResponse.serializer(), get("/api/guid/brief/object/$guid"))

suspend fun getValueBrief(guid: String): BriefValueViewResponse =
    Json.parse(BriefValueViewResponse.serializer(), get("/api/guid/brief/value/$guid"))

suspend fun LinkValueData.getObjectBriefById(): BriefObjectView {
    val response = Json.parse(BriefObjectViewResponse.serializer(), get("/api/guid/brief/object/id/${encodeURIComponent(id)}"))
    return BriefObjectView.of(id, guid, response)
}

suspend fun getValueBriefById(id: String): BriefValueViewResponse =
    Json.parse(BriefValueViewResponse.serializer(), get("/api/guid/brief/value/id/${encodeURIComponent(id)}"))

suspend fun getSuggestedObjects(query: String): ObjectsList {
    return Json.parse(ObjectsList.serializer(), get("/api/search/object/suggestion?text=$query"))
}
