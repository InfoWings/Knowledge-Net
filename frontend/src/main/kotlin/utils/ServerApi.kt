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

suspend fun request(method: String, url: String, body: dynamic, headers: dynamic = defaultHeaders): Response {
    val response = window.fetch(url, object : RequestInit {
        override var method: String? = method
        override var body: dynamic = body
        override var credentials: RequestCredentials? = "same-origin".asDynamic()
        override var headers: dynamic = headers
    }).await()
    return response
}

suspend fun post(url: String, body: dynamic, headers: dynamic) = request("POST", url, body, headers)

suspend fun get(url: String, body: dynamic, headers: dynamic) = request("GET", url, body, headers)

suspend fun <T> requestAndParseResult(method: String, url: String, body: dynamic, parse: (dynamic) -> T): T {
    val response = request(method, url, body)
    return parse(response.json().await())
}

suspend fun <T> postAndParseResult(url: String, body: dynamic, parse: (dynamic) -> T): T =
    requestAndParseResult("POST", url, body, parse)

suspend fun <T> getAndParseResult(url: String, body: dynamic, parse: (dynamic) -> T): T =
    requestAndParseResult("GET", url, body, parse)

suspend fun login(url: String, body: UserDto): Boolean {
    val response = post(url, JSON.stringify(body), null)
    if (response.ok) {
        parseToken(response)
    }
    return response.ok
}

private suspend fun parseToken(response: Response) {
    val jwtToken = JSON.parse<JwtToken>(response.text().await())
    console.log("refresh: $jwtToken")
    localStorage["auth-access-token"] = jwtToken.accessToken
    localStorage["auth-refresh-token"] = jwtToken.refreshToken
    localStorage["auth-role"] = jwtToken.role.name
}

suspend fun getResponseText(url: String): String {
    var response = get(url, null, headers)
    if (!response.ok) {
        response = refreshAndGet(url) ?: response
    }
    return response.text().await()
}

private val headers = json(
    "x-access-authorization" to "Bearer ${localStorage["auth-access-token"]}",
    "x-refresh-authorization" to "Bearer ${localStorage["auth-refresh-token"]}"
)

private suspend fun refreshAndGet(url: String): Response? {
    try {
        val refreshed = refresh()
        if (refreshed) {
            return get(url, null, headers)
        }
    } catch (ignored: Exception) {
    }
    removeTokenInfo()
    window.location.replace("/")
    return null
}

private suspend fun refresh(): Boolean {
    val response = get("/api/access/refresh", null, headers)
    if (response.ok) {
        parseToken(response)
    }
    return response.ok
}

private fun removeTokenInfo() {
    localStorage.removeItem("auth-access-token")
    localStorage.removeItem("auth-refresh-token")
    localStorage.removeItem("auth-role")
}