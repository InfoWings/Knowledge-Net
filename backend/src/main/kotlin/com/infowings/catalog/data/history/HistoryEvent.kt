package com.infowings.catalog.data.history

import com.fasterxml.jackson.databind.ObjectMapper
import com.infowings.catalog.common.AspectData
import com.infowings.catalog.data.aspect.AspectVertex
import com.infowings.catalog.storage.ASPECT_CLASS
import com.orientechnologies.orient.core.id.ORID


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

    data class Delete(val data: Map<String, String>) : HistoryPayload(EventKind.DELETE) {
        override fun serialize(): String = ObjectMapper().writeValueAsString(data)
    }

    data class SoftDelete(val data: Map<String, String>) : HistoryPayload(EventKind.SOFT_DELETE) {
        override fun serialize(): String = ObjectMapper().writeValueAsString(data)
    }
}

// свойства сущности, хранащиеся в каждой записи лога
data class HistoryKeys(
        val entityId: ORID,
        val entityClass: String,
        val entityName: String
)

data class HistoryEvent(
    val user: String,
    val timestamp: Long,
    val version: Int,
    val keys: HistoryKeys,
    val payload: HistoryPayload
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


fun <T>toDelta(before: T, after: T, asString: (T) -> String): Delta? {
    val strBefore = asString(before)
    val strAfter = asString(after)
    return if (strBefore != strAfter) {
        Delta(strBefore, strAfter)
    } else {
        null
    }
}

fun <T>toPlainPayload(entity: T, asString: List<Pair<String, (T) -> String>>): Map<String, String> {
    return asString.map {
        val value = it.second(entity)
        if (value != null) it.first to value!! else null
    }.filterNotNull().toMap()
}

fun <T>toUpdatePayload(before: T, after: T, asString: List<Pair<String, (T) -> String>>): HistoryPayload.Update {
    val deltas = asString.map {
        val d = toDelta(before, after, it.second)
        if (d != null) it.first to d!! else null
    }.filterNotNull().toMap()

    return HistoryPayload.Update(deltas)
}

private fun <T>asStringOrEmpty(v: T?) = v?.toString().orEmpty()

private val aspectExtractors = listOf<Pair<String, (AspectData) -> String>>(
        Pair("name", {v -> v.name }),
        Pair("measure", {v -> asStringOrEmpty(v.measure)}),
        Pair("baseType", {v -> asStringOrEmpty(v.baseType)})
)

fun AspectData.toCreatePayload(): HistoryPayload.Create =
        HistoryPayload.Create(
                toPlainPayload(this, aspectExtractors)
        )


fun AspectData.toUpdatePayload(previous: AspectData): HistoryPayload.Update {
    return toUpdatePayload(previous, this, aspectExtractors)
}

fun AspectData.toDeletePayload(): HistoryPayload.Delete =
        HistoryPayload.Delete(
                toPlainPayload(this, aspectExtractors)
        )

fun AspectData.toSoftDeletePayload(): HistoryPayload.SoftDelete =
        HistoryPayload.SoftDelete(
                toPlainPayload(this, aspectExtractors)
        )

private fun AspectVertex.toHistoryKeys(): HistoryKeys =
        HistoryKeys(this.identity, ASPECT_CLASS, name)

fun AspectVertex.toHistoryEvent(user: String, payload: HistoryPayload): HistoryEvent =
        HistoryEvent(user, System.currentTimeMillis(), version, toHistoryKeys(), payload)