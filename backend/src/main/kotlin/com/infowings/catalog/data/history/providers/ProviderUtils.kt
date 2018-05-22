package com.infowings.catalog.data.history.providers

import com.infowings.catalog.common.Delta
import com.infowings.catalog.common.EventType
import com.infowings.catalog.data.history.HistoryFactDto
import com.orientechnologies.orient.core.id.ORID


fun Set<HistoryFactDto>.idEventMap(classname: String): Map<ORID, List<HistoryFactDto>> = this
    .filter { it.event.entityClass == classname }
    .groupBy { it.event.entityId }
    .map { (id, events) -> id to events.sortedBy { it.event.timestamp } }
    .toMap()


fun createAspectFieldDelta(event: EventType, field: String, before: String?, after: String?): Delta =
    when (event) {
        EventType.CREATE, EventType.UPDATE -> Delta(field, before, after)
        EventType.DELETE, EventType.SOFT_DELETE -> Delta(field, before, null)
    }