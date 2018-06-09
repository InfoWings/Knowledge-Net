package com.infowings.catalog.data.history

import com.infowings.catalog.auth.user.UserVertex
import com.infowings.catalog.common.*
import com.orientechnologies.orient.core.id.ORID
import com.orientechnologies.orient.core.record.OVertex
import java.util.*

data class DiffPayload(
    val data: Map<String, String>,
    val addedLinks: Map<String, List<ORID>>,
    val removedLinks: Map<String, List<ORID>>
) {
    private fun <T> valueOrEmptyList(map: Map<String, List<T>>, key: String): List<T> = map[key] ?: emptyList()

    private fun <T> singleOf(map: Map<String, List<T>>, key: String): T? {
        val links = valueOrEmptyList(map, key)
        if (links.size > 1) {
            throw IllegalStateException("too many elements for key $key: $links")
        }

        return links.firstOrNull()
    }

    fun addedFor(target: String): List<ORID> = valueOrEmptyList(addedLinks, target)
    fun removedFor(target: String): List<ORID> = valueOrEmptyList(removedLinks, target)

    fun addedSingleFor(target: String): ORID? = singleOf(addedLinks, target)
    fun removedSingleFor(target: String): ORID? = singleOf(removedLinks, target)

    constructor() : this(emptyMap(), emptyMap(), emptyMap())

    fun toData(): DiffPayloadData = DiffPayloadData(data = data,
        addedLinks = addedLinks.mapValues { it.value.map { it.toString() } },
        removedLinks = removedLinks.mapValues { it.value.map { it.toString() } }
    )
}

data class MutableSnapshot(val data: MutableMap<String, String>, val links: MutableMap<String, MutableSet<ORID>>) {
    fun updateField(key: String, value: String) {
        data[key] = value
    }

    fun addLink(target: String, link: ORID) {
        if (target in links) {
            links[target]?.add(link)
        } else {
            links[target] = mutableSetOf(link)
        }
    }

    fun removeLink(target: String, link: ORID) {
        if (target in links) {
            links[target]?.remove(link)
            if (links[target]?.size == 0) links.remove(target)
        } else {
            links[target] = mutableSetOf(link)
        }
    }

    fun toSnapshot() = Snapshot(data.toMap(), links.mapValues { it.value.toList() }.toMap())
}

data class Snapshot(
    val data: Map<String, String>,
    val links: Map<String, List<ORID>>
) {
    constructor() : this(emptyMap(), emptyMap())

    fun toMutable() = MutableSnapshot(data.toMutableMap(), links.mapValues { it.value.toMutableSet() }.toMutableMap())

    fun toSnapshotData() = SnapshotData(data, links.mapValues { it.value.map { it.toString() } })
}

/* Метаданные о событии в жизни сущности. Ккаждый экземпляр соответствует одной высокоуровневой операции
 на уровне сервиса
  Вариант для записи события - некоторые поля представлены как vertex
  */
data class HistoryEventWrite(
    val userVertex: UserVertex,
    val timestamp: Long,
    val version: Int,
    val type: EventType,
    val entityVertex: OVertex,
    val entityClass: String,
    val sessionId: UUID
)

/*
Структура для представления прочитанного события - вместе vertex - данные, извелеченные оттуда
 */
data class HistoryEvent(
    val username: String,
    val timestamp: Long,
    val version: Int,
    val type: EventType,
    val entityId: ORID,
    val entityClass: String,
    val sessionId: UUID
) {
    fun toHistoryEventData() = HistoryEventData(
        username = username, timestamp = timestamp, version = version, type = type,
        entityId = entityId.toString(), entityClass = entityClass, sessionId = sessionId.toString()
    )
}


/* Исторический факт. Состоит из метаданных (event) и данных о внесенных изменениях (payload)
 */
data class HistoryFactWrite(
    val event: HistoryEventWrite,
    val payload: DiffPayload
)

data class HistoryFact(
    val event: HistoryEvent,
    val payload: DiffPayload
)


data class HistorySnapshot(
    val event: HistoryEvent,
    val before: Snapshot,
    val after: Snapshot,
    val diff: DiffPayload
) {
    fun toData(): HistorySnapshotData = HistorySnapshotData(
        event = event.toHistoryEventData(),
        before = before.toSnapshotData(),
        after = after.toSnapshotData(),
        diff = diff.toData()
    )
}

fun toHistoryFact(event: HistoryEventWrite, base: Snapshot, other: Snapshot) =
    HistoryFactWrite(event, diffSnapshots(base, other))

fun diffSnapshots(base: Snapshot, other: Snapshot): DiffPayload {
    // Предполагаем, что поля не выкидываются, но могут добавляться
    // выкинутое поле отследить не сложно, но его надо как-то особо в базе представить.
    // Без явной необходимости не хочется
    val updatedData = other.data
        .filter {
            it.value != base.data[it.key]
        }

    val addedLinks = other.links
        .mapValues {
            it.value - (base.links.getOrElse(it.key, { emptyList() }))
        }
        .filterValues {
            it.isNotEmpty()
        }

    val removedLinks = base.links
        .mapValues {
            it.value - (other.links.getOrElse(it.key, { emptyList() }))
        }
        .filterValues {
            it.isNotEmpty()
        }

    return DiffPayload(updatedData, addedLinks = addedLinks, removedLinks = removedLinks)
}

fun <T> asStringOrEmpty(v: T?) = v?.toString().orEmpty()
