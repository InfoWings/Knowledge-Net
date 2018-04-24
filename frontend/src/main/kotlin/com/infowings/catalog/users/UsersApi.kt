package com.infowings.catalog.users

import com.infowings.catalog.common.User
import com.infowings.catalog.common.Users
import com.infowings.catalog.utils.get
import com.infowings.catalog.utils.post
import kotlinx.serialization.json.JSON

internal suspend fun getAllUsers(): Users = JSON.parse(get("/api/admin/users/all"))

internal suspend fun createUser(user: User): User = JSON.parse(post("/api/admin/users/create", JSON.stringify(user)))

internal suspend fun updateUser(user: User): User = JSON.parse(post("/api/admin/users/update", JSON.stringify(user)))