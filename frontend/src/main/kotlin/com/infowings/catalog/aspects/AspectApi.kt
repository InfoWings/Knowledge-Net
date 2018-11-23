package com.infowings.catalog.aspects

import com.infowings.catalog.common.*
import com.infowings.catalog.utils.delete
import com.infowings.catalog.utils.encodeURIComponent
import com.infowings.catalog.utils.get
import com.infowings.catalog.utils.post
import kotlinx.serialization.json.JSON

suspend fun getAllAspects(orderBy: List<AspectOrderBy> = emptyList(), nameQuery: String = ""): AspectsList {
    return JSON.parse(
        get("/api/aspect/all" +
                "?orderFields=${orderBy.map { it.name.toString() }.joinToString { it }}" +
                "&direct=${orderBy.map { it.direction.toString() }.joinToString { it }}" +
                "&q=$nameQuery"
        )
    )
}

suspend fun getAspectTree(id: String): AspectTree = JSON.parse(get("/api/aspect/tree/${encodeURIComponent(id)}"))

suspend fun getAspectById(id: String): AspectData = JSON.parse(get("/api/aspect/id/${encodeURIComponent(id)}"))

suspend fun createAspect(body: AspectData): AspectData = JSON.parse(post("/api/aspect/create", JSON.stringify(body)))

suspend fun updateAspect(body: AspectData): AspectData = JSON.parse(post("/api/aspect/update", JSON.stringify(body)))

suspend fun removeAspect(body: AspectData) = post("/api/aspect/remove", JSON.stringify(body))

suspend fun removeAspectProperty(id: String, force: Boolean = false): AspectPropertyDeleteResponse =
    JSON.parse(delete("/api/aspect/property/${encodeURIComponent(id)}?force=$force"))

suspend fun forceRemoveAspect(body: AspectData) = post("/api/aspect/forceRemove", JSON.stringify(body))

suspend fun getSuggestedAspects(
    query: String,
    aspectId: String? = null,
    aspectPropertyId: String? = null
): AspectsList {
    val aspectIdEncoded = aspectId?.let { encodeURIComponent(it) } ?: ""
    val propertyAspectIdEncoded = aspectPropertyId?.let { encodeURIComponent(it) } ?: ""
    return JSON.parse(get("/api/search/aspect/suggestion?text=$query&aspectId=$aspectIdEncoded&aspectPropertyId=$propertyAspectIdEncoded"))
}

suspend fun getSuggestedMeasureData(query: String, findInGroups: Boolean = false): SuggestedMeasureData =
    JSON.parse(get("/api/search/measure/suggestion?text=$query&findInGroups=$findInGroups"))

suspend fun getHints(query: String): AspectsHints {
    return JSON.parse(get("/api/search/aspect/hint?text=$query"))
}

