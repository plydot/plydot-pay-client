package com.plydot.pay.internal

import com.plydot.pay.exception.PlydotPayException
import com.plydot.pay.model.ErrorResponse
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

internal enum class HttpMethod {
    GET,
    POST,
    PUT,
    PATCH,
}

internal data class HttpResponseBody(
    val statusCode: Int,
    val body: String,
)

internal class JdkHttpTransport(
    private val baseUrl: String,
    private val apiKey: String,
    connectTimeout: Duration,
    private val readTimeout: Duration,
) {
    private val httpClient: HttpClient =
        HttpClient.newBuilder()
            .connectTimeout(connectTimeout)
            .build()

    inline fun <reified T> exchange(
        method: HttpMethod,
        path: String,
        body: Any? = null,
        idempotencyKey: String? = null,
    ): T = exchange(method, path, body, idempotencyKey, T::class.java)

    inline fun <reified T> exchangeList(
        method: HttpMethod,
        path: String,
    ): List<T> = exchangeList(method, path, T::class.java)

    fun <T> exchange(
        method: HttpMethod,
        path: String,
        body: Any? = null,
        idempotencyKey: String? = null,
        responseType: Class<T>,
    ): T {
        val response = rawExchange(method, path, body, idempotencyKey)
        if (response.statusCode in 200..299) {
            return if (response.body.isBlank()) {
                throw IllegalStateException("Expected response body for $method $path")
            } else {
                JsonMapper.mapper.readValue(response.body, responseType)
            }
        }
        throw toException(response)
    }

    fun <T> exchangeList(
        method: HttpMethod,
        path: String,
        responseType: Class<T>,
    ): List<T> {
        val response = rawExchange(method, path)
        if (response.statusCode in 200..299) {
            return JsonMapper.mapper.readValue(
                response.body,
                JsonMapper.mapper.typeFactory.constructCollectionType(List::class.java, responseType),
            )
        }
        throw toException(response)
    }

    private fun rawExchange(
        method: HttpMethod,
        path: String,
        body: Any? = null,
        idempotencyKey: String? = null,
    ): HttpResponseBody {
        val uri = URI.create(normalizeBaseUrl(baseUrl) + path)
        val builder =
            HttpRequest.newBuilder()
                .uri(uri)
                .timeout(readTimeout)
                .header("Authorization", "Bearer $apiKey")
                .header("Accept", "application/json")

        if (idempotencyKey != null) {
            builder.header("Idempotency-Key", idempotencyKey)
        }

        when (method) {
            HttpMethod.GET -> builder.GET()
            HttpMethod.POST -> {
                builder.header("Content-Type", "application/json")
                val json = if (body == null) "{}" else JsonMapper.write(body)
                builder.POST(HttpRequest.BodyPublishers.ofString(json))
            }
            HttpMethod.PUT, HttpMethod.PATCH -> {
                builder.header("Content-Type", "application/json")
                val json = if (body == null) "{}" else JsonMapper.write(body)
                builder.method(method.name, HttpRequest.BodyPublishers.ofString(json))
            }
        }

        val httpResponse = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString())
        return HttpResponseBody(httpResponse.statusCode(), httpResponse.body())
    }

    private fun toException(response: HttpResponseBody): PlydotPayException {
        val error =
            runCatching {
                JsonMapper.read<ErrorResponse>(response.body)
            }.getOrElse {
                ErrorResponse(
                    code = "HTTP_${response.statusCode}",
                    message = response.body.ifBlank { "Request failed with HTTP ${response.statusCode}" },
                )
            }
        return PlydotPayException(error, response.statusCode)
    }

    private fun normalizeBaseUrl(url: String): String =
        if (url.endsWith("/")) url.dropLast(1) else url
}
