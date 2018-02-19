package com.infowings.common.catalog

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(var username: String, var password: String)