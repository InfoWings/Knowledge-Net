package com.infowings.catalog.common

import kotlinx.serialization.Serializable

@Serializable
data class BadRequest(val code: BadRequestCode, val message: String?)

enum class BadRequestCode {
    INCORRECT_INPUT,
    NEED_CONFIRMATION
}