package com.infowings.catalog.utils


import com.infowings.catalog.common.JwtToken
import com.infowings.catalog.common.UserDto
import kotlinx.coroutines.experimental.await
import org.w3c.fetch.RequestCredentials
import org.w3c.fetch.RequestInit
import org.w3c.fetch.Response
import kotlin.browser.document
import kotlin.browser.window
import kotlin.js.Date
import kotlin.js.JSON
import kotlin.js.json
import kotlinx.serialization.json.JSON as KJSON

private const val POST = "POST"
private const val GET = "GET"

private const val AUTH_ROLE = "auth-role"

/**
 * Http POST request to server.
 * Return object of type T which is obtained by parsing response text.
 */
suspend fun <T> post(url: String, body: dynamic): T {
    return JSON.parse(authorizedRequest(POST, url, body).text().await())
}

/**
 * Http GET request to server.
 * Return object of type T which is obtained by parsing response text.
 */
suspend fun <T> get(url: String, body: dynamic = null): T {
    return JSON.parse(authorizedRequest(GET, url, body).text().await())
}

/**
 * Http request to server after authorization.
 */
private suspend fun authorizedRequest(method: String, url: String, body: dynamic): Response {
    var response = request(method, url, body)
    if (!response.ok) {
        response = refreshTokenAndRepeatRequest(method, url, body, response)
    }
    return response
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
 * If refreshing was successful then return response to repeat request,
 * else replace window location to root.
 */
private suspend fun refreshTokenAndRepeatRequest(
    method: String,
    url: String,
    body: dynamic,
    oldResponse: Response
): Response {
    val isRefreshed = refreshToken()
    if (isRefreshed) {
        return request(method, url, body)
    }
    window.location.replace("/")
    return oldResponse
}

private suspend fun refreshToken(): Boolean {
    val response = authorizedRequest(GET, "/api/access/refresh", null)
    if (response.ok) {
        val isParsed = parseToken(response)
        return isParsed
    }
    return false
}

/**
 * Method for login to server.
 * After success login authorization token saved in local storage
 */
suspend fun login(body: UserDto): Boolean {
    val response = request(POST, "/api/access/signIn", JSON.stringify(body))
    if (response.ok) {
        val isParsed = parseToken(response)
        return isParsed
    }
    return false
}

/**
 * Parse token save in cookies
 */
private suspend fun parseToken(response: Response): Boolean {
    return try {
        var ms: dynamic
        val jwtToken = JSON.parse<JwtToken>(response.text().await())
        val nowInMs = Date.now()
        ms = jwtToken.accessTokenExpirationTimeInMs.asDynamic() + nowInMs
        val accessExpireDate = Date(ms as Number).toUTCString()
        ms = jwtToken.refreshTokenExpirationTimeInMs.asDynamic() + nowInMs
        val refreshExpireDate = Date(ms as Number).toUTCString()
        document.cookie = "x-access-authorization=Bearer ${jwtToken.accessToken};expires=$accessExpireDate;path=/"
        document.cookie = "x-refresh-authorization=Bearer ${jwtToken.refreshToken};expires=$refreshExpireDate;path=/"
        document.cookie = "$AUTH_ROLE=${jwtToken.role}"
        true
    } catch (e: Exception) {
        false
    }
}

/**
 * Return authorization role name that saved in cookies.
 */
fun getAuthorizationRole(): String? {
    return document.cookie.split("; ")
        .filter { it.startsWith(AUTH_ROLE) }
        .map { it.split("=")[1] }
        .filter { it.isNotEmpty() }
        .getOrNull(0)
}

fun logout() {
    document.cookie = "$AUTH_ROLE="
}