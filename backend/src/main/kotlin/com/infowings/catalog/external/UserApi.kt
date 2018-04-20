package com.infowings.catalog.external

import com.infowings.catalog.auth.user.User
import com.infowings.catalog.auth.user.UserService
import com.infowings.catalog.common.UserData
import com.infowings.catalog.common.UsersList
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/admin/users")
class UserApi(val userService: UserService) {

    @GetMapping("all")
    fun getAllUsers() = UsersList(userService.getAllUsers().map { it.toUserData() })

    @PostMapping("block/{username}")
    fun blockUser(@PathVariable username: String) = userService.blockUser(username).toUserData()

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<String> {
        return when (e) {
            is UsernameNotFoundException -> ResponseEntity.badRequest().body(e.message)
            else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.message)
        }
    }

    private fun User.toUserData() = UserData(this.username, this.role, this.blocked)
}