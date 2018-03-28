package com.infowings.catalog.history

import com.infowings.catalog.common.history.AspectHistory
import com.infowings.catalog.common.history.AspectHistoryList
import com.infowings.catalog.common.history.HistoryData
import com.infowings.catalog.utils.get
import kotlinx.serialization.json.JSON

suspend fun getAllEvents(): List<HistoryData<*>> = getAllAspectEvents()

suspend fun getAllAspectEvents(): List<AspectHistory> =
    JSON.parse<AspectHistoryList>(get("/api/history/aspects")).history
