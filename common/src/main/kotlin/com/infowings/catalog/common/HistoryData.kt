package com.infowings.catalog.common

import com.infowings.catalog.common.history.refbook.RefBookHistoryData
import kotlinx.serialization.Serializable

enum class EventType {
    CREATE, UPDATE, SOFT_DELETE, DELETE
}

@Serializable
data class Delta(
    var field: String,
    var before: String?, // null means created
    val after: String? // null means deleted
)

@Serializable
data class HistoryData<T>(
    var username: String,
    var eventType: EventType,
    var entityName: String,
    var info: String?,
    var deleted: Boolean,
    var timestamp: Long,
    var version: Int,
    var fullData: T,
    var changes: List<Delta>
)

@Serializable
data class SnapshotData(
    val data: Map<String, String>,
    val links: Map<String, List<String>>
)


typealias AspectHistory = HistoryData<AspectDataView>

typealias RefBookHistory = HistoryData<RefBookHistoryData.Companion.BriefState>


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
class AspectHistoryList(val history: List<AspectHistory>)

@Serializable
class RefBookHistoryList(val history: List<RefBookHistory>)
