package com.infowings.catalog

import kotlinx.serialization.Serializable

enum class UserRole {
    ADMIN, USER, POWERED_USER
}

@Serializable
data class JwtToken(val accessToken: String, val refreshToken: String, val role: UserRole, var expirationTime: Long)