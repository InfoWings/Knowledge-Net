package com.infowings.catalog.auth

import org.springframework.core.env.Environment
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


class JWTAuthorizationFilter(authManager: AuthenticationManager, var env: Environment, var jwtService: JWTService)
    : BasicAuthenticationFilter(authManager) {

    override fun doFilterInternal(request: HttpServletRequest?, response: HttpServletResponse?, chain: FilterChain?) {
        if (request!!.requestURI.startsWith("/api/access")) {
            chain?.doFilter(request, response)
            return
        }
        val headerAccess = request.getHeader(env.getProperty("spring.security.header.access"))
        if (headerAccess == null || !headerAccess.startsWith(env.getProperty("spring.security.prefix"))) {
            chain?.doFilter(request, response)
            return
        }
        val authentication = getAuthentication(headerAccess)

        SecurityContextHolder.getContext().authentication = authentication
        chain?.doFilter(request, response)
    }

    private fun getAuthentication(token: String): UsernamePasswordAuthenticationToken? {
        val user = jwtService.parseTokenString(token)
        return if (user != null) {
            UsernamePasswordAuthenticationToken(user, null, mutableListOf<GrantedAuthority>(SimpleGrantedAuthority(user.role.name)))
        } else null
    }
}