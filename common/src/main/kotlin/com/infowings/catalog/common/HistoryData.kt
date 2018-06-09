package com.infowings.catalog.common

import kotlinx.serialization.Serializable

enum class EventType {
    CREATE, UPDATE, SOFT_DELETE, DELETE;

    fun isDelete(): Boolean = this == DELETE || this == SOFT_DELETE
}

@Serializable
data class FieldDelta(
    var fieldName: String,
    var before: String?, // null means created
    var after: String? // null means deleted
)

@Serializable
data class SnapshotData(val data: Map<String, String>, val links: Map<String, List<String>>)

/*
Структура для предствления прочитанного события, пригодная для контекста KotlinJS
 */
@Serializable
data class HistoryEventData(
    val username: String,
    val timestamp: Long,
    val version: Int,
    val type: EventType,
    val entityId: String,
    val entityClass: String,
    val sessionId: String
)


@Serializable
data class HistoryData<T>(
    val event: HistoryEventData,
    var info: String?,
    var deleted: Boolean,
    var fullData: T,
    var changes: List<FieldDelta>
)

typealias AspectHistory = HistoryData<AspectDataView>
typealias SubjectHistory = HistoryData<SnapshotData>

@Serializable
data class AspectDataView(val aspectData: AspectData, val related: List<AspectData>)


enum class AspectField {
    NAME {
        override val view: String
            get() = "Name"
    },
    MEASURE {
        override val view: String
            get() = "Measure"
    },
    BASE_TYPE {
        override val view: String
            get() = "Base type"
    },
    DESCRIPTION {
        override val view: String
            get() = "Description"
    };

    abstract val view: String

    companion object {
        const val PROPERTY = "Property"
        const val SUBJECT = "Subject"
        const val REFERENCE_BOOK = "Reference book"
    }
}

enum class AspectPropertyField {
    NAME, CARDINALITY, ASPECT, DESCRIPTION;
}

@Serializable
data class DiffPayloadData(
    val data: Map<String, String>,
    val addedLinks: Map<String, List<String>>,
    val removedLinks: Map<String, List<String>>
)

@Serializable
data class HistorySnapshotData(
    val event: HistoryEventData,
    val before: SnapshotData,
    val after: SnapshotData,
    val diff: DiffPayloadData
)


@Serializable
class AspectHistoryList(val history: List<AspectHistory>)

@Serializable
class SubjectHistoryList(val history: List<SubjectHistory>)
