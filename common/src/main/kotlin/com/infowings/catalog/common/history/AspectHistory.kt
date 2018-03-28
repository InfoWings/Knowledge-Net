package com.infowings.catalog.common.history

import com.infowings.catalog.common.AspectData
import kotlinx.serialization.Serializable

typealias AspectHistory = HistoryData<AspectData>

enum class AspectField {
    NAME, MEASURE, BASE_TYPE;
}

// impossible to inherit enum from abstract class. Interfaces are not supported for serialization
@Serializable
class AspectFieldWrapper(private val field: AspectField) : HistoryField() {
    override val name: String
        get() = this.field.name
}


@Serializable
class AspectPropertyHistory(private val index: Int) : HistoryField() {
    override val name: String
        get() = "property[$index]"
}

@Serializable
class AspectHistoryList(val history: List<AspectHistory>)