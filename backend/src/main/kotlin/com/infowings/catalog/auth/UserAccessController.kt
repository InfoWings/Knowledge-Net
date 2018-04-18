package com.infowings.catalog.auth

import com.infowings.catalog.auth.user.UserService
import com.infowings.catalog.common.JwtToken
import com.infowings.catalog.common.UserDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.bind.annotation.*
import java.net.URLDecoder

@RestController
@RequestMapping("/api/access")
class UserAccessController(var userService: UserService, var jwtService: JWTService) {

    @Value("\${spring.security.header.refresh}")
    lateinit var REFRESH_HEADER: String

    @PostMapping("signIn")
    fun signIn(@RequestBody user: UserDto): ResponseEntity<*> {
        try {
            val userEntity = userService.findByUsername(user.username)
            if (userEntity.password == user.password) {
                return ResponseEntity(jwtService.createJwtToken(userEntity.username), HttpStatus.OK)
            }
        } catch (ignored: UsernameNotFoundException) {
        }
        return ResponseEntity("Invalid user and password pair", HttpStatus.FORBIDDEN)
    }

    @GetMapping("refresh")
    fun refresh(request: RequestEntity<Map<String, String>>): ResponseEntity<JwtToken> {
        return try {
            val refreshTokenHeader = request.headers.getFirst("cookie")!!
                .split("; ")
                .filter { it.startsWith(REFRESH_HEADER) }
                .map { it.split("=")[1] }
                .map { URLDecoder.decode(it, "UTF-8") }
                .first()

            val jwtInfo = jwtService.parseTokenString(refreshTokenHeader)
            ResponseEntity(jwtService.createJwtToken(jwtInfo!!.username), HttpStatus.OK)
        } catch (e: Exception) {
            ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
    }
}

class UserDetailsServiceImpl(var userService: UserService) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        val user = userService.findByUsername(username)
        return User(
            user.username,
            user.password,
            mutableListOf<GrantedAuthority>(SimpleGrantedAuthority(user.role.name))
        )
    }
}