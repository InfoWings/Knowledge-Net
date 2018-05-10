package com.infowings.catalog.external

import com.infowings.catalog.auth.user.*
import com.infowings.catalog.common.*
import com.infowings.catalog.common.Users
import kotlinx.serialization.json.JSON
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

    private fun errorResponse(
        message: String,
        code: BadRequestCode = BadRequestCode.INCORRECT_INPUT
    ): ResponseEntity<String> = ResponseEntity.badRequest().body(JSON.stringify(BadRequest(code, message)))

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<String> {
        return when (e) {

            is UserNotFoundException -> errorResponse("User with username: ${e.username} not found.")

            is UserWithSuchUsernameAlreadyExist -> errorResponse("User with username: ${e.username} already exist.")

            is UsernameNullOrEmptyException -> errorResponse("Username should not be empty")

            is PasswordNullOrEmptyException -> errorResponse("Password should not be empty")

            is UserRoleNullOrEmptyException -> errorResponse("User role should not be empty")

            else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.message)
        }
    }
}