package com.infowings.common

import kotlinx.serialization.Serializable

enum class UserRole {
    ADMIN, USER, POWERED_USER
}

@Serializable
data class JwtToken(
    val accessToken: String,
    val refreshToken: String,
    val role: UserRole,
    var accessTokenExpirationTimeInMs: Long,
    var refreshTokenExpirationTimeInMs: Long
)