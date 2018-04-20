package com.infowings.catalog.common

import kotlinx.serialization.Serializable

@Serializable
data class UserCredentials(val username: String, val password: String)

@Serializable
data class UserData(val username: String, val userRole: UserRole, val blocked: Boolean)

@Serializable
data class UsersList(val users: List<UserData>)