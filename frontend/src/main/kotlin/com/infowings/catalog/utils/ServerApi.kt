package com.infowings.catalog.utils


import com.infowings.catalog.common.JwtToken
import com.infowings.catalog.common.UserCredentials
import kotlinx.coroutines.experimental.await
import org.w3c.fetch.RequestCredentials
import org.w3c.fetch.RequestInit
import org.w3c.fetch.Response
import kotlin.browser.document
import kotlin.browser.window
import kotlin.js.*
import kotlin.js.JSON
import kotlinx.serialization.json.JSON as KJSON

private const val POST = "POST"
private const val GET = "GET"
private const val PUT = "PUT"

private const val AUTH_ROLE = "auth-role"
private const val REFRESH_AUTH = "x-refresh-authorization"
private const val ACCESS_AUTH = "x-access-authorization"

private const val OK = 200
private const val BAD_REQUEST = 400
private const val UNAUTHORIZED = 401
private const val FORBIDDEN = 403
private const val NOT_MODIFIED = 304

external fun encodeURIComponent(component: String): String = definedExternally
external fun decodeURIComponent(component: String): String = definedExternally

/**
 * Http POST request to server.
 * Return response text.
 */
suspend fun post(url: String, body: dynamic): String {
    return authorizedRequest(POST, url, body).text().await()
}

/**
 * Http GET request to server.
 * Returns response text.
 */
suspend fun get(url: String, body: dynamic = null): String {
    return authorizedRequest(GET, url, body).text().await()
}

/**
 * Http PUT request to server.
 * Returns response text.
 */
suspend fun put(url: String, body: dynamic = null): String {
    return authorizedRequest(PUT, url, body).text().await()
}


/**
 * Http request to server after authorization.
 * If response status 200(OK) then return response
 * if response status 401(unauthorized) then remove role cookie and redirect to login page
 * if response status 403(forbidden) then refresh token and repeat request
 * if response status 400(bad request) then throws [BadRequestException]
 * else throws ServerException
 */
private suspend fun authorizedRequest(method: String, url: String, body: dynamic, repeat: Boolean = false): Response {
    val response = request(method, url, body)
    val statusCode = response.status.toInt()

    return when (statusCode) {
        OK -> response
        UNAUTHORIZED -> {
            redirectToLoginPage()
            response
        }
        FORBIDDEN -> {
            if (repeat) {
                redirectToLoginPage()
                response
            } else {
                refreshTokenAndRepeatRequest(method, url, body)
            }
        }
        NOT_MODIFIED -> throw NotModifiedException()
        BAD_REQUEST -> throw BadRequestException(response.text().await())
        else -> throw ServerException(response.text().await())
    }
}

/**
 * Exception that represents unsuccessful response on http request to server,
 * except for 401(unauthorized) and 403(forbidden) cases
 */

class ServerException(message: String) : RuntimeException(message)

/**
 * Exception that contains message about what was wrong with request to server.
 */
class BadRequestException(override val message: String) : RuntimeException(message)

class NotModifiedException : RuntimeException()

private fun redirectToLoginPage() {
    removeAuthRole()
    window.location.replace("/")
}

/**
 * Generic request to server with default headers.
 */
private suspend fun request(method: String, url: String, body: dynamic, headers: dynamic = defaultHeaders): Response =
    window.fetch(url, object : RequestInit {
        override var method: String? = method
        override var body: dynamic = body
        override var credentials: RequestCredentials? = "same-origin".asDynamic()
        override var headers: dynamic = headers
    }).await()

private val defaultHeaders = json(
    "Accept" to "application/json",
    "Content-Type" to "application/json;charset=UTF-8"
)

/**
 * Method that try to refresh token and repeat request.
 * If response to refresh token request was successful then return response to repeat request
 * if status of response to refresh token request is 401 (unauthorized) then redirect to login page
 * @throws ServerException if response to refresh request status is not 200 or 401
 */
private suspend fun refreshTokenAndRepeatRequest(method: String, url: String, body: dynamic): Response {
    val responseToRefresh = request(GET, "/api/access/refresh", null)
    val refreshStatus = responseToRefresh.status.toInt()
    return when (refreshStatus) {
        OK -> {
            parseToken(responseToRefresh)
            authorizedRequest(method, url, body, repeat = true)
        }
        UNAUTHORIZED -> {
            redirectToLoginPage()
            responseToRefresh
        }
        else -> throw ServerException(responseToRefresh.text().await())
    }
}

/**
 * Method for login to server.
 */
suspend fun login(body: UserCredentials): Boolean {
    val response = request(POST, "/api/access/signIn", JSON.stringify(body))
    return if (response.ok) {
        try {
            parseToken(response)
            true
        } catch (e: TokenParsingException) {
            console.log(e.message)
            false
        }
    } else {
        false
    }
}

/**
 * Parse token and save in cookies
 * @throws TokenParsingException if parsing fail
 */
private suspend fun parseToken(response: Response) {
    try {
        val jwtToken = JSON.parse<JwtToken>(response.text().await())
        val nowInMs = Date.now()

        val accessExpireDateMs = jwtToken.accessTokenExpirationTimeInMs.asDynamic() + nowInMs
        val accessExpireDate = Date(accessExpireDateMs as Number).toUTCString()

        val refreshExpireDateMs = jwtToken.refreshTokenExpirationTimeInMs.asDynamic() + nowInMs
        val refreshExpireDate = Date(refreshExpireDateMs as Number).toUTCString()

        val accessValue = encodeURIComponent("Bearer ${jwtToken.accessToken}")
        val refreshValue = encodeURIComponent("Bearer ${jwtToken.refreshToken}")

        document.cookie = "$ACCESS_AUTH=$accessValue;expires=$accessExpireDate;path=/"
        document.cookie = "$REFRESH_AUTH=$refreshValue;expires=$refreshExpireDate;path=/"
        document.cookie = "$AUTH_ROLE=${jwtToken.role}"
    } catch (e: Exception) {
        throw TokenParsingException(e)
    }
}

private class TokenParsingException(e: Exception) : RuntimeException(e) {
    override val message: String?
        get() = "TokenParsingException caused by:\n${super.message}"
}

/**
 * Return authorization role name that saved in cookies.
 */
fun getAuthorizationRole(): String? {
    return getCookieValue(AUTH_ROLE)
}

private fun getCookieValue(key: String): String? {
    return document.cookie.split("; ")
        .filter { it.startsWith(key) }
        .map { it.split("=")[1] }
        .filter { it.isNotEmpty() }
        .map { decodeURIComponent(it) }
        .firstOrNull()
}

/**
 * Remove authorization role from cookies
 */
fun removeAuthRole() {
    document.cookie = "$AUTH_ROLE="
}