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

private val defaultHeaders = json(
    "Accept" to "application/json",
    "Content-Type" to "application/json;charset=UTF-8"
)

suspend fun request(method: String, url: String, body: dynamic, headers: dynamic = defaultHeaders): Response =
    window.fetch(url, object : RequestInit {
        override var method: String? = method
        override var body: dynamic = body
        override var credentials: RequestCredentials? = "same-origin".asDynamic()
        override var headers: dynamic = headers
    }).await()

suspend fun post(url: String, body: dynamic) = request("POST", url, body, authorizationHeaders)

suspend fun get(url: String, body: dynamic = null) = request("GET", url, body, authorizationHeaders)

suspend fun <T> requestAndParseResult(method: String, url: String, body: dynamic, parse: (dynamic) -> T): T {
    val response = request(method, url, body, authorizationHeaders)
    return parse(response.json().await())
}

suspend fun <T> postAndParseResult(url: String, body: dynamic, parse: (dynamic) -> T): T =
    requestAndParseResult("POST", url, body, parse)

suspend fun <T> getAndParseResult(url: String, body: dynamic, parse: (dynamic) -> T): T =
    requestAndParseResult("GET", url, body, parse)

suspend fun login(body: UserDto): Boolean {
    val response = request("POST", "/api/access/signIn", JSON.stringify(body))
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
        localStorage["auth-access-token"] = jwtToken.accessToken
        localStorage["auth-refresh-token"] = jwtToken.refreshToken
        localStorage["auth-role"] = jwtToken.role.name
        return true
    } catch (e: Exception) {
        return false
    }
}

suspend fun getResponseText(url: String): String {
    var response = get(url)
    if (!response.ok) {
        response = refreshAndGet(url) ?: response
    }
    return response.text().await()
}

private val authorizationHeaders = json(
    "x-access-authorization" to "Bearer ${localStorage["auth-access-token"]}",
    "x-refresh-authorization" to "Bearer ${localStorage["auth-refresh-token"]}"
)

private suspend fun refreshAndGet(url: String): Response? {
    val isRefreshed = refresh()
    if (isRefreshed) {
        return get(url)
    }
    removeTokenInfo()
    window.location.replace("/")
    return null
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
    localStorage.removeItem("auth-access-token")
    localStorage.removeItem("auth-refresh-token")
    localStorage.removeItem("auth-role")
}