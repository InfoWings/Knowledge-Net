package com.infowings.catalog.history

import com.infowings.catalog.common.*
import com.infowings.catalog.utils.get
import kotlinx.serialization.json.JSON

suspend fun getAllEvents(): List<HistoryData<*>> {
    val aspectEvents = getAllAspectEvents()
    val refBookEvents = getAllRefBookEvents()
    val objectEvents = getAllObjectEvents()
    val subjectEvents = getAllSubjectEvents()


    /* Здесь нам надо смреджить два сортированных списка. Полагаемся на то, что используется реализация merge sort,
     * умеющая распознавать отсортированные подсписки. Кажется в JDK так и есть, а котлиновские коллекции - обертки
      * над JDK. Если это вдруг не так, поменяем */
    return (aspectEvents + refBookEvents + objectEvents + subjectEvents).sortedByDescending { it.event.timestamp }
}

suspend fun getAllAspectEvents(): List<AspectHistory> =
    JSON.nonstrict.parse(AspectHistoryList.serializer(), get("/api/history/aspects")).history

suspend fun getAllRefBookEvents(): List<RefBookHistory> =
    JSON.nonstrict.parse(RefBookHistoryList.serializer(), get("/api/history/refbook")).history

suspend fun getAllObjectEvents(): List<ObjectHistory> =
    JSON.nonstrict.parse(ObjectHistoryList.serializer(), get("/api/history/objects")).history

suspend fun getAllSubjectEvents(): List<SubjectHistory> =
    JSON.nonstrict.parse(SubjectHistoryList.serializer(), get("/api/history/subjects")).history
