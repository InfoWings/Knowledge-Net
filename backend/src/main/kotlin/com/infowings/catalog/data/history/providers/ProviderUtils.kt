package com.infowings.catalog.data.history.providers

import com.infowings.catalog.common.EventType
import com.infowings.catalog.common.FieldDelta
import com.infowings.catalog.data.history.HistoryFact


fun Set<HistoryFact>.idEventMap(classname: String): Map<String, List<HistoryFact>> = this
    .filter { it.event.entityClass == classname }
    .groupBy { it.event.entityId }
    .mapValues { (_, events) -> events.sortedBy { it.event.timestamp } }


fun createAspectFieldDelta(event: EventType, field: String, before: String?, after: String?): FieldDelta =
    when (event) {
        EventType.CREATE, EventType.UPDATE -> FieldDelta(field, before, after)
        EventType.DELETE, EventType.SOFT_DELETE -> FieldDelta(field, before, null)
    }