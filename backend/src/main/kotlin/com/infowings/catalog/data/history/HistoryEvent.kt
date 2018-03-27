package com.infowings.catalog.data.history

import com.fasterxml.jackson.databind.ObjectMapper
import com.infowings.catalog.data.aspect.AspectVertex
import com.infowings.catalog.storage.ASPECT_CLASS
import com.infowings.catalog.storage.id
import com.orientechnologies.orient.core.record.OVertex


data class Delta (
    val before: String,
    val after: String
)

sealed class HistoryPayload(val event: EventKind) {
    abstract fun serialize(): String

    data class Create(val data: Map<String, String>) : HistoryPayload(EventKind.CREATE) {
        override fun serialize(): String = ObjectMapper().writeValueAsString(data)
    }

    data class Update(val data: Map<String, Delta>) : HistoryPayload(EventKind.UPDATE) {
        override fun serialize(): String = ObjectMapper().writeValueAsString(data)
    }
}

// свойства сущности, хранащиеся в каждой записи лога
data class HistoryKeys(
        val entityId: String,
        val vertex: OVertex,
        val entityClass: String,
        val entityName: String
)

data class HistoryEvent(
    val user: String,
    val timestamp: Long,
    val keys: HistoryKeys,
    val payload: HistoryPayload
)

fun AspectVertex.toCreatePayload(): HistoryPayload.Create =
        HistoryPayload.Create(
                listOfNotNull(
                        "name" to name,
                        if (measure != null) ("measure" to measure.toString()) else null,
                        if (baseType != null) ("baseType" to baseType?.toString()!!) else null
                ).toMap()
        )

/*
  name идет и в payload, и в entity
  В payload, потому name может меняться
  D keys, потому что это все-таки устойчивый аттрибут сущности, по которому скорее всего
  захочется искать, поэтому прятать его в payload не очень хорошо

  Собственно, если возникнет желание эффективно искать факты изменения отдельных аттрибутов
  ("кто и когда менял measure у каких бы то ни было аспектов"), то такие атрибуты то же надо будет выносить
  в keys
 */

private fun AspectVertex.toHistoryKeys(): HistoryKeys =
        HistoryKeys(id, this, ASPECT_CLASS, name)

fun AspectVertex.toHistoryEvent(user: String, payload: HistoryPayload): HistoryEvent =
        HistoryEvent(user, System.currentTimeMillis(), toHistoryKeys(), payload)
