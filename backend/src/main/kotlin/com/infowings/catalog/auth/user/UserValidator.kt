package com.infowings.catalog.auth.user

import com.infowings.catalog.common.UserData

class UserValidator {

    fun checkUserDataConsistency(userData: UserData) {
        when {
            userData.username.isNullOrEmpty() -> throw UsernameNullOrEmptyException()
            userData.password.isNullOrEmpty() -> throw PasswordNullOrEmptyException()
            userData.role == null -> throw UserRoleNullOrEmptyException()
        }
    }
}