package com.plydot.pay

import com.plydot.pay.internal.HttpMethod
import com.plydot.pay.internal.JdkHttpTransport
import com.plydot.pay.model.CheckoutStatus
import com.plydot.pay.model.CreateCheckoutRequest
import com.plydot.pay.model.CreateCheckoutResponse
import com.plydot.pay.model.PayCheckoutRequest
import com.plydot.pay.model.PayCheckoutResponse
import com.plydot.pay.model.CreatePayerRequest
import com.plydot.pay.model.PayerResponse
import com.plydot.pay.model.PaymentResponse
import com.plydot.pay.model.PaymentStatus
import com.plydot.pay.model.ProviderOptionResponse
import com.plydot.pay.model.ProviderPayerAssignmentResponse
import com.plydot.pay.model.UpdatePayerRequest
import com.plydot.pay.model.AssignProviderPayersRequest
import com.plydot.pay.thirdparty.ThirdPartyCheckoutRequest
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.util.UUID

class PlydotPayClient private constructor(
    private val transport: JdkHttpTransport,
) {
    fun createCheckout(
        request: CreateCheckoutRequest,
        idempotencyKey: String? = null,
    ): CreateCheckoutResponse =
        transport.exchange(
            HttpMethod.POST,
            "/v1/checkouts",
            request,
            idempotencyKey,
        )

    fun createThirdPartyCheckout(
        request: ThirdPartyCheckoutRequest,
        idempotencyKey: String? = null,
    ): CreateCheckoutResponse =
        createCheckout(request.toCreateCheckoutRequest(), idempotencyKey)

    fun getCheckout(id: UUID): CreateCheckoutResponse =
        transport.exchange(HttpMethod.GET, "/v1/checkouts/$id")

    fun listCheckouts(
        status: CheckoutStatus? = null,
        accountRef: String? = null,
        from: Instant? = null,
        to: Instant? = null,
        page: Int = 0,
        size: Int = 50,
    ): List<CreateCheckoutResponse> =
        transport.exchangeList(
            HttpMethod.GET,
            "/v1/checkouts${buildQuery {
                status?.let { param("status", it.name) }
                accountRef?.let { param("accountRef", it) }
                from?.let { param("from", it.toString()) }
                to?.let { param("to", it.toString()) }
                param("page", page.toString())
                param("size", size.toString())
            }}",
        )

    fun payCheckout(
        checkoutId: UUID,
        request: PayCheckoutRequest,
    ): PayCheckoutResponse =
        transport.exchange(
            HttpMethod.POST,
            "/v1/checkouts/$checkoutId/pay",
            request,
        )

    fun cancelCheckout(checkoutId: UUID): CreateCheckoutResponse =
        transport.exchange(HttpMethod.POST, "/v1/checkouts/$checkoutId/cancel")

    fun getPayment(id: UUID): PaymentResponse =
        transport.exchange(HttpMethod.GET, "/v1/payments/$id")

    fun listPayments(
        status: PaymentStatus? = null,
        checkoutId: UUID? = null,
        from: Instant? = null,
        to: Instant? = null,
        page: Int = 0,
        size: Int = 50,
    ): List<PaymentResponse> =
        transport.exchangeList(
            HttpMethod.GET,
            "/v1/payments${buildQuery {
                status?.let { param("status", it.name) }
                checkoutId?.let { param("checkoutId", it.toString()) }
                from?.let { param("from", it.toString()) }
                to?.let { param("to", it.toString()) }
                param("page", page.toString())
                param("size", size.toString())
            }}",
        )

    fun listPayers(active: Boolean? = null): List<PayerResponse> =
        transport.exchangeList(
            HttpMethod.GET,
            "/v1/payers${buildQuery { active?.let { param("active", it.toString()) } }}",
        )

    fun getPayer(id: UUID): PayerResponse =
        transport.exchange(HttpMethod.GET, "/v1/payers/$id")

    fun createPayer(request: CreatePayerRequest): PayerResponse =
        transport.exchange(HttpMethod.POST, "/v1/payers", request)

    fun updatePayer(id: UUID, request: UpdatePayerRequest): PayerResponse =
        transport.exchange(HttpMethod.PATCH, "/v1/payers/$id", request)

    fun listProviderPayers(providerId: UUID): ProviderPayerAssignmentResponse =
        transport.exchange(HttpMethod.GET, "/v1/providers/$providerId/payers")

    fun assignProviderPayers(
        providerId: UUID,
        request: AssignProviderPayersRequest,
    ): ProviderPayerAssignmentResponse =
        transport.exchange(HttpMethod.PUT, "/v1/providers/$providerId/payers", request)

    /** Assigned providers with nested payers (merchant) or full catalog (platform admin). */
    fun listProviders(): List<ProviderOptionResponse> =
        transport.exchangeList(HttpMethod.GET, "/v1/providers")

    fun failCheckoutForSwitch(checkoutId: UUID, reason: String? = "PROVIDER_SWITCH"): CreateCheckoutResponse =
        transport.exchange(
            HttpMethod.POST,
            "/v1/checkouts/$checkoutId/fail-for-switch",
            mapOf("reason" to reason),
        )

    class Builder {
        private var apiKey: String? = null
        private var baseUrl: String = DEFAULT_BASE_URL
        private var connectTimeout: Duration = Duration.ofSeconds(5)
        private var readTimeout: Duration = Duration.ofSeconds(30)

        fun apiKey(value: String) = apply { apiKey = value }

        fun baseUrl(value: String) = apply { baseUrl = value }

        fun connectTimeout(value: Duration) = apply { connectTimeout = value }

        fun readTimeout(value: Duration) = apply { readTimeout = value }

        fun build(): PlydotPayClient {
            val resolvedApiKey = apiKey?.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("apiKey is required")
            return PlydotPayClient(
                JdkHttpTransport(
                    baseUrl = baseUrl,
                    apiKey = resolvedApiKey,
                    connectTimeout = connectTimeout,
                    readTimeout = readTimeout,
                ),
            )
        }
    }

    companion object {
        const val DEFAULT_BASE_URL: String = "https://pay.plydot.dev"

        @JvmStatic
        fun builder(): Builder = Builder()
    }
}

private class QueryBuilder {
    private val params = mutableListOf<Pair<String, String>>()

    fun param(name: String, value: String) {
        params += name to value
    }

    fun build(): String {
        if (params.isEmpty()) {
            return ""
        }
        return params.joinToString(
            prefix = "?",
            separator = "&",
        ) { (name, value) ->
            "${encode(name)}=${encode(value)}"
        }
    }
}

private inline fun buildQuery(block: QueryBuilder.() -> Unit): String =
    QueryBuilder().apply(block).build()

private fun encode(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8)
