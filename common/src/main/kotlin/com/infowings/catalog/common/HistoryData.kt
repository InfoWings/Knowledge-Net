package com.infowings.catalog.common

import kotlinx.serialization.Serializable

enum class EventKind {
    CREATE, UPDATE, SOFT_DELETE, DELETE
}

@Serializable
class Delta(
    var field: String, // null means created
    var before: String?,
    var after: String? // null means deleted
)

@Serializable
class HistoryData<T>(
    var user: String,
    var event: EventKind,
    var entityName: String,
    var info: String,
    var deleted: Boolean,
    var timestamp: Long,
    var version: Int,
    var fullData: T,
    var changes: List<Delta>
)

typealias AspectHistory = HistoryData<AspectData>

@Serializable
class AspectHistoryList(val history: List<AspectHistory>)

fun createDeltaFromProperty(propertyNumber: Int, before: String?, after: String?) =
    Delta("property[$propertyNumber]", before, after)