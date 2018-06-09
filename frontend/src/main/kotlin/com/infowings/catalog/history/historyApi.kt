package com.infowings.catalog.history

import com.infowings.catalog.common.*
import com.infowings.catalog.utils.get
import kotlinx.serialization.json.JSON

suspend fun getAllEvents(): List<HistoryData<*>> {
    val subjectEvents = getAllSubjectEvents()
    val aspectEvents = getAllAspectEvents()

    /* Здесь нам надо смреджить два сортированных списка. Полагаемся на то, что используется реализация merge sort,
     * умеющая распознавать отсортированные подсписки. Кажется в JDK так и есть, а котлиновские коллекции - обертки
      * над JDK. Если это вдруг не так, поменяем */
    return (aspectEvents + subjectEvents).sortedBy { -it.event.timestamp }
}

suspend fun getAllAspectEvents(): List<AspectHistory> =
    JSON.nonstrict.parse<AspectHistoryList>(get("/api/history/aspects")).history

suspend fun getAllSubjectEvents(): List<SubjectHistory> =
    JSON.nonstrict.parse<SubjectHistoryList>(get("/api/history/subjects")).history
