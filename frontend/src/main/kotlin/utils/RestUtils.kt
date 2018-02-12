package utils

import com.infowings.common.JwtToken
import com.infowings.common.UserDto
import kotlinx.coroutines.experimental.await
import kotlinx.serialization.json.JSON
import org.w3c.dom.get
import org.w3c.fetch.RequestCredentials
import org.w3c.fetch.RequestInit
import org.w3c.fetch.Response
import kotlin.browser.localStorage
import kotlin.browser.window
import kotlin.js.json


suspend fun login(url: String, body: UserDto): Boolean {
    val response = request("POST", url, JSON.stringify(body))
    if (response.ok) {
        parseToken(response)
    }
    return response.ok
}

suspend fun refresh(): Boolean {
    val response = request("GET", "/api/access/refresh", headers = getHeader())
    if (response.ok) {
        parseToken(response)
    }
    return response.ok
}

suspend fun parseToken(response: Response) {
    val jwtToken = JSON.parse<JwtToken>(response.text().await())
    console.log("refresh: $jwtToken")
    localStorage.setItem("auth-access-token", jwtToken.accessToken)
    localStorage.setItem("auth-refresh-token", jwtToken.refreshToken)
    localStorage.setItem("auth-role", jwtToken.role.name)
}

suspend fun request(method: String,
                    url: String,
                    body: dynamic = null,
                    headers: dynamic = json(
                            "Accept" to "application/json",
                            "Content-Type" to "application/json;charset=UTF-8")): Response {

    val response = window.fetch(url, object : RequestInit {
        override var method: String? = method
        override var body: dynamic = body
        override var credentials: RequestCredentials? = "same-origin".asDynamic()
        override var headers: dynamic = headers
    }).await()
    return response
}

suspend fun getRequest(url: String, body: dynamic = null): String {
    var response = request("GET", url, body, getHeader())
    if (!response.ok) {
        try {
            refresh()
        } catch (e: Exception) {
            removeTokenInfo()
            window.location.replace("/")
        }
        response = request("GET", url, body, getHeader())
        if (response.status.toInt() == 403 || response.status.toInt() == 401) {
            removeTokenInfo()
            window.location.replace("/")
        }
    }
    return response.text().await()
}

fun getHeader(): dynamic {
    return json("x-access-authorization" to "Bearer ${localStorage["auth-access-token"]}",
            "x-refresh-authorization" to "Bearer ${localStorage["auth-refresh-token"]}")
}

fun removeTokenInfo() {
    localStorage.removeItem("auth-access-token")
    localStorage.removeItem("auth-refresh-token")
    localStorage.removeItem("auth-role")
}
