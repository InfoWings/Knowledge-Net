package com.infowings.catalog.auth

import com.infowings.catalog.auth.user.UserService
import com.infowings.catalog.common.JwtToken
import com.infowings.catalog.common.UserRole
import com.infowings.catalog.loggerFor
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.Principal
import java.util.*

@Serializable
data class JwtInfo(var username: String, var role: UserRole) : Principal {
    // реализовать Principal важно для того, чтобы в метода класслов с тегами
    // @RestController и @RequestMapping(...) можно было использовать параметр типа
    // Principal и в него попадал экземпляр JwtInfo
    // А если не унаследовать, то Spring создаст свой экземпляр, чей getName()
    // будет возвращать toString от экземплчяра JwtInfo
    override fun getName() = username
}

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
    lateinit var userService: UserService


    fun createJwtToken(username: String): JwtToken {
        val user = userService.findByUsername(username)
        val jwtInfo = JwtInfo(user.username, user.role)
        val accessToken = createTokenString(jwtInfo, ACCESS_TIME.toLong())
        val refreshToken = createTokenString(jwtInfo, REFRESH_TIME.toLong())
        return JwtToken(accessToken, refreshToken, jwtInfo.role, ACCESS_TIME.toLong(), REFRESH_TIME.toLong())
    }

    fun parseTokenString(token: String): JwtInfo? {
        return try {
            val obj = Jwts.parser()
                .setSigningKey(SECRET)
                .parseClaimsJws(token.replace("$PREFIX ", ""))
                .body

            if (obj.subject != null && Date().before(obj.expiration))
                Json.parse(JwtInfo.serializer(), obj.subject) else null

        } catch (e: Exception) {
            logger.error(e.message)
            null
        }
    }

    private fun createTokenString(jwtInfo: JwtInfo, expirationTime: Long) =
        Jwts.builder()
            .setSubject(Json.stringify(JwtInfo.serializer(), jwtInfo))
            .setExpiration(Date(System.currentTimeMillis() + expirationTime))
            .signWith(SignatureAlgorithm.HS512, SECRET)
            .compact()
}

private val logger = loggerFor<JWTService>()