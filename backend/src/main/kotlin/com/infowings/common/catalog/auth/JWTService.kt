package com.infowings.common.catalog.auth

import com.infowings.common.catalog.JwtToken
import com.infowings.common.catalog.UserRole
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JSON
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*

@Serializable
data class JwtInfo(var username: String, var role: UserRole)

@Service
class JWTService {

    @Value("\${spring.security.secret}")
    lateinit var SECRET: String

    @Value("\${spring.security.access.expiration-time}")
    lateinit var ACCESS_TIME: String

    @Value("\${spring.security.refresh.expiration-time}")
    lateinit var REFRESH_TIME: String

    @Value("\${spring.security.prefix}")
    lateinit var PREFIX: String

    @Autowired
    lateinit var userAcceptService: UserAcceptService


    fun createJwtToken(username: String): JwtToken {
        val user = userAcceptService.findByUsername(username)
        val jwtInfo = JwtInfo(user!!.username, user.role)
        val accessToken = createTokenString(jwtInfo, ACCESS_TIME.toLong())
        val refreshToken = createTokenString(jwtInfo, REFRESH_TIME.toLong())
        return JwtToken(accessToken, refreshToken, jwtInfo.role, ACCESS_TIME.toLong())
    }

    fun parseTokenString(token: String): JwtInfo? {

        try {
            val obj = Jwts.parser()
                    .setSigningKey(SECRET)
                    .parseClaimsJws(token.replace("$PREFIX ", ""))
                    .body

            return if (obj.subject != null && Date().before(obj.expiration))
                JSON.parse(obj.subject) else null

        } catch (ignored: Exception) {
            return null
        }
    }

    private fun createTokenString(jwtInfo: JwtInfo, expirationTime: Long) =
            Jwts.builder()
                    .setSubject(JSON.stringify(jwtInfo))
                    .setExpiration(Date(System.currentTimeMillis() + expirationTime))
                    .signWith(SignatureAlgorithm.HS512, SECRET)
                    .compact()
}