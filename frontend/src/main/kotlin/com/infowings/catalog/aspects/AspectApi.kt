package com.infowings.catalog.aspects

import com.infowings.catalog.common.*
import com.infowings.catalog.utils.*
import kotlinx.serialization.json.Json

suspend fun getAllAspects(orderBy: List<SortOrder> = emptyList(), nameQuery: String = ""): AspectsList {
    val orderFields = "orderFields=${orderBy.map { it.name.toString() }.joinToString { it }}"
    val direction = "direct=${orderBy.map { it.direction.toString() }.joinToString { it }}"
    val query = if (nameQuery.isNotBlank()) "q=$nameQuery" else null

    val queryString = listOfNotNull(orderFields, direction, query).joinToString("&")
    return try {
        Json.parse(AspectsList.serializer(), get("/api/aspect/all?$queryString"))
    } catch (e: BadRequestException) {
        println(e.message)
        AspectsList(emptyList())
    }
}

suspend fun getAspectTree(id: String): AspectTree = Json.parse(AspectTree.serializer(), get("/api/aspect/tree/${encodeURIComponent(id)}"))

suspend fun getAspectById(id: String): AspectData = Json.parse(AspectData.serializer(), get("/api/aspect/id/${encodeURIComponent(id)}"))

suspend fun createAspect(body: AspectData): AspectData =
    Json.parse(AspectData.serializer(), post("/api/aspect/create", Json.stringify(AspectData.serializer(), body)))

suspend fun updateAspect(body: AspectData): AspectData =
    Json.parse(AspectData.serializer(), post("/api/aspect/update", Json.stringify(AspectData.serializer(), body)))

suspend fun removeAspect(body: AspectData) = post("/api/aspect/remove", Json.stringify(AspectData.serializer(), body))

suspend fun removeAspectProperty(id: String, force: Boolean = false): AspectPropertyDeleteResponse =
    Json.parse(AspectPropertyDeleteResponse.serializer(), delete("/api/aspect/property/${encodeURIComponent(id)}?force=$force"))

suspend fun forceRemoveAspect(body: AspectData) = post("/api/aspect/forceRemove", Json.stringify(AspectData.serializer(), body))

suspend fun getSuggestedAspects(
    query: String,
    aspectId: String? = null,
    aspectPropertyId: String? = null
): AspectsList {
    val aspectIdEncoded = aspectId?.let { encodeURIComponent(it) } ?: ""
    val propertyAspectIdEncoded = aspectPropertyId?.let { encodeURIComponent(it) } ?: ""
    return Json.parse(
        AspectsList.serializer(),
        get("/api/search/aspect/suggestion?text=$query&aspectId=$aspectIdEncoded&aspectPropertyId=$propertyAspectIdEncoded")
    )
}

suspend fun getSuggestedMeasureData(query: String, findInGroups: Boolean = false): SuggestedMeasureData =
    Json.parse(SuggestedMeasureData.serializer(), get("/api/search/measure/suggestion?text=$query&findInGroups=$findInGroups"))

suspend fun getAspectHints(
    query: String,
    aspectId: String? = null,
    aspectPropertyId: String? = null
): AspectsHints {
    val aspectIdEncoded = aspectId?.let { encodeURIComponent(it) } ?: ""
    val propertyAspectIdEncoded = aspectPropertyId?.let { encodeURIComponent(it) } ?: ""
    return try {
        Json.parse(AspectsHints.serializer(), get("/api/search/aspect/hint?text=$query&aspectId=$aspectIdEncoded&aspectPropertyId=$propertyAspectIdEncoded"))
    } catch (e: BadRequestException) {
        AspectsHints.empty()
    }
}

