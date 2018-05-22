package com.infowings.catalog.common

import kotlinx.serialization.Serializable

enum class EventType {
    CREATE, UPDATE, SOFT_DELETE, DELETE
}

@Serializable
data class Delta(
    var field: String,
    var before: String?, // null means created
    var after: String? // null means deleted
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

typealias AspectHistory = HistoryData<AspectDataView>


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
