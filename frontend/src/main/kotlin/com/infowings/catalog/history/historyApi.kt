package com.infowings.catalog.history

import com.infowings.catalog.common.*
import com.infowings.catalog.utils.get
import kotlinx.serialization.json.JSON

suspend fun getAllEvents(): List<HistoryData<*>> {
    val aspectEvents = getAllAspectEvents()
    val refBookEvents = getAllRefBookEvents()
    val subjectEvents = getAllSubjectEvents()

    /* Здесь нам надо смреджить два сортированных списка. Полагаемся на то, что используется реализация merge sort,
     * умеющая распознавать отсортированные подсписки. Кажется в JDK так и есть, а котлиновские коллекции - обертки
      * над JDK. Если это вдруг не так, поменяем */
    return (aspectEvents + subjectEvents + refBookEvents).sortedBy { -it.event.timestamp }
}

suspend fun getAllAspectEvents(): List<AspectHistory> =
    JSON.nonstrict.parse<AspectHistoryList>(get("/api/history/aspects")).history

suspend fun getAllRefBookEvents(): List<RefBookHistory> =
    JSON.nonstrict.parse<RefBookHistoryList>(get("/api/history/refbook")).history

suspend fun getAllSubjectEvents(): List<SubjectHistory> =
    JSON.nonstrict.parse<SubjectHistoryList>(get("/api/history/subjects")).history
