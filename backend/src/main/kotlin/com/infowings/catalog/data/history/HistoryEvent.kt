package com.infowings.catalog.data.history

import com.infowings.catalog.data.aspect.Aspect
import com.infowings.catalog.data.aspect.AspectPropertyCardinality

data class HistoryEvent(
        val name: String,
        val user: String,
        val rid: String,
        val event: EventKind,
        val cls: String,
        val data: Map<String, String>
)