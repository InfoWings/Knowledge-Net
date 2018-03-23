package com.infowings.catalog.aspects

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectsList
import com.infowings.catalog.utils.encodeURIComponent
import com.infowings.catalog.utils.get
import com.infowings.catalog.utils.post
import kotlinx.serialization.json.JSON

suspend fun getAllAspects(): AspectsList = JSON.parse(get("/api/aspect/all"))

suspend fun createAspect(body: AspectData): AspectData = JSON.parse(post("/api/aspect/create", JSON.stringify(body)))

suspend fun updateAspect(body: AspectData): AspectData = JSON.parse(post("/api/aspect/update", JSON.stringify(body)))

suspend fun getSuggestedAspects(query: String, aspectId: String?, aspectPropertyId: String?): AspectsList {
    val aspectIdEncoded = aspectId?.let { encodeURIComponent(it) } ?: ""
    val propertyAspectIdEncoded = aspectPropertyId?.let { encodeURIComponent(it) } ?: ""
    return JSON.parse(get("/api/search/aspect/suggestion?text=$query&aspectId=$aspectIdEncoded&aspectPropertyId=$propertyAspectIdEncoded"))
}

suspend fun getSuggestedMeasurementUnits(query: String, findInGroups: Boolean = false): Array<String> =
    kotlin.js.JSON.parse(get("/api/search/measure/suggestion?text=$query&findInGroups=$findInGroups"))
