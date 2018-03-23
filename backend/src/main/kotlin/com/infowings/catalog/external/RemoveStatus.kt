package com.infowings.catalog.external

import kotlinx.serialization.Serializable

enum class StatusType {
    REMOVED, ABSENT, REFERABLE
}

@Serializable
class RemoveStatus(
        val aspectName: String,
        val status: StatusType
)