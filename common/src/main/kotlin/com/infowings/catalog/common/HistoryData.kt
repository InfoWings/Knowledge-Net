package com.infowings.catalog.common

import kotlinx.serialization.Serializable

enum class EventKind {
    CREATE, UPDATE, SOFT_DELETE, DELETE
}

@Serializable
class Delta(var before: String?, var after: String?) // after in null means deleted, before is null means created

@Serializable
class HistoryData<T>(
    var user: String,
    var event: EventKind,
    var entityName: String,
    var publicName: String, // for user identifying
    var deleted: Boolean,
    var timestamp: Long,
    var version: Int,
    var fullData: T,
    var changes: List<Delta>
)

