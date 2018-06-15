package com.infowings.catalog.history

import com.infowings.catalog.common.*
import com.infowings.catalog.utils.get
import kotlinx.serialization.json.JSON

suspend fun getAllEvents(): List<HistoryData<*>> {
    val aspectEvents = getAllAspectEvents()
    val refBookEvents = getAllRefBookEvents()
    val objectEvents = getAllObjectEvents()

    return (aspectEvents + refBookEvents + objectEvents).sortedByDescending { it.timestamp }
}

suspend fun getAllAspectEvents(): List<AspectHistory> =
    JSON.nonstrict.parse<AspectHistoryList>(get("/api/history/aspects")).history

suspend fun getAllRefBookEvents(): List<RefBookHistory> =
    JSON.nonstrict.parse<RefBookHistoryList>(get("/api/history/refbook")).history

suspend fun getAllObjectEvents(): List<ObjectHistory> =
    JSON.nonstrict.parse<ObjectHistoryList>(get("/api/history/objects")).history
