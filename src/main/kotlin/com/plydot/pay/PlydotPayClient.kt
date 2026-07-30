package com.plydot.pay

import com.plydot.pay.auth.PlydotPayAuth
import com.plydot.pay.internal.HttpMethod
import com.plydot.pay.internal.JdkHttpTransport
import com.plydot.pay.model.AccessTokenResponse
import com.plydot.pay.model.CheckoutStatus
import com.plydot.pay.model.CreateCheckoutRequest
import com.plydot.pay.model.CreateCheckoutResponse
import com.plydot.pay.model.PayCheckoutRequest
import com.plydot.pay.model.PayCheckoutResponse
import com.plydot.pay.model.CreatePayerRequest
import com.plydot.pay.model.PayerResponse
import com.plydot.pay.model.PaymentResponse
import com.plydot.pay.model.PaymentStatus
import com.plydot.pay.model.PayoutAccountResponse
import com.plydot.pay.model.ProviderOptionResponse
import com.plydot.pay.model.ProviderPayerAssignmentResponse
import com.plydot.pay.model.SettlementBalanceResponse
import com.plydot.pay.model.SettlementPayoutResponse
import com.plydot.pay.model.SettlementPayoutStatus
import com.plydot.pay.model.SubmitPayoutRequest
import com.plydot.pay.model.UpdatePayerRequest
import com.plydot.pay.model.AssignProviderPayersRequest
import com.plydot.pay.settlement.SettlementWorkflow
import com.plydot.pay.thirdparty.ThirdPartyCheckoutRequest
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeoutException

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

    /** Available balance for settlement (live Yo-verified payments only). Requires API key. */
    fun getSettlementBalance(merchantId: UUID? = null): SettlementBalanceResponse =
        transport.exchange(
            HttpMethod.GET,
            "/v1/settlements/balance${buildQuery {
                merchantId?.let { param("merchantId", it.toString()) }
            }}",
        )

    /** Configured bank/MoMo destination for payouts. Requires API key scoped to [merchantId]. */
    fun getPayoutAccount(merchantId: UUID): PayoutAccountResponse =
        transport.exchange(HttpMethod.GET, "/v1/merchants/$merchantId/payout-account")

    /**
     * Submit a payout request. Requires **merchant admin JWT** via [merchantAccessToken] or
     * [Builder.accessToken]. Omit [request.amountMinor] to pay out the full available balance.
     */
    fun submitPayoutRequest(
        request: SubmitPayoutRequest? = null,
        merchantAccessToken: String? = null,
    ): SettlementPayoutResponse =
        transport.exchange(
            HttpMethod.POST,
            "/v1/settlements/payout-requests",
            request ?: SubmitPayoutRequest(),
            bearerToken = merchantAccessToken,
        )

    fun getPayoutRequest(id: UUID): SettlementPayoutResponse =
        transport.exchange(HttpMethod.GET, "/v1/settlements/payout-requests/$id")

    fun listPayoutRequests(
        merchantId: UUID? = null,
        status: SettlementPayoutStatus? = null,
        page: Int = 0,
        size: Int = 50,
    ): List<SettlementPayoutResponse> =
        transport.exchangeList(
            HttpMethod.GET,
            "/v1/settlements/payout-requests${buildQuery {
                merchantId?.let { param("merchantId", it.toString()) }
                status?.let { param("status", it.name) }
                param("page", page.toString())
                param("size", size.toString())
            }}",
        )

    /**
     * Poll a payout until it reaches one of [targetStatuses] or [timeout] elapses.
     * Typical use: wait for `VERIFIED` or `VERIFICATION_FAILED` after [submitPayoutRequest].
     */
    @JvmOverloads
    fun waitForPayoutRequest(
        payoutId: UUID,
        timeout: Duration = Duration.ofMinutes(5),
        pollInterval: Duration = Duration.ofSeconds(3),
        targetStatuses: Set<SettlementPayoutStatus> = SettlementWorkflow.TERMINAL_VERIFY_STATUSES,
    ): SettlementPayoutResponse {
        val deadline = Instant.now().plus(timeout)
        while (Instant.now().isBefore(deadline)) {
            val payout = getPayoutRequest(payoutId)
            if (payout.status in targetStatuses) {
                return payout
            }
            Thread.sleep(pollInterval.toMillis())
        }
        throw TimeoutException("Payout $payoutId did not reach $targetStatuses within $timeout")
    }

    /** Convenience wrapper for balance → submit → poll. */
    fun settlementWorkflow(merchantAccessToken: String, merchantId: UUID): SettlementWorkflow =
        SettlementWorkflow(this, merchantAccessToken, merchantId)

    class Builder {
        private var apiKey: String? = null
        private var accessToken: String? = null
        private var baseUrl: String = DEFAULT_BASE_URL
        private var connectTimeout: Duration = Duration.ofSeconds(5)
        private var readTimeout: Duration = Duration.ofSeconds(30)

        fun apiKey(value: String) = apply { apiKey = value }

        /** Optional merchant admin JWT for settlement submit (overrides API key on those calls). */
        fun accessToken(value: String?) = apply { accessToken = value }

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
                    defaultAccessToken = accessToken,
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

        /** Exchange Keycloak username/password for a JWT via `POST /v1/auth/token`. */
        @JvmStatic
        @JvmOverloads
        fun obtainAccessToken(
            baseUrl: String = DEFAULT_BASE_URL,
            username: String,
            password: String,
            connectTimeout: Duration = Duration.ofSeconds(5),
            readTimeout: Duration = Duration.ofSeconds(30),
        ): AccessTokenResponse =
            PlydotPayAuth.obtainAccessToken(
                baseUrl = baseUrl,
                username = username,
                password = password,
                connectTimeout = connectTimeout,
                readTimeout = readTimeout,
            )
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
