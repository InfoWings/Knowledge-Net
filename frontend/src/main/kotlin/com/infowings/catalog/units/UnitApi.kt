package com.infowings.catalog.units

import com.infowings.catalog.utils.get

suspend fun filterMeasureNames(filterText: String): Array<String> {
    return JSON.parse(get("/api/search/measure/suggestion?text=$filterText"))
}


