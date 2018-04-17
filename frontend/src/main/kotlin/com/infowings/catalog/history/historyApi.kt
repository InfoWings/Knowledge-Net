package com.infowings.catalog.history

import com.infowings.catalog.common.AspectHistory
import com.infowings.catalog.common.AspectHistoryList
import com.infowings.catalog.common.HistoryData
import com.infowings.catalog.utils.get
import kotlinx.serialization.json.JSON

suspend fun getAllEvents(): List<HistoryData<*>> = getAllAspectEvents()

suspend fun getAllAspectEvents(): List<AspectHistory> =
    JSON.nonstrict.parse<AspectHistoryList>(get("/api/history/aspects")).history
