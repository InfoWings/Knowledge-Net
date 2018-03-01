package com.infowings.catalog.units

import com.infowings.catalog.common.MeasureNames
import com.infowings.catalog.utils.get
import kotlinx.serialization.json.JSON

suspend fun filterMeasureNames(filterText: String): List<String> {
    return JSON.parse<MeasureNames>(get("/api/search/measure/suggestion?text=$filterText")).names
}


