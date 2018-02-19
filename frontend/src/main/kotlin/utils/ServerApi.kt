package utils


import com.infowings.common.JwtToken
import com.infowings.common.UserDto
import kotlinx.coroutines.experimental.await
import kotlinx.serialization.json.JSON
import org.w3c.dom.get
import org.w3c.dom.set
import org.w3c.fetch.RequestCredentials
import org.w3c.fetch.RequestInit
import org.w3c.fetch.Response
import kotlin.browser.localStorage
import kotlin.browser.window
import kotlin.js.json

private const val POST = "POST"
private const val GET = "GET"

private const val AUTH_ACCESS_TOKEN = "auth-access-token"
private const val AUTH_REFRESH_TOKEN = "auth-refresh-token"
private const val AUTH_ROLE = "auth-role"

suspend fun post(url: String, body: dynamic) = authorizedRequest(POST, url, body)

suspend fun get(url: String, body: dynamic = null) = authorizedRequest(GET, url, body)

private suspend fun authorizedRequest(method: String, url: String, body: dynamic): Response {
    var response = request(method, url, body, authorizationHeaders)

    if (!response.ok) {
        response = refreshAndRepeat(method, url, body, response)
    }

    return response
}

private suspend fun request(method: String, url: String, body: dynamic, headers: dynamic = defaultHeaders): Response =
    window.fetch(url, object : RequestInit {
        override var method: String? = method
        override var body: dynamic = body
        override var credentials: RequestCredentials? = "same-origin".asDynamic()
        override var headers: dynamic = headers
    }).await()

private val authorizationHeaders = json(
    "x-access-authorization" to "Bearer ${localStorage[AUTH_ACCESS_TOKEN]}",
    "x-refresh-authorization" to "Bearer ${localStorage[AUTH_REFRESH_TOKEN]}"
)

private val defaultHeaders = json(
    "Accept" to "application/json",
    "Content-Type" to "application/json;charset=UTF-8"
)

private suspend fun refreshAndRepeat(method: String, url: String, body: dynamic, oldResponse: Response): Response {
    val isRefreshed = refresh()
    if (isRefreshed) {
        return request(method, url, body, authorizationHeaders)
    }
    removeTokenInfo()
    window.location.replace("/")
    return oldResponse
}

suspend fun <T> postAndParseResult(url: String, body: dynamic, parse: (dynamic) -> T): T =
    requestAndParseResult(POST, url, body, parse)

suspend fun <T> getAndParseResult(url: String, body: dynamic, parse: (dynamic) -> T): T =
    requestAndParseResult(GET, url, body, parse)

private suspend fun <T> requestAndParseResult(method: String, url: String, body: dynamic, parse: (dynamic) -> T): T {
    val response = authorizedRequest(method, url, body)
    return parse(response.json().await())
}

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

private suspend fun refresh(): Boolean {
    val response = get("/api/access/refresh")
    if (response.ok) {
        val isParsed = parseToken(response)
        return isParsed
    }
    return false
}

private fun removeTokenInfo() {
    localStorage.removeItem(AUTH_ACCESS_TOKEN)
    localStorage.removeItem(AUTH_REFRESH_TOKEN)
    localStorage.removeItem(AUTH_ROLE)
}