package com.infowings.catalog.common

import com.infowings.catalog.common.history.objekt.ObjectHistoryData
import com.infowings.catalog.common.history.refbook.RefBookHistoryData
import kotlinx.serialization.Serializable


/*
 * На данный момент сбор данных об истории сделан в виде обобщенного фреймворка, настраиваемого на разные типы
 * сущностей.
 *
 * Кажется, для сбора данных об истории, накатывания дельт и аггрегации,
 * а также для представления данных о структуре изменений, можно сделать нечто подобное.
 * Потому что здесь не так важны бизнес-семантика полей или операции, предоставляемые типами данных в структурах
 * бизнес-логики. Достаточно только сравнивать их строковые представления.
 *
 * Тут есть трудность том, что нужно оперировать сущностями более сложной структуры, чем при сборе данных, но
 * кажется что данные о структуре сложных сущностей можно сделать настраиваемым параметром для общего фреймворка,
 * который бы накатывал дельта, сравнивал версии и сообщал о различиях, работая внутри с json-подобной структурой.
 *
 * С другой стороны, продумывание такого фреймворка может занять какое-то время, поэтому пока его нет делаем так,
 * чтобы реализовывать фичи, но держим в голове эту идею
 *
 * */

enum class EventType {
    CREATE, UPDATE, SOFT_DELETE, DELETE;

    fun isDelete(): Boolean = this == DELETE || this == SOFT_DELETE
}

@Serializable
data class FieldDelta(
    var fieldName: String,
    var before: String?, // null means created
    val after: String? // null means deleted
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
) {
    companion object {
        val empty = HistoryEventData("", 0, 0, EventType.UPDATE, "", "", "")
    }
}

@Serializable
data class HistoryData<T>(
    val event: HistoryEventData,
    var info: String?,
    var deleted: Boolean,
    var fullData: T,
    var changes: List<FieldDelta>
)

typealias AspectHistory = HistoryData<AspectDataView>
typealias RefBookHistory = HistoryData<RefBookHistoryData.Companion.BriefState>
typealias ObjectHistory = HistoryData<ObjectHistoryData.Companion.BriefState>
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
class RefBookHistoryList(val history: List<RefBookHistory>)

@Serializable
class ObjectHistoryList(val history: List<ObjectHistory>)

@Serializable
class SubjectHistoryList(val history: List<SubjectHistory>)
