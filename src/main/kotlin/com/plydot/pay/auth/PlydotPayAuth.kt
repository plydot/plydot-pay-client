package com.plydot.pay.auth

import com.plydot.pay.internal.JsonMapper
import com.plydot.pay.model.AccessTokenResponse
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.Base64

/**
 * Obtain a Keycloak JWT for merchant admin settlement operations.
 * Uses `POST /v1/auth/token` with HTTP Basic credentials.
 */
object PlydotPayAuth {
    @JvmStatic
    fun obtainAccessToken(
        baseUrl: String,
        username: String,
        password: String,
        connectTimeout: Duration = Duration.ofSeconds(5),
        readTimeout: Duration = Duration.ofSeconds(30),
    ): AccessTokenResponse {
        val normalizedBase = if (baseUrl.endsWith("/")) baseUrl.dropLast(1) else baseUrl
        val basic =
            Base64.getEncoder().encodeToString(
                "$username:$password".toByteArray(StandardCharsets.UTF_8),
            )
        val client =
            HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build()
        val request =
            HttpRequest.newBuilder()
                .uri(URI.create("$normalizedBase/v1/auth/token"))
                .timeout(readTimeout)
                .header("Authorization", "Basic $basic")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw IllegalStateException(
                "Failed to obtain access token (HTTP ${response.statusCode()}): ${response.body()}",
            )
        }
        return JsonMapper.read(response.body())
    }
}
