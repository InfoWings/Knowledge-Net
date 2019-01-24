package com.infowings.catalog.objects

import com.infowings.catalog.common.*
import com.infowings.catalog.common.guid.BriefObjectView
import com.infowings.catalog.common.guid.BriefObjectViewResponse
import com.infowings.catalog.common.guid.BriefValueViewResponse
import com.infowings.catalog.common.guid.EntityMetadata
import com.infowings.catalog.common.objekt.*
import com.infowings.catalog.utils.*
import kotlinx.serialization.json.JSON
import kotlin.js.Date

suspend fun getAllObjects(orderBy: List<SortOrder>, query: String?, offset: Int? = null, limit: Int? = null): ObjectsResponse {
    val fields = orderBy.map { it.name }.joinToString(",")
    val directions = orderBy.map { it.direction }.joinToString(",")
    val limitPart = if (limit != null && limit > 0) "limit=$limit" else null
    val offsetPart = if (offset != null && offset > 0) "offset=$offset" else null
    val params = listOfNotNull(limitPart, offsetPart, "orderFields=$fields", "direct=$directions", "q=${query ?: ""}").joinToString("&")
    println("params: $params")
    return JSON.parse(ObjectsResponse.serializer(), get("/api/objects?$params"))

    //return JSON.parse(get("/api/objects?orderFields=$fields&direct=$directions&q=${query?:""}"))
}

suspend fun getDetailedObject(id: String): DetailedObjectViewResponse {
    val data = get("/api/objects/${encodeURIComponent(id)}/viewdetails")
    return JSON.parse(DetailedObjectViewResponse.serializer(), data)
}

suspend fun getDetailedObjectForEdit(id: String): ObjectEditDetailsResponse {
    return JSON.parse(ObjectEditDetailsResponse.serializer(), get("/api/objects/${encodeURIComponent(id)}/editdetails"))
}

suspend fun createObject(request: ObjectCreateRequest): ObjectChangeResponse =
    JSON.parse(ObjectChangeResponse.serializer(), post("/api/objects/create", JSON.stringify(ObjectCreateRequest.serializer(), request)))

suspend fun createProperty(request: PropertyCreateRequest): PropertyCreateResponse =
    JSON.parse(PropertyCreateResponse.serializer(), post("/api/objects/createProperty", JSON.stringify(PropertyCreateRequest.serializer(), request)))

suspend fun createValue(request: ValueCreateRequest): ValueChangeResponse =
    JSON.parse(ValueChangeResponse.serializer(), post("/api/objects/createValue", JSON.stringify(ValueCreateRequestDTO.serializer(), request.toDTO())))

suspend fun updateObject(request: ObjectUpdateRequest): ObjectChangeResponse =
    JSON.parse(ObjectChangeResponse.serializer(), post("/api/objects/update", JSON.stringify(ObjectUpdateRequest.serializer(), request)))

suspend fun updateProperty(request: PropertyUpdateRequest): PropertyUpdateResponse =
    JSON.parse(PropertyUpdateResponse.serializer(), post("/api/objects/updateProperty", JSON.stringify(PropertyUpdateRequest.serializer(), request)))

suspend fun updateValue(request: ValueUpdateRequest): ValueChangeResponse {
    return JSON.parse(ValueChangeResponse.serializer(), post("/api/objects/updateValue", JSON.stringify(ValueUpdateRequestDTO.serializer(), request.toDTO())))
}

suspend fun deleteObject(id: String, force: Boolean) {
    delete("/api/objects/object/${encodeURIComponent(id)}?force=$force")
}

suspend fun deleteProperty(id: String, force: Boolean): PropertyDeleteResponse =
    JSON.parse(PropertyDeleteResponse.serializer(), delete("/api/objects/property/${encodeURIComponent(id)}?force=$force"))

suspend fun deleteValue(id: String, force: Boolean): ValueDeleteResponse =
    JSON.parse(ValueDeleteResponse.serializer(), delete("/api/objects/value/${encodeURIComponent(id)}?force=$force"))

suspend fun recalculateValue(fromMeasure: String, toMeasure: String, value: String): ValueRecalculationResponse {
    return JSON.parse(
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
    return JSON.parse(EntityMetadata.serializer(), get("/api/guid/meta/${encodeURIComponent(guid)}"))
}

suspend fun getObjectBrief(guid: String): BriefObjectViewResponse =
    JSON.parse(BriefObjectViewResponse.serializer(), get("/api/guid/brief/object/$guid"))

suspend fun getValueBrief(guid: String): BriefValueViewResponse =
    JSON.parse(BriefValueViewResponse.serializer(), get("/api/guid/brief/value/$guid"))

suspend fun LinkValueData.getObjectBriefById(): BriefObjectView {
    val response = JSON.parse(BriefObjectViewResponse.serializer(), get("/api/guid/brief/object/id/${encodeURIComponent(id)}"))
    return BriefObjectView.of(id, guid, response)
}

suspend fun getValueBriefById(id: String): BriefValueViewResponse =
    JSON.parse(BriefValueViewResponse.serializer(), get("/api/guid/brief/value/id/${encodeURIComponent(id)}"))

suspend fun getSuggestedObjects(query: String): ObjectsList {
    return JSON.parse(ObjectsList.serializer(), get("/api/search/object/suggestion?text=$query"))
}
