package com.infowings.catalog.api

import com.infowings.catalog.common.JwtToken
import com.infowings.catalog.common.UserCredentials
import kotlinx.serialization.json.JSON
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.impl.client.HttpClients
import org.apache.http.ssl.SSLContexts
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.*
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.DefaultResponseErrorHandler
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity
import org.springframework.web.util.UriComponentsBuilder
import java.io.IOException
import java.net.URI
import java.security.KeyManagementException
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.cert.X509Certificate
import java.time.Instant


internal class KNServerContext(server: String, port: Int?, val user: String, val password: String) {
    private val url = if (port != null && port != 0) "$server:$port" else server

    val restTemplate = restTemplate()
    val loginContext: LoginContext by lazy {
        logger.info("Trying to login to $url by user:$user")
        return@lazy LoginContext(this)
    }

    fun builder(path: String): RequestBuilder = RequestBuilder(fullUrl(path), this)
    fun fullUrl(path: String): String = "$url$path"

    @Throws(KeyStoreException::class, NoSuchAlgorithmException::class, KeyManagementException::class)
    private fun restTemplate(): RestTemplate {
        val acceptingTrustStrategy = { _: Array<X509Certificate>, _: String -> true }

        val sslContext = SSLContexts.custom()
            .loadTrustMaterial(null, acceptingTrustStrategy)
            .build()

        val csf = SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE)

        val httpClient = HttpClients.custom()
            .setSSLSocketFactory(csf)
            .build()

        val requestFactory = HttpComponentsClientHttpRequestFactory()

        requestFactory.httpClient = httpClient
        requestFactory.setConnectionRequestTimeout(5000)
        return RestTemplate(requestFactory).apply { errorHandler = ErrorHandler(this@KNServerContext) }
    }

}

internal class LoginContext(private val context: KNServerContext) {
    private lateinit var accessHeader: String
    private lateinit var refreshHeader: String

    val authHeader: String
        get() =
            "$accessHeader; $refreshHeader"

    init {
        login()
    }

    fun login() {
        val path = context.fullUrl("/api/access/signIn")
        val userCredentials = UserCredentials(context.user, context.password)
        val token = context.restTemplate.postForEntity<String>(path, userCredentials, String::class).body ?: throw ServiceUnavailable
        parseToken(token)
    }

    private fun parseToken(response: String) {
        val jwtToken = JSON.parse<JwtToken>(response)
        val nowInMs = Instant.now()

        val accessExpireDateMs = jwtToken.accessTokenExpirationTimeInMs + nowInMs.toEpochMilli()
        val accessExpireDate = Instant.ofEpochMilli(accessExpireDateMs).toString()

        val refreshExpireDateMs = jwtToken.refreshTokenExpirationTimeInMs + nowInMs.toEpochMilli()
        val refreshExpireDate = Instant.ofEpochMilli(refreshExpireDateMs).toString()

        val accessValue = encodeURIComponent("Bearer ${jwtToken.accessToken}")
        val refreshValue = encodeURIComponent("Bearer ${jwtToken.refreshToken}")

        accessHeader = "$ACCESS_AUTH=$accessValue;expires=$accessExpireDate;path=/"
        refreshHeader = "$REFRESH_AUTH=$refreshValue;expires=$refreshExpireDate;path=/"
    }
}

private val logger = loggerFor<KNServerContext>()

inline fun <reified T : Any> loggerFor(): Logger =
    LoggerFactory.getLogger(T::class.java.name) ?: throw Throwable("Cannot access to logger library")


private const val REFRESH_AUTH = "x-refresh-authorization"
private const val ACCESS_AUTH = "x-access-authorization"

// https://stackoverflow.com/questions/607176/java-equivalent-to-javascripts-encodeuricomponent-that-produces-identical-outpu
fun encodeURIComponent(path: String): String = URI(null, null, path, null).rawPath

internal class RequestBuilder(path: String, private val context: KNServerContext) {
    private val uriBuilder = UriComponentsBuilder.fromUriString(path)
    private var body: String? = null

    fun parameter(name: String, param: Any?): RequestBuilder = build { uriBuilder.queryParam(name, param) }
    fun parameter(name: String, param: List<*>): RequestBuilder = build { uriBuilder.queryParam(name, *param.toTypedArray()) }
    inline fun <reified T : Any> body(value: T): RequestBuilder = build { body = JSON.stringify(value) }
    fun path(value: String): RequestBuilder = build { uriBuilder.path("/$value") }

    private fun build(block: () -> Unit): RequestBuilder {
        block()
        return this
    }

    inline fun <reified T> post(): T = execute(HttpMethod.POST) ?: throw ServiceUnavailable

    inline fun <reified T> get(): T = execute(HttpMethod.GET) ?: throw ServiceUnavailable

    fun postAndIgnore() = execute<Any?>(HttpMethod.POST)

    //todo: we must separate business logic errors from connectivity issues
    inline fun <reified T> getOrNull(): T? =
        try {
            execute(HttpMethod.GET)
        } catch (e: Exception) {
            logger.warn(e.message, e)
            null
        }

    private inline fun <reified T> execute(httpMethod: HttpMethod): T? = refreshTokenOnFail { doExecute<T>(httpMethod).body }

    private inline fun <reified T> doExecute(httpMethod: HttpMethod): ResponseEntity<T> {
        val headers = HttpHeaders().apply {
            set("Cookie", context.loginContext.authHeader)
        }

        val body = this.body
        val httpEntity = when (body) {
            null -> HttpEntity(null, headers)
            else -> {
                headers.contentType = MediaType.APPLICATION_JSON_UTF8;
                HttpEntity(body, headers)
            }
        }

        val uriString = uriBuilder.build().toUriString()
        logger.debug("$httpMethod $uriString")
        return context.restTemplate.exchange(uriString, httpMethod, httpEntity, T::class.java)
    }
}

internal class ErrorHandler(private val context: KNServerContext) : DefaultResponseErrorHandler() {
    @Throws(IOException::class)
    override fun handleError(response: ClientHttpResponse) {
        if (response.statusCode == HttpStatus.FORBIDDEN) {
            context.loginContext.login()
            throw RetryRequest
        }
    }
}


private inline fun <T> refreshTokenOnFail(block: () -> T?): T? {
    repeat(2) {
        try {
            return block()
        } catch (e: RetryRequest) {
            logger.error(e.message, e)
        }
    }
    throw ServiceUnavailable
}

object ServiceUnavailable : Exception("Knowledge Net server unavailable")
object RetryRequest : Exception("Knowledge Net server unavailable")
