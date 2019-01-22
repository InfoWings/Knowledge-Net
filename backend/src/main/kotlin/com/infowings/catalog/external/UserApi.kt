package com.infowings.catalog.external

import com.infowings.catalog.auth.user.*
import com.infowings.catalog.common.BadRequestCode
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
    fun getAllUsers() = Users(userService.getAllUsers().map { it.removePassword() })

    @PostMapping("changeRole")
    fun changeRole(@RequestBody user: User) = userService.changeRole(user).removePassword()

    @PostMapping("changeBlocked")
    fun changeBlocked(@RequestBody user: User) = userService.changeBlocked(user).removePassword()

    @PostMapping("changePassword")
    fun changePassword(@RequestBody user: User) = userService.changePassword(user).removePassword()

    @PostMapping("create")
    fun createUser(@RequestBody userData: UserData) = userService.createUser(userData).removePassword()

    private fun User.removePassword() = this.copy(password = "")

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<String> {
        return when (e) {
            is UserNotFoundException -> badRequest("User with username: ${e.username} not found.", BadRequestCode.INCORRECT_INPUT)
            is UserWithSuchUsernameAlreadyExist -> badRequest("User with username: ${e.username} already exist.", BadRequestCode.INCORRECT_INPUT)
            is UsernameNullOrEmptyException -> badRequest("Username should not be empty", BadRequestCode.INCORRECT_INPUT)
            is PasswordNullOrEmptyException -> badRequest("Password should not be empty", BadRequestCode.INCORRECT_INPUT)
            is UserRoleNullOrEmptyException -> badRequest("User role should not be empty", BadRequestCode.INCORRECT_INPUT)

            else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.message)
        }
    }
}