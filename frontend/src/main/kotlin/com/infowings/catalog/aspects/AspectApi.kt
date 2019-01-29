package com.infowings.catalog.aspects

import com.infowings.catalog.common.*
import com.infowings.catalog.utils.delete
import com.infowings.catalog.utils.encodeURIComponent
import com.infowings.catalog.utils.get
import com.infowings.catalog.utils.post
import kotlinx.serialization.json.JSON

suspend fun getAllAspects(orderBy: List<SortOrder> = emptyList(), nameQuery: String = ""): AspectsList {
    val orderFields = "orderFields=${orderBy.map { it.name.toString() }.joinToString { it }}"
    val direction = "direct=${orderBy.map { it.direction.toString() }.joinToString { it }}"
    val query = if (nameQuery.isBlank()) "q=$nameQuery" else null

    val queryString = listOfNotNull(orderFields, direction, query).joinToString("&")

    return JSON.parse(AspectsList.serializer(), get("/api/aspect/all?$queryString"))
}

suspend fun getAspectTree(id: String): AspectTree = JSON.parse(AspectTree.serializer(), get("/api/aspect/tree/${encodeURIComponent(id)}"))

suspend fun getAspectById(id: String): AspectData = JSON.parse(AspectData.serializer(), get("/api/aspect/id/${encodeURIComponent(id)}"))

suspend fun createAspect(body: AspectData): AspectData =
    JSON.parse(AspectData.serializer(), post("/api/aspect/create", JSON.stringify(AspectData.serializer(), body)))

suspend fun updateAspect(body: AspectData): AspectData =
    JSON.parse(AspectData.serializer(), post("/api/aspect/update", JSON.stringify(AspectData.serializer(), body)))

suspend fun removeAspect(body: AspectData) = post("/api/aspect/remove", JSON.stringify(AspectData.serializer(), body))

suspend fun removeAspectProperty(id: String, force: Boolean = false): AspectPropertyDeleteResponse =
    JSON.parse(AspectPropertyDeleteResponse.serializer(), delete("/api/aspect/property/${encodeURIComponent(id)}?force=$force"))

suspend fun forceRemoveAspect(body: AspectData) = post("/api/aspect/forceRemove", JSON.stringify(AspectData.serializer(), body))

suspend fun getSuggestedAspects(
    query: String,
    aspectId: String? = null,
    aspectPropertyId: String? = null
): AspectsList {
    val aspectIdEncoded = aspectId?.let { encodeURIComponent(it) } ?: ""
    val propertyAspectIdEncoded = aspectPropertyId?.let { encodeURIComponent(it) } ?: ""
    return JSON.parse(
        AspectsList.serializer(),
        get("/api/search/aspect/suggestion?text=$query&aspectId=$aspectIdEncoded&aspectPropertyId=$propertyAspectIdEncoded")
    )
}

suspend fun getSuggestedMeasureData(query: String, findInGroups: Boolean = false): SuggestedMeasureData =
    JSON.parse(SuggestedMeasureData.serializer(), get("/api/search/measure/suggestion?text=$query&findInGroups=$findInGroups"))

suspend fun getAspectHints(
    query: String,
    aspectId: String? = null,
    aspectPropertyId: String? = null
): AspectsHints {
    val aspectIdEncoded = aspectId?.let { encodeURIComponent(it) } ?: ""
    val propertyAspectIdEncoded = aspectPropertyId?.let { encodeURIComponent(it) } ?: ""
    return JSON.parse(AspectsHints.serializer(), get("/api/search/aspect/hint?text=$query&aspectId=$aspectIdEncoded&aspectPropertyId=$propertyAspectIdEncoded"))
}

