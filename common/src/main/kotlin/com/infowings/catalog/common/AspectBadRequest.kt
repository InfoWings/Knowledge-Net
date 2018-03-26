package com.infowings.catalog.common

import kotlinx.serialization.Serializable

@Serializable
data class AspectBadRequest(val code: AspectBadRequestCode, val message: String?)

enum class AspectBadRequestCode {
    INCORRECT_INPUT, NEED_CONFIRMATION
}