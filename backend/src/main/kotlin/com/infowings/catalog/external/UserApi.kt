package com.infowings.catalog.external

import com.infowings.catalog.auth.user.UserNotFoundException
import com.infowings.catalog.auth.user.UserService
import com.infowings.catalog.auth.user.UserWithSuchUsernameAlreadyExist
import com.infowings.catalog.common.User
import com.infowings.catalog.common.UserData
import com.infowings.catalog.common.Users
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/admin/users")
class UserApi(val userService: UserService) {

    @GetMapping("all")
    fun getAllUsers() = Users(userService.getAllUsers().map { it.copy(password = "") })

    @PostMapping("update")
    fun updateUser(@RequestBody user: User) = userService.updateUser(user).copy(password = "")

    @PostMapping("create")
    fun createUser(@RequestBody userData: UserData) = userService.createUser(userData).copy(password = "")

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<String> {
        return when (e) {
            is UserNotFoundException, is UserWithSuchUsernameAlreadyExist -> ResponseEntity.badRequest().body(e.message)
            else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.message)
        }
    }
}