package com.infowings.catalog.data.history

import com.infowings.catalog.auth.user.UserVertex
import com.infowings.catalog.common.EventType
import com.infowings.catalog.common.SnapshotData
import com.infowings.catalog.common.history.refbook.RefBookHistoryData
import com.infowings.catalog.common.Range
import com.infowings.catalog.common.history.objekt.ObjectHistoryData
import com.orientechnologies.orient.core.id.ORID

data class DiffPayload(
    val data: Map<String, String>,
    val addedLinks: Map<String, List<ORID>>,
    val removedLinks: Map<String, List<ORID>>
) {
    fun addedFor(vertexClass: String): List<ORID> = addedLinks[vertexClass] ?: emptyList()
    fun removedFor(vertexClass: String): List<ORID> = removedLinks[vertexClass] ?: emptyList()
}

data class MutableSnapshot(val data: MutableMap<String, String>, val links: MutableMap<String, MutableSet<ORID>>) {
    fun apply(diff: DiffPayload) {
        diff.data.forEach { updateField(it.key, it.value) }
        diff.addedLinks.forEach { addLinks(it.key, it.value) }
        diff.removedLinks.forEach { removeLinks(it.key, it.value) }
    }

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

    fun addLinks(target: String, toAdd: List<ORID>) {
        links.computeIfAbsent(target, { mutableSetOf() }).addAll(toAdd)
    }

    fun removeLinks(target: String, toRemove: List<ORID>) {
        if (target in links) {
            links[target]?.removeAll(toRemove)
            if (links[target]?.size == 0) links.remove(target)
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


data class HistoryEvent(
    val username: String,
    val timestamp: Long,
    val version: Int,
    val type: EventType,
    val entityId: ORID,
    val entityClass: String
)

data class HistoryFact(
    val userVertex: UserVertex,
    val event: HistoryEvent,
    val payload: DiffPayload,
    val subject: HistoryAware
)

fun toHistoryFact(userVertex: UserVertex, event: HistoryEvent, subject: HistoryAware, base: Snapshot, other: Snapshot) =
    HistoryFact(userVertex, event, diffSnapshots(base, other), subject)

fun diffSnapshots(base: Snapshot, other: Snapshot): DiffPayload {
    // Предполагаем, что поля не выкидываются, но могут добавляться
    // выкинутое поле отследить не сложно, но его надо как-то особо в базе представить.
    // Без явной необходимости не хочется

    val updatedData = other.data
        .filter {
            it.value != base.data[it.key]
        }

    val addedLinks = other.links
        .mapValues { (linkType, links) ->
            links - (base.links.getOrElse(linkType, { emptyList() }))
        }.filterValues {
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

class RefBookHistoryInfo {
    companion object {
        data class Header(
            val id: String,
            val snapshot: MutableSnapshot,
            val aspectName: String
        ) {
            fun toData() = RefBookHistoryData.Companion.Header(
                id = id,
                name = snapshot.data.getValue("value"),
                description = snapshot.data["description"],
                aspectId = snapshot.links.getValue("aspect").first().toString(),
                aspectName = aspectName
            )

        }

        data class Item(val id: String, val snapshot: MutableSnapshot) {
            fun toData() = RefBookHistoryData.Companion.Item(
                id = id,
                name = snapshot.data.getValue("value"),
                description = snapshot.data["description"]
            )
        }

        data class BriefState(val header: Header, val item: Item?) {
            fun toData() = RefBookHistoryData.Companion.BriefState(header.toData(), item?.toData())
        }
    }
}

class ObjectHistoryInfo {
    companion object {
        data class Objekt(
            val id: String,
            val snapshot: MutableSnapshot,
            val subjectName: String
        ) {
            fun toData() = ObjectHistoryData.Companion.Objekt(
                id = id,
                name = snapshot.data.getValue("name"),
                description = snapshot.data["description"],
                subjectId = snapshot.links.getValue("subject").first().toString(),
                subjectName = subjectName
            )

        }

        data class Property(val id: String, val snapshot: MutableSnapshot, val aspectName: String) {
            fun toData() = ObjectHistoryData.Companion.Property(
                id = id,
                name = snapshot.data.getValue("name"),
                aspectId = snapshot.links.getValue("aspect").first().toString(),
                aspectName = aspectName
            )
        }

        data class Value(val id: String, val snapshot: MutableSnapshot, val aspectPropertyName: String?) {
            fun toData(): ObjectHistoryData.Companion.Value {
                val name = snapshot.data["name"]
                return ObjectHistoryData.Companion.Value(
                    id = id,
                    aspectPropertyId = snapshot.links["aspectProperty"]?.first()?.toString(),
                    aspectPropertyName = aspectPropertyName
                )
            }
        }

        data class BriefState(val objekt: Objekt, val property: Property?, val value: Value?) {
            fun toData() = ObjectHistoryData.Companion.BriefState(objekt.toData(), property?.toData(), value?.toData())
        }
    }
}

fun Range.asString() = "$left:$right"
