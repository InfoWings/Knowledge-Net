package com.infowings.catalog.auth

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
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/api/access")
class UserAccessController(var userAcceptService: UserAcceptService, var jwtService: JWTService) {

    @Value("\${spring.security.header.refresh}")
    lateinit var REFRESH_HEADER: String

    @PostMapping("signIn")
    fun signIn(@RequestBody user: UserDto): ResponseEntity<*> {
        val userEntity = userAcceptService.findByUsername(user.username)
        if (userEntity == null || userEntity.password != user.password) {
            return ResponseEntity("Invalid user and password pair", HttpStatus.FORBIDDEN)
        }
        return ResponseEntity(jwtService.createJwtToken(userEntity.username), HttpStatus.OK)
    }

    @GetMapping("refresh")
    fun refresh(request: RequestEntity<Map<String, String>>): ResponseEntity<JwtToken> {
        val refreshTokenHeader = request.headers.getFirst(REFRESH_HEADER)
        return try {
            val jwtInfo = jwtService.parseTokenString(refreshTokenHeader)
            ResponseEntity(jwtService.createJwtToken(jwtInfo!!.username), HttpStatus.OK)
        } catch (e: Exception) {
            ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
    }
}

@Service
class UserDetailsServiceImpl(var userAcceptService: UserAcceptService) : UserDetailsService {
    override fun loadUserByUsername(username: String?): UserDetails? {
        val user = userAcceptService.findByUsername(username!!) ?: return null
        return User(user.username, user.password, mutableListOf<GrantedAuthority>(SimpleGrantedAuthority(user.role.name)))
    }
}