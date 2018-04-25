package com.infowings.catalog.common

import kotlinx.serialization.Serializable

@Serializable
data class UserCredentials(val username: String, val password: String)

@Serializable
data class User(
    val username: String,
    val password: String,
    val role: UserRole,
    val blocked: Boolean = false
)

@Serializable
data class Users(val users: List<User>)