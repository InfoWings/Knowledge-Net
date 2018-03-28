package com.infowings.catalog.common.history

import kotlinx.serialization.Serializable

enum class EventKind {
    CREATE, UPDATE, SOFT_DELETE, DELETE
}

@Serializable
abstract class HistoryField {
    abstract val name: String
}

@Serializable
data class Delta(
    var field: HistoryField, // null means created
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

