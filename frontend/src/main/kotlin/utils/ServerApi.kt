package utils


import com.infowings.common.JwtToken
import com.infowings.common.UserDto
import kotlinx.coroutines.experimental.await
import org.w3c.dom.get
import org.w3c.dom.set
import org.w3c.fetch.RequestCredentials
import org.w3c.fetch.RequestInit
import org.w3c.fetch.Response
import kotlin.browser.localStorage
import kotlin.browser.window
import kotlin.js.JSON
import kotlin.js.json
import kotlinx.serialization.json.JSON as KJSON

private const val POST = "POST"
private const val GET = "GET"

private const val AUTH_ACCESS_TOKEN = "auth-access-token"
private const val AUTH_REFRESH_TOKEN = "auth-refresh-token"
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
 * Http request to server with authorization headers.
 */
private suspend fun authorizedRequest(method: String, url: String, body: dynamic): Response {
    var response = request(method, url, body, authorizationHeaders)

    if (!response.ok) {
        response = refreshTokenAndRepeatRequest(method, url, body, response)
    }

    return response
}

/**
 * Authorization headers:
 *  1. authorization access token
 *  2. authorization refresh token
 */
private val authorizationHeaders = json(
    "x-access-authorization" to "Bearer ${localStorage[AUTH_ACCESS_TOKEN]}",
    "x-refresh-authorization" to "Bearer ${localStorage[AUTH_REFRESH_TOKEN]}"
)

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
        return request(method, url, body, authorizationHeaders)
    }
    removeTokenInfo()
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

private suspend fun parseToken(response: Response): Boolean {
    try {
        val jwtToken = JSON.parse<JwtToken>(response.text().await())
        console.log("refresh: $jwtToken")
        localStorage[AUTH_ACCESS_TOKEN] = jwtToken.accessToken
        localStorage[AUTH_REFRESH_TOKEN] = jwtToken.refreshToken
        localStorage[AUTH_ROLE] = jwtToken.role.name
        return true
    } catch (e: Exception) {
        return false
    }
}

private fun removeTokenInfo() {
    localStorage.removeItem(AUTH_ACCESS_TOKEN)
    localStorage.removeItem(AUTH_REFRESH_TOKEN)
    localStorage.removeItem(AUTH_ROLE)
}