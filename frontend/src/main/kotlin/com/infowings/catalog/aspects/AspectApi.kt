package com.infowings.catalog.aspects

import com.infowings.catalog.common.AspectData
import com.infowings.catalog.common.AspectsList
import com.infowings.catalog.utils.get
import com.infowings.catalog.utils.post
import kotlinx.serialization.json.JSON

suspend fun getAllAspects(): AspectsList = JSON.parse(get("/api/aspect/all"))

suspend fun createAspect(body: AspectData): AspectData {
    console.log(JSON.stringify(body))

    return JSON.parse(post("/api/aspect/create", JSON.stringify(body)))
}
