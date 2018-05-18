package com.infowings.catalog.data.history.providers

import com.infowings.catalog.common.*
import com.infowings.catalog.data.aspect.OpenDomain
import com.infowings.catalog.data.history.HistoryFactDto

fun AspectData.submit(fact: HistoryFactDto): AspectData {
    return when (fact.event.type) {
        EventType.CREATE, EventType.UPDATE -> {
            val baseTypeObj = baseType?.let { BaseType.restoreBaseType(it) }
            copy(
                measure = fact.payload.data.getOrDefault(AspectField.MEASURE.name, measure),
                baseType = fact.payload.data.getOrDefault(AspectField.BASE_TYPE.name, baseType),
                name = fact.payload.data.getOrDefault(AspectField.NAME.name, name),
                domain = baseTypeObj?.let { OpenDomain(it).toString() },
                version = fact.event.version
            )
        }
        else -> copy(deleted = true, version = fact.event.version)
    }
}

fun List<AspectPropertyData>.submit(events: List<HistoryFactDto>?): List<AspectPropertyData> {

    if (events == null) {
        return map { it.copy() }
    }

    val updatedProps = map { aspectPropertyData ->
        val relatedEvents = events.filter { it.event.entityId.toString() == aspectPropertyData.id }.sortedBy { it.event.timestamp }
        var initial = aspectPropertyData
        relatedEvents.forEach {
            initial = aspectPropertyData.submit(it)
        }
        return@map initial
    }

    return updatedProps.filterNot { it.deleted }
}

fun AspectPropertyData.submit(fact: HistoryFactDto): AspectPropertyData {
    return when (fact.event.type) {
        EventType.CREATE, EventType.UPDATE -> copy(
            name = fact.payload.data.getOrDefault(AspectPropertyField.NAME.name, name),
            cardinality = fact.payload.data.getOrDefault(AspectPropertyField.CARDINALITY.name, cardinality),
            aspectId = fact.payload.data.getOrDefault(AspectPropertyField.ASPECT.name, aspectId),
            version = fact.event.version
        )
        else -> copy(deleted = true, version = fact.event.version)
    }
}