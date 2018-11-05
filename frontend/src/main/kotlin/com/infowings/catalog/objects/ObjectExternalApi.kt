package com.infowings.catalog.objects

import com.infowings.catalog.common.*
import com.infowings.catalog.common.guid.BriefObjectView
import com.infowings.catalog.common.guid.BriefObjectViewResponse
import com.infowings.catalog.common.guid.BriefValueViewResponse
import com.infowings.catalog.common.guid.EntityMetadata
import com.infowings.catalog.common.objekt.*
import com.infowings.catalog.utils.delete
import com.infowings.catalog.utils.encodeURIComponent
import com.infowings.catalog.utils.get
import com.infowings.catalog.utils.post
import kotlinx.serialization.json.JSON

suspend fun getAllObjects(): ObjectsResponse = JSON.parse(get("/api/objects"))

suspend fun getDetailedObject(id: String): DetailedObjectViewResponse {
    val data = get("/api/objects/${encodeURIComponent(id)}/viewdetails")
    return JSON.parse(data)
}

suspend fun getDetailedObjectForEdit(id: String): ObjectEditDetailsResponse {
    return JSON.parse(get("/api/objects/${encodeURIComponent(id)}/editdetails"))
}

suspend fun createObject(request: ObjectCreateRequest): ObjectChangeResponse =
    JSON.parse(post("/api/objects/create", JSON.stringify(request)))

suspend fun createProperty(request: PropertyCreateRequest): PropertyCreateResponse =
    JSON.parse(post("/api/objects/createProperty", JSON.stringify(request)))

suspend fun createValue(request: ValueCreateRequest): ValueChangeResponse =
    JSON.parse(post("/api/objects/createValue", JSON.stringify(request.toDTO())))

suspend fun updateObject(request: ObjectUpdateRequest): ObjectChangeResponse =
    JSON.parse(post("/api/objects/update", JSON.stringify(request)))

suspend fun updateProperty(request: PropertyUpdateRequest): PropertyUpdateResponse =
    JSON.parse(post("/api/objects/updateProperty", JSON.stringify(request)))

suspend fun updateValue(request: ValueUpdateRequest): ValueChangeResponse {
    return JSON.parse(post("/api/objects/updateValue", JSON.stringify(request.toDTO())))
}

suspend fun deleteObject(id: String, force: Boolean) {
    delete("/api/objects/object/${encodeURIComponent(id)}?force=$force")
}

suspend fun deleteProperty(id: String, force: Boolean): PropertyDeleteResponse =
    JSON.parse(delete("/api/objects/property/${encodeURIComponent(id)}?force=$force"))

suspend fun deleteValue(id: String, force: Boolean): ValueDeleteResponse =
    JSON.parse(delete("/api/objects/value/${encodeURIComponent(id)}?force=$force"))

suspend fun recalculateValue(fromMeasure: String, toMeasure: String, value: String): ValueRecalculationResponse {
    return JSON.parse(
        get(
            "/api/objects/recalculateValue?from=${encodeURIComponent(fromMeasure)}&to=${encodeURIComponent(toMeasure)}&value=${encodeURIComponent(
                value
            )}"
        )
    )
}

suspend fun loadEntityMetadata(guid: String): EntityMetadata =
    JSON.parse(get("/api/guid/meta/$guid"))

suspend fun getObjectBrief(guid: String): BriefObjectViewResponse =
    JSON.parse(get("/api/guid/brief/object/$guid"))

suspend fun getValueBrief(guid: String): BriefValueViewResponse =
    JSON.parse(get("/api/guid/brief/value/$guid"))

suspend fun LinkValueData.getObjectBriefById(): BriefObjectView {
    val response: BriefObjectViewResponse = JSON.parse(get("/api/guid/brief/object/id/${encodeURIComponent(id)}"))
    return BriefObjectView.of(id, guid, response)
}

suspend fun getValueBriefById(id: String): BriefValueViewResponse =
    JSON.parse(get("/api/guid/brief/value/id/${encodeURIComponent(id)}"))

suspend fun getSuggestedObjects(query: String): ObjectsList {
    return JSON.parse(get("/api/search/object/suggestion?text=$query"))
}
