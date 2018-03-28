package com.infowings.catalog.common

import kotlinx.serialization.Serializable

enum class EventKind {
    CREATE, UPDATE, SOFT_DELETE, DELETE
}

@Serializable
data class Delta(
    var field: String, // null means created
    var before: String?,
    var after: String? // null means deleted
)

@Serializable
data class HistoryData<T>(
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

typealias AspectHistory = HistoryData<AspectDataView>

@Serializable
data class AspectDataView(val aspectData: AspectData, val related: List<AspectData>)

enum class AspectField {
    NAME, MEASURE, BASE_TYPE;
}

@Serializable
class AspectHistoryList(val history: List<AspectHistory>)
